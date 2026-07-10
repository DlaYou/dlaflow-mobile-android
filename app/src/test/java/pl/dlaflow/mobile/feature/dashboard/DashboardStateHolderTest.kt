package pl.dlaflow.mobile.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class DashboardStateHolderTest {
    private val message = DlaFlowUiMessage(
        titleRes = 1,
        descriptionRes = 2,
        retryable = true,
    )

    @Test
    fun `zero business snapshot remains content`() {
        val holder = DashboardStateHolder()
        val request = holder.beginLoad("session-a")
        val zero = dashboardDto(
            todayRevenue = 0.0,
            notifications = emptyList(),
            activePhotoTask = null,
        ).toDashboardContent()

        assertTrue(holder.acceptSuccess(request, zero))
        assertEquals(DlaFlowUiState.Content(zero), holder.state.contentState)
    }

    @Test
    fun `new request keeps content visible and rejects stale result`() {
        val holder = DashboardStateHolder()
        val first = holder.beginLoad("session-a")
        val old = dashboardDto(todayRevenue = 100.0).toDashboardContent()
        assertTrue(holder.acceptSuccess(first, old))

        val stale = holder.beginLoad("session-a")
        val current = holder.beginLoad("session-a")
        assertEquals(old, holder.state.contentOrNull())
        assertTrue(holder.state.isRefreshing)
        assertFalse(holder.acceptSuccess(stale, dashboardDto(todayRevenue = 200.0).toDashboardContent()))
        assertTrue(holder.acceptSuccess(current, dashboardDto(todayRevenue = 300.0).toDashboardContent()))
        assertEquals(300.0, holder.state.contentOrNull()!!.todayRevenue, 0.0)
    }

    @Test
    fun `reset invalidates active request without reusing its id`() {
        val holder = DashboardStateHolder()
        val stale = holder.beginLoad("session-a")
        holder.reset()
        val current = holder.beginLoad("session-b")

        assertFalse(holder.acceptSuccess(stale, dashboardDto().toDashboardContent()))
        assertTrue(current.requestId > stale.requestId)
    }

    @Test
    fun `first load is loading and accepted success clears request metadata`() {
        val holder = DashboardStateHolder()

        val request = holder.beginLoad("session-a")

        assertEquals(DlaFlowUiState.Loading, holder.state.contentState)
        assertFalse(holder.state.isRefreshing)
        assertEquals(request.requestId, holder.state.activeRequestId)

        assertTrue(holder.acceptSuccess(request, dashboardDto().toDashboardContent()))
        assertFalse(holder.state.isRefreshing)
        assertNull(holder.state.activeRequestId)
        assertNull(holder.state.transientMessage)
    }

    @Test
    fun `offline keeps last content and clears active request`() {
        val holder = DashboardStateHolder()
        val initial = holder.beginLoad("session-a")
        val content = dashboardDto().toDashboardContent()
        holder.acceptSuccess(initial, content)
        val refresh = holder.beginLoad("session-a")

        assertTrue(holder.acceptOffline(refresh, message))

        assertEquals(DlaFlowUiState.Offline(content), holder.state.contentState)
        assertEquals(content, holder.state.contentOrNull())
        assertEquals(message, holder.state.transientMessage)
        assertFalse(holder.state.isRefreshing)
        assertNull(holder.state.activeRequestId)
    }

    @Test
    fun `generic failure retains content and exposes transient message`() {
        val holder = DashboardStateHolder()
        val initial = holder.beginLoad("session-a")
        val content = dashboardDto().toDashboardContent()
        holder.acceptSuccess(initial, content)
        val refresh = holder.beginLoad("session-a")

        assertTrue(holder.acceptFailure(refresh, message))

        assertEquals(DlaFlowUiState.Content(content), holder.state.contentState)
        assertEquals(message, holder.state.transientMessage)
        assertFalse(holder.state.isRefreshing)
        assertNull(holder.state.activeRequestId)
    }

    @Test
    fun `generic failure without content becomes error`() {
        val holder = DashboardStateHolder()
        val request = holder.beginLoad("session-a")

        assertTrue(holder.acceptFailure(request, message))

        assertEquals(DlaFlowUiState.Error(message), holder.state.contentState)
        assertNull(holder.state.transientMessage)
    }

    @Test
    fun `no access replaces content and clears terminal metadata`() {
        val holder = DashboardStateHolder()
        val request = holder.beginLoad("session-a")

        assertTrue(holder.acceptNoAccess(request))

        assertEquals(DlaFlowUiState.NoAccess, holder.state.contentState)
        assertFalse(holder.state.isRefreshing)
        assertNull(holder.state.activeRequestId)
        assertNull(holder.state.transientMessage)
    }

    @Test
    fun `request must match both id and session key`() {
        val holder = DashboardStateHolder()
        val request = holder.beginLoad("session-a")
        val before = holder.state
        val wrongSession = DashboardLoadRequest(request.requestId, "session-b")

        assertFalse(holder.acceptFailure(wrongSession, message))
        assertEquals(before, holder.state)
    }

    @Test
    fun `unauthorized validates request resets feature and preserves request numbering`() {
        val holder = DashboardStateHolder()
        val stale = holder.beginLoad("session-a")
        val current = holder.beginLoad("session-a")

        assertFalse(holder.acceptUnauthorized(stale))
        assertTrue(holder.acceptUnauthorized(current))
        assertEquals(DashboardUiState(), holder.state)

        val afterReset = holder.beginLoad("session-b")
        assertTrue(afterReset.requestId > current.requestId)
    }
}
