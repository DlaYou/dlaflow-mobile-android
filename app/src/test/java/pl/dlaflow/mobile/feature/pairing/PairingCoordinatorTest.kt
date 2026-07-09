package pl.dlaflow.mobile.feature.pairing

import java.net.SocketTimeoutException
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import pl.dlaflow.mobile.MobileSession
import pl.dlaflow.mobile.core.network.MobileApiException

class PairingCoordinatorTest {
    @Test
    fun `coordinator sends custom name once and hands off successful session`() {
        val holder = readyHolder("ABC-123", "Magazyn")
        val calls = mutableListOf<PairingSubmission>()
        var acceptedSession: MobileSession? = null
        val coordinator = PairingCoordinator(
            stateHolder = holder,
            gateway = PairingGateway { _, submission ->
                calls += submission
                MobileSession("device-1", submission.deviceName, "Firma", "token", "user@example.test")
            },
            executor = Executor { it.run() },
            postToMain = { it() },
            onStarted = {},
            onSuccess = { _, session -> acceptedSession = session },
            onFailure = {},
        )

        coordinator.submit("https://panel.dlayou.pl")

        assertEquals(1, calls.size)
        assertEquals("Magazyn", calls.single().deviceName)
        assertEquals("Magazyn", acceptedSession?.deviceName)
    }

    @Test
    fun `pairing code errors return to code entry and keep name`() {
        val cases = listOf(
            "MOBILE_PAIRING_CODE_NOT_FOUND" to PairingFeedback.CODE_NOT_FOUND,
            "MOBILE_PAIRING_CODE_USED" to PairingFeedback.CODE_USED,
            "MOBILE_PAIRING_CODE_EXPIRED" to PairingFeedback.CODE_EXPIRED,
        )

        cases.forEach { (code, expectedFeedback) ->
            val holder = readyHolder("ABC-123", "Pakowanie")
            coordinatorThrowing(holder, MobileApiException(410, code, "raw")).submit("https://panel.dlayou.pl")

            assertEquals(PairingStep.CODE, holder.state.step)
            assertEquals("", holder.state.codeInput)
            assertEquals("Pakowanie", holder.state.deviceNameInput)
            assertEquals(expectedFeedback, holder.state.localFeedback)
        }
    }

    @Test
    fun `timeout keeps both drafts on name step`() {
        val holder = readyHolder("ABC-123", "Magazyn")
        coordinatorThrowing(holder, SocketTimeoutException("raw")).submit("https://panel.dlayou.pl")

        assertEquals(PairingStep.NAME, holder.state.step)
        assertEquals("ABC-123", holder.state.codeInput)
        assertEquals("Magazyn", holder.state.deviceNameInput)
        assertNotNull(holder.state.sharedMessage)
    }

    private fun readyHolder(code: String, name: String) = PairingStateHolder().apply {
        updateCode(code)
        continueToName()
        updateDeviceName(name)
    }

    private fun coordinatorThrowing(holder: PairingStateHolder, error: Throwable) = PairingCoordinator(
        stateHolder = holder,
        gateway = PairingGateway { _, _ -> throw error },
        executor = Executor { it.run() },
        postToMain = { it() },
        onStarted = {},
        onSuccess = { _, _ -> error("Unexpected success") },
        onFailure = {},
    )
}
