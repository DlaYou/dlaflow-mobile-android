package pl.dlaflow.mobile.feature.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class ScannerStateHolderTest {
    private val message = DlaFlowUiMessage(1, 2, retryable = true)

    @Test
    fun `blank or cancelled capture preserves the complete previous state`() {
        val holder = ScannerStateHolder()
        val request = holder.beginLookup("session-a", "SYNTHETIC")!!
        holder.acceptSuccess(request, scannerResult(ScannerMatchKind.MATCH))
        val before = holder.state

        assertNull(holder.beginLookup("session-a", "   "))
        holder.captureCancelled()

        assertEquals(before, holder.state)
    }

    @Test
    fun `newest request and matching session are required`() {
        val holder = ScannerStateHolder()
        val stale = holder.beginLookup("session-a", "OLD")!!
        val current = holder.beginLookup("session-a", "NEW")!!
        val before = holder.state

        assertFalse(holder.acceptSuccess(stale, scannerResult(ScannerMatchKind.MATCH)))
        assertFalse(holder.acceptFailure(current.copy(sessionKey = "session-b"), message))
        assertEquals(before, holder.state)
        assertTrue(holder.acceptSuccess(current, scannerResult(ScannerMatchKind.NO_MATCH)))
        assertEquals(ScannerMatchKind.NO_MATCH, holder.state.contentOrNull()?.kind)
    }

    @Test
    fun `pending launch waits and is consumed exactly once`() {
        val holder = ScannerStateHolder()

        assertTrue(holder.waitForSession("  SYNTHETIC-PENDING  "))
        assertEquals(DlaFlowUiState.Loading, holder.state.lookupState)
        assertTrue(holder.state.waitingForSession)

        assertEquals("SYNTHETIC-PENDING", holder.consumePendingLaunch())
        assertNull(holder.consumePendingLaunch())
        assertFalse(holder.state.waitingForSession)
    }

    @Test
    fun `blank pending launch does not replace content`() {
        val holder = ScannerStateHolder()
        val request = holder.beginLookup("session-a", "SYNTHETIC")!!
        holder.acceptSuccess(request, scannerResult(ScannerMatchKind.MATCH))
        val before = holder.state

        assertFalse(holder.waitForSession("  "))

        assertEquals(before, holder.state)
    }

    @Test
    fun `pending verification failure is terminal and cannot be applied twice`() {
        val holder = ScannerStateHolder()
        holder.waitForSession("SYNTHETIC-PENDING")

        assertTrue(holder.failPendingLaunch(message))
        assertFalse(holder.failPendingLaunch(message))
        assertEquals(DlaFlowUiState.Error(message), holder.state.lookupState)
        assertFalse(holder.state.waitingForSession)
    }

    @Test
    fun `offline no access and generic errors are typed terminal states`() {
        val offlineHolder = ScannerStateHolder()
        val offlineRequest = offlineHolder.beginLookup("session-a", "OFFLINE")!!
        assertTrue(offlineHolder.acceptOffline(offlineRequest, message))
        assertEquals(DlaFlowUiState.Offline<ScannerLookupResult>(), offlineHolder.state.lookupState)

        val noAccessHolder = ScannerStateHolder()
        val noAccessRequest = noAccessHolder.beginLookup("session-a", "FORBIDDEN")!!
        assertTrue(noAccessHolder.acceptNoAccess(noAccessRequest))
        assertEquals(DlaFlowUiState.NoAccess, noAccessHolder.state.lookupState)

        val errorHolder = ScannerStateHolder()
        val errorRequest = errorHolder.beginLookup("session-a", "ERROR")!!
        assertTrue(errorHolder.acceptFailure(errorRequest, message))
        assertEquals(DlaFlowUiState.Error(message), errorHolder.state.lookupState)
    }

    @Test
    fun `unauthorized can be completed as unconfirmed only for the accepted request`() {
        val holder = ScannerStateHolder()
        val unauthorized = holder.beginLookup("session-a", "AUTH")!!

        assertTrue(holder.acceptUnauthorized(unauthorized))
        assertEquals(DlaFlowUiState.Loading, holder.state.lookupState)
        val unconfirmed = DlaFlowUiMessage(3, 4, retryable = true)
        assertTrue(holder.acceptSessionUnconfirmed(unauthorized, unconfirmed))
        assertFalse(holder.acceptSessionUnconfirmed(unauthorized, unconfirmed))
        assertEquals(DlaFlowUiState.Error(unconfirmed), holder.state.lookupState)
    }

    @Test
    fun `new lookup suppresses late unauthorized confirmation callback`() {
        val holder = ScannerStateHolder()
        val old = holder.beginLookup("session-a", "OLD")!!
        holder.acceptUnauthorized(old)
        val current = holder.beginLookup("session-a", "NEW")!!
        holder.acceptSuccess(current, scannerResult(ScannerMatchKind.MATCH))

        assertFalse(holder.acceptSessionUnconfirmed(old, message))
        assertEquals(ScannerMatchKind.MATCH, holder.state.contentOrNull()?.kind)
    }

    @Test
    fun `accepted unauthorized recovery can start exactly one request-bound retry`() {
        val holder = ScannerStateHolder()
        val unauthorized = holder.beginLookup("session-a", "AUTH")!!
        holder.acceptUnauthorized(unauthorized)

        val retry = holder.beginUnauthorizedRetry(unauthorized)

        assertTrue(retry != null)
        assertTrue(retry!!.requestId > unauthorized.requestId)
        assertNull(holder.beginUnauthorizedRetry(unauthorized))
    }

    @Test
    fun `reset invalidates active and pending work without reusing request ids`() {
        val holder = ScannerStateHolder()
        val old = holder.beginLookup("session-a", "OLD")!!
        holder.waitForSession("PENDING")

        holder.reset()

        assertEquals(ScannerUiState(), holder.state)
        assertFalse(holder.acceptSuccess(old, scannerResult(ScannerMatchKind.MATCH)))
        val current = holder.beginLookup("session-b", "NEW")!!
        assertTrue(current.requestId > old.requestId)
    }

    @Test
    fun `reset also invalidates pending unauthorized recovery`() {
        val holder = ScannerStateHolder()
        val unauthorized = holder.beginLookup("session-a", "AUTH")!!
        holder.acceptUnauthorized(unauthorized)

        holder.reset()

        assertNull(holder.beginUnauthorizedRetry(unauthorized))
        assertFalse(holder.acceptSessionUnconfirmed(unauthorized, message))
    }
}

internal fun scannerResult(kind: ScannerMatchKind) = ScannerLookupResult(
    kind = kind,
    order = if (kind == ScannerMatchKind.NO_MATCH) null else ScannerOrder("ORDER-1", "Zamówienie testowe", "Nowe"),
    shipment = if (kind == ScannerMatchKind.NO_MATCH) null else ScannerShipment("InPost", "Gotowa"),
)
