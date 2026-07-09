package pl.dlaflow.mobile.feature.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage

class PairingStateHolderTest {
    @Test
    fun `valid code opens mandatory device name and back preserves drafts`() {
        val holder = PairingStateHolder()
        holder.updateCode("abc123")

        assertTrue(holder.continueToName())
        holder.updateDeviceName("Magazyn")
        assertEquals(PairingStep.NAME, holder.state.step)

        assertTrue(holder.back())
        assertEquals(PairingStep.CODE, holder.state.step)
        assertEquals("ABC-123", holder.state.codeInput)
        assertEquals("Magazyn", holder.state.deviceNameInput)
    }

    @Test
    fun `submission requires valid name and prevents duplicate request`() {
        val holder = PairingStateHolder()
        holder.updateCode("ABC-123")
        holder.continueToName()

        assertNull(holder.beginSubmission())
        assertEquals(PairingFeedback.DEVICE_NAME_REQUIRED, holder.state.localFeedback)

        holder.updateDeviceName("Telefon Maćka")
        val first = holder.beginSubmission()
        assertNotNull(first)
        assertEquals("Telefon Maćka", first?.deviceName)
        assertNull(holder.beginSubmission())
        assertTrue(holder.state.isSubmitting)
    }

    @Test
    fun `expired code clears code but preserves name`() {
        val holder = PairingStateHolder()
        holder.updateCode("ABC-123")
        holder.continueToName()
        holder.updateDeviceName("Pakowanie")
        val request = holder.beginSubmission()!!

        holder.rejectCode(request.requestId, PairingFeedback.CODE_EXPIRED)

        assertEquals(PairingStep.CODE, holder.state.step)
        assertEquals("", holder.state.codeInput)
        assertEquals("Pakowanie", holder.state.deviceNameInput)
        assertFalse(holder.state.isSubmitting)
    }

    @Test
    fun `stale callback cannot replace a newer pairing request`() {
        val holder = PairingStateHolder()
        val retryableMessage = DlaFlowUiMessage(titleRes = 1, descriptionRes = 2, retryable = true)
        holder.updateCode("ABC-123")
        holder.continueToName()
        holder.updateDeviceName("Magazyn")
        val first = holder.beginSubmission()!!
        assertTrue(holder.failRetryable(first.requestId, retryableMessage))
        val second = holder.beginSubmission()!!

        assertFalse(holder.failRetryable(first.requestId, retryableMessage))
        assertEquals(second.requestId, holder.state.activeRequestId)
        assertTrue(holder.state.isSubmitting)
    }
}
