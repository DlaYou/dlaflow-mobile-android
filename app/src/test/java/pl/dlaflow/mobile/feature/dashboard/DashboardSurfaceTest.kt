package pl.dlaflow.mobile.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class DashboardSurfaceTest {
    private val retryableMessage = DlaFlowUiMessage(
        titleRes = R.string.mobile_error_offline_title,
        descriptionRes = R.string.mobile_error_offline_description,
        retryable = true,
    )

    @Test
    fun `loading preserves the legacy zero dashboard surface`() {
        val surface = DashboardUiState().toDashboardSurface()

        assertTrue(surface is DashboardSurface.Dashboard)
        surface as DashboardSurface.Dashboard
        assertNull(surface.content)
        assertNull(surface.notice)
        assertFalse(surface.isRefreshing)
    }

    @Test
    fun `content exposes refresh progress without replacing dashboard`() {
        val content = dashboardDto().toDashboardContent()

        val surface = DashboardUiState(
            contentState = DlaFlowUiState.Content(content),
            isRefreshing = true,
        ).toDashboardSurface()

        assertEquals(
            DashboardSurface.Dashboard(
                content = content,
                notice = null,
                isRefreshing = true,
            ),
            surface,
        )
    }

    @Test
    fun `offline with retained content exposes retry notice and keeps dashboard`() {
        val content = dashboardDto(todayRevenue = 420.0).toDashboardContent()

        val surface = DashboardUiState(
            contentState = DlaFlowUiState.Offline(content),
            transientMessage = retryableMessage,
        ).toDashboardSurface()

        assertEquals(
            DashboardSurface.Dashboard(
                content = content,
                notice = DashboardNotice(retryableMessage, showRetry = true),
                isRefreshing = false,
            ),
            surface,
        )
    }

    @Test
    fun `offline without content becomes controlled retryable failure`() {
        val surface = DashboardUiState(
            contentState = DlaFlowUiState.Offline(),
            transientMessage = retryableMessage,
        ).toDashboardSurface()

        assertEquals(
            DashboardSurface.Failure(
                message = retryableMessage,
                showRetry = true,
                isOffline = true,
            ),
            surface,
        )
    }

    @Test
    fun `terminal error respects message retryability`() {
        val nonRetryableMessage = retryableMessage.copy(retryable = false)

        val surface = DashboardUiState(
            contentState = DlaFlowUiState.Error(nonRetryableMessage),
        ).toDashboardSurface()

        assertEquals(
            DashboardSurface.Failure(
                message = nonRetryableMessage,
                showRetry = false,
                isOffline = false,
            ),
            surface,
        )
    }

    @Test
    fun `no access becomes dedicated surface without dashboard actions`() {
        assertSame(
            DashboardSurface.NoAccess,
            DashboardUiState(contentState = DlaFlowUiState.NoAccess).toDashboardSurface(),
        )
    }

    @Test
    fun `transient retryable message stays visible over retained content`() {
        val content = dashboardDto().toDashboardContent()

        val surface = DashboardUiState(
            contentState = DlaFlowUiState.Content(content),
            transientMessage = retryableMessage,
        ).toDashboardSurface()

        assertEquals(
            DashboardSurface.Dashboard(
                content = content,
                notice = DashboardNotice(retryableMessage, showRetry = true),
                isRefreshing = false,
            ),
            surface,
        )
    }
}
