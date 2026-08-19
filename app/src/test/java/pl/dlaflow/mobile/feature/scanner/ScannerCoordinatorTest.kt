package pl.dlaflow.mobile.feature.scanner

import java.net.UnknownHostException
import java.util.ArrayDeque
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class ScannerCoordinatorTest {
    @Test
    fun `lookup reaches state only through main queue and reports match kind`() {
        val harness = ScannerHarness()

        assertTrue(harness.coordinator.acceptCapture(" SYNTHETIC ", "session-a"))
        assertEquals(DlaFlowUiState.Loading, harness.holder.state.lookupState)
        harness.executor.runNext()
        assertEquals(DlaFlowUiState.Loading, harness.holder.state.lookupState)
        harness.mainQueue.runNext()

        assertEquals(ScannerMatchKind.MATCH, harness.holder.state.contentOrNull()?.kind)
        assertEquals(listOf(ScannerFeedback.LOOKUP_LOADING, ScannerFeedback.MATCHED), harness.feedback)
    }

    @Test
    fun `stale success emits neither state change nor feedback`() {
        val harness = ScannerHarness(
            resultForCode = { code -> scannerResult(if (code == "NEW") ScannerMatchKind.MATCH else ScannerMatchKind.NO_MATCH) },
        )
        harness.coordinator.acceptCapture("OLD", "session-a")
        harness.coordinator.acceptCapture("NEW", "session-a")

        harness.executor.runLast()
        harness.mainQueue.runNext()
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(ScannerMatchKind.MATCH, harness.holder.state.contentOrNull()?.kind)
        assertEquals(
            listOf(ScannerFeedback.LOOKUP_LOADING, ScannerFeedback.LOOKUP_LOADING, ScannerFeedback.MATCHED),
            harness.feedback,
        )
    }

    @Test
    fun `blank capture preserves state and reports empty capture without scheduling`() {
        val harness = ScannerHarness()
        harness.coordinator.acceptCapture("SYNTHETIC", "session-a")
        harness.executor.runNext()
        harness.mainQueue.runNext()
        val before = harness.holder.state

        assertFalse(harness.coordinator.acceptCapture("  ", "session-a"))

        assertEquals(before, harness.holder.state)
        assertEquals(0, harness.executor.size)
        assertEquals(ScannerFeedback.CAPTURE_EMPTY, harness.feedback.last())
    }

    @Test
    fun `launch waits for saved session and resumes exactly once`() {
        val harness = ScannerHarness()

        harness.coordinator.acceptLaunch("PENDING", activeToken = null, hasSavedSession = true)
        assertTrue(harness.holder.state.waitingForSession)
        assertEquals(0, harness.executor.size)

        assertTrue(harness.coordinator.resumePendingLaunch("session-a"))
        assertFalse(harness.coordinator.resumePendingLaunch("session-a"))
        assertEquals(1, harness.executor.size)
        harness.executor.runNext()
        harness.mainQueue.runNext()
        assertEquals(ScannerMatchKind.MATCH, harness.holder.state.contentOrNull()?.kind)
    }

    @Test
    fun `capture waits for saved session and resumes lookup exactly once`() {
        val requestedCodes = mutableListOf<String>()
        val harness = ScannerHarness(resultForCode = { code ->
            requestedCodes += code
            scannerResult(ScannerMatchKind.MATCH)
        })

        assertTrue(
            harness.coordinator.acceptCapture(
                rawCode = " PENDING-CAPTURE ",
                token = null,
                hasSavedSession = true,
            ),
        )
        assertTrue(harness.holder.state.waitingForSession)
        assertEquals(0, harness.executor.size)
        assertEquals(ScannerFeedback.WAITING_FOR_SESSION, harness.feedback.last())

        assertTrue(harness.coordinator.resumePendingLaunch("session-a"))
        assertFalse(harness.coordinator.resumePendingLaunch("session-a"))
        assertEquals(1, harness.executor.size)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(listOf("PENDING-CAPTURE"), requestedCodes)
        assertEquals(ScannerMatchKind.MATCH, harness.holder.state.contentOrNull()?.kind)
    }

    @Test
    fun `capture without active or saved session fails closed`() {
        val harness = ScannerHarness()

        assertFalse(
            harness.coordinator.acceptCapture(
                rawCode = "SYNTHETIC",
                token = null,
                hasSavedSession = false,
            ),
        )

        assertTrue(harness.holder.state.lookupState is DlaFlowUiState.Error)
        assertFalse(harness.holder.state.waitingForSession)
        assertEquals(0, harness.executor.size)
        assertEquals(ScannerFeedback.LOAD_FAILED, harness.feedback.last())
    }

    @Test
    fun `launch without active or saved session becomes controlled failure`() {
        val harness = ScannerHarness()

        harness.coordinator.acceptLaunch("SYNTHETIC", activeToken = null, hasSavedSession = false)

        assertTrue(harness.holder.state.lookupState is DlaFlowUiState.Error)
        assertEquals(0, harness.executor.size)
        assertEquals(ScannerFeedback.LOAD_FAILED, harness.feedback.last())
    }

    @Test
    fun `pending verification can fail once with controlled state`() {
        val harness = ScannerHarness()
        harness.coordinator.acceptLaunch("PENDING", activeToken = null, hasSavedSession = true)

        assertTrue(harness.coordinator.failPendingLaunch())
        assertFalse(harness.coordinator.failPendingLaunch())
        assertTrue(harness.holder.state.lookupState is DlaFlowUiState.Error)
    }

    @Test
    fun `accepted 401 requests host confirmation and valid session retries network lookup exactly once`() {
        val error = MobileApiException(401, "AUTH_REQUIRED", "private")
        var attempts = 0
        val harness = ScannerHarness(resultForCode = {
            attempts += 1
            if (attempts == 1) throw error
            scannerResult(ScannerMatchKind.MATCH)
        })

        harness.coordinator.acceptCapture("SYNTHETIC", "session-a")
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(1, harness.unauthorized.size)
        assertSame(error, harness.unauthorized.single().error)
        assertTrue(harness.unauthorized.single().allowRetry)
        assertEquals(0, harness.executor.size)
        assertEquals(DlaFlowUiState.Loading, harness.holder.state.lookupState)

        harness.unauthorized.single().onSessionValid()
        harness.unauthorized.single().onSessionValid()
        assertEquals(1, harness.executor.size)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(2, attempts)
        assertEquals(ScannerMatchKind.MATCH, harness.holder.state.contentOrNull()?.kind)
    }

    @Test
    fun `unconfirmed 401 ends as retryable error and late callback cannot overwrite newer result`() {
        val error = MobileApiException(401, "AUTH_REQUIRED", "private")
        var fail = true
        val harness = ScannerHarness(resultForCode = {
            if (fail) throw error
            scannerResult(ScannerMatchKind.MATCH)
        })
        harness.coordinator.acceptCapture("OLD", "session-a")
        harness.executor.runNext()
        harness.mainQueue.runNext()
        val late = harness.unauthorized.single().onSessionUnconfirmed

        fail = false
        harness.coordinator.acceptCapture("NEW", "session-a")
        harness.executor.runNext()
        harness.mainQueue.runNext()
        late()

        assertEquals(ScannerMatchKind.MATCH, harness.holder.state.contentOrNull()?.kind)
    }

    @Test
    fun `stale valid-session confirmation cannot retry after a newer lookup`() {
        val error = MobileApiException(401, "AUTH_REQUIRED", "private")
        var fail = true
        val harness = ScannerHarness(resultForCode = {
            if (fail) throw error
            scannerResult(ScannerMatchKind.MATCH)
        })
        harness.coordinator.acceptCapture("OLD", "session-a")
        harness.executor.runNext()
        harness.mainQueue.runNext()
        val staleRecovery = harness.unauthorized.single().onSessionValid

        fail = false
        harness.coordinator.acceptCapture("NEW", "session-a")
        harness.executor.runNext()
        harness.mainQueue.runNext()
        staleRecovery()

        assertEquals(0, harness.executor.size)
        assertEquals(ScannerMatchKind.MATCH, harness.holder.state.contentOrNull()?.kind)
    }

    @Test
    fun `second 401 after confirmed valid session is terminal without another confirmation`() {
        val error = MobileApiException(401, "AUTH_REQUIRED", "private")
        val harness = ScannerHarness(resultForCode = { throw error })
        harness.coordinator.acceptCapture("SYNTHETIC", "session-a")
        harness.executor.runNext()
        harness.mainQueue.runNext()

        harness.unauthorized.single().onSessionValid()
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(1, harness.unauthorized.size)
        assertTrue(harness.holder.state.lookupState is DlaFlowUiState.Error)
        assertEquals(0, harness.executor.size)
    }

    @Test
    fun `403 and offline map to typed states without raw exception text`() {
        val forbidden = ScannerHarness(resultForCode = { throw MobileApiException(403, "FORBIDDEN", "private") })
        forbidden.coordinator.acceptCapture("SYNTHETIC", "session-a")
        forbidden.executor.runNext()
        forbidden.mainQueue.runNext()
        assertEquals(DlaFlowUiState.NoAccess, forbidden.holder.state.lookupState)

        val offline = ScannerHarness(resultForCode = { throw UnknownHostException("private host") })
        offline.coordinator.acceptCapture("SYNTHETIC", "session-a")
        offline.executor.runNext()
        offline.mainQueue.runNext()
        assertTrue(offline.holder.state.lookupState is DlaFlowUiState.Offline)
    }

    @Test
    fun `invalid matched payload becomes controlled error and cannot open an order`() {
        val harness = ScannerHarness(resultForCode = { throw InvalidScannerResultException() })

        harness.coordinator.acceptCapture("SYNTHETIC", "session-a")
        harness.executor.runNext()
        harness.mainQueue.runNext()

        val terminal = harness.holder.state.lookupState as DlaFlowUiState.Error
        assertFalse(terminal.message.retryable)
        assertTrue(harness.openedOrders.isEmpty())
        assertEquals(ScannerFeedback.LOAD_FAILED, harness.feedback.last())
    }

    @Test
    fun `typed actions delegate capture and order opening without feature dependency`() {
        val harness = ScannerHarness()

        harness.coordinator.handleAction(ScannerAction.RequestCapture)
        harness.coordinator.handleAction(ScannerAction.OpenOrder("ORDER-1"))
        harness.coordinator.handleAction(ScannerAction.OpenOrder(" "))

        assertEquals(1, harness.captureRequests)
        assertEquals(listOf("ORDER-1"), harness.openedOrders)
    }
}

private class ScannerHarness(
    resultForCode: (String) -> ScannerLookupResult = { scannerResult(ScannerMatchKind.MATCH) },
) {
    val holder = ScannerStateHolder()
    val executor = ScannerQueuedExecutor()
    val mainQueue = ScannerMainQueue()
    val feedback = mutableListOf<ScannerFeedback>()
    val unauthorized = mutableListOf<ScannerUnauthorizedEvent>()
    val openedOrders = mutableListOf<String>()
    var captureRequests = 0
    val coordinator = ScannerCoordinator(
        stateHolder = holder,
        gateway = object : ScannerGateway {
            override fun lookup(token: String, code: String): ScannerLookupResult = resultForCode(code)
        },
        executor = executor,
        postToMain = mainQueue::post,
        onFeedback = feedback::add,
        onRequestCapture = { captureRequests += 1 },
        onOpenOrder = openedOrders::add,
        onUnauthorized = { error, allowRetry, onSessionValid, onSessionUnconfirmed ->
            unauthorized += ScannerUnauthorizedEvent(error, allowRetry, onSessionValid, onSessionUnconfirmed)
        },
    )
}

private data class ScannerUnauthorizedEvent(
    val error: Throwable,
    val allowRetry: Boolean,
    val onSessionValid: () -> Unit,
    val onSessionUnconfirmed: () -> Unit,
)

private class ScannerQueuedExecutor : Executor {
    private val tasks = ArrayDeque<Runnable>()
    val size: Int get() = tasks.size
    override fun execute(command: Runnable) {
        tasks.addLast(command)
    }
    fun runNext() = tasks.removeFirst().run()
    fun runLast() = tasks.removeLast().run()
}

private class ScannerMainQueue {
    private val tasks = ArrayDeque<() -> Unit>()
    fun post(task: () -> Unit) {
        tasks.addLast(task)
    }
    fun runNext() = tasks.removeFirst().invoke()
}
