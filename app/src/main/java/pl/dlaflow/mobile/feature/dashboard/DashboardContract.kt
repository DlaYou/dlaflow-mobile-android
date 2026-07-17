package pl.dlaflow.mobile.feature.dashboard

import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal data class DashboardKpis(
    val newOrders: Int,
    val toShip: Int,
    val overdueOrProblems: Int,
    val messages: Int,
)

internal data class DashboardNotification(
    val id: String,
    val title: String,
    val description: String,
    val tone: String,
    val source: String,
    val account: String,
    val occurredAt: String,
    val readAt: String?,
    val actionType: String,
    val actionLabel: String,
)

internal data class DashboardNotificationSummary(
    val unreadCount: Int,
    val unreadAttentionCount: Int,
)

internal data class DashboardTrendPoint(
    val date: String,
    val orders: Int,
    val revenue: Double,
)

internal data class DashboardCallerIdStatus(
    val enabled: Boolean,
    val label: String,
)

internal data class DashboardPhotoTask(
    val id: String,
    val productName: String,
    val productSku: String,
    val productImage: String,
    val status: String,
    val mediaCount: Int,
    val maxPhotos: Int,
    val expiresAt: String,
)

internal data class DashboardContent(
    val userName: String,
    val tenantName: String,
    val todayRevenue: Double,
    val revenueChangePercent: Double,
    val kpis: DashboardKpis,
    val notificationSummary: DashboardNotificationSummary,
    val notifications: List<DashboardNotification>,
    val activePhotoTask: DashboardPhotoTask?,
    val callerIdStatus: DashboardCallerIdStatus,
    val trend: List<DashboardTrendPoint>,
    val generatedAt: String,
)

internal data class DashboardUiState(
    val contentState: DlaFlowUiState<DashboardContent> = DlaFlowUiState.Loading,
    val isRefreshing: Boolean = false,
    val activeRequestId: Long? = null,
    val transientMessage: DlaFlowUiMessage? = null,
)

internal data class DashboardNotice(
    val message: DlaFlowUiMessage,
    val showRetry: Boolean,
)

internal sealed interface DashboardSurface {
    data class Dashboard(
        val content: DashboardContent?,
        val notice: DashboardNotice?,
        val isRefreshing: Boolean,
    ) : DashboardSurface

    data class Failure(
        val message: DlaFlowUiMessage?,
        val showRetry: Boolean,
        val isOffline: Boolean,
    ) : DashboardSurface

    data object NoAccess : DashboardSurface
}

internal fun DashboardUiState.toDashboardSurface(): DashboardSurface = when (val current = contentState) {
    DlaFlowUiState.Loading -> DashboardSurface.Dashboard(
        content = null,
        notice = null,
        isRefreshing = isRefreshing,
    )

    is DlaFlowUiState.Content -> DashboardSurface.Dashboard(
        content = current.data,
        notice = transientMessage?.let { DashboardNotice(it, showRetry = it.retryable) },
        isRefreshing = isRefreshing,
    )

    is DlaFlowUiState.Offline -> current.lastContent?.let { content ->
        DashboardSurface.Dashboard(
            content = content,
            notice = transientMessage?.let { DashboardNotice(it, showRetry = true) },
            isRefreshing = isRefreshing,
        )
    } ?: DashboardSurface.Failure(
        message = transientMessage,
        showRetry = true,
        isOffline = true,
    )

    is DlaFlowUiState.Error -> DashboardSurface.Failure(
        message = current.message,
        showRetry = current.message.retryable,
        isOffline = false,
    )

    DlaFlowUiState.NoAccess -> DashboardSurface.NoAccess
    DlaFlowUiState.Empty -> DashboardSurface.Failure(
        message = transientMessage,
        showRetry = false,
        isOffline = false,
    )
}

internal data class DashboardLayoutPolicy(
    val revenueCardHeightDp: Int,
    val kpiTileHeightDp: Int,
    val quickActionHeightDp: Int,
    val quickActionLabelMaxLines: Int,
    val quickActionSubtitleMaxLines: Int,
    val stackRevenueComparison: Boolean,
)

internal fun dashboardLayoutPolicy(widthDp: Int, fontScale: Float): DashboardLayoutPolicy {
    val largeTextOnNarrowScreen = widthDp <= 380 && fontScale >= 1.2f
    return if (largeTextOnNarrowScreen) {
        DashboardLayoutPolicy(
            revenueCardHeightDp = 124,
            kpiTileHeightDp = 116,
            quickActionHeightDp = 116,
            quickActionLabelMaxLines = 2,
            quickActionSubtitleMaxLines = 2,
            stackRevenueComparison = true,
        )
    } else {
        DashboardLayoutPolicy(
            revenueCardHeightDp = 104,
            kpiTileHeightDp = 98,
            quickActionHeightDp = 84,
            quickActionLabelMaxLines = 1,
            quickActionSubtitleMaxLines = 1,
            stackRevenueComparison = false,
        )
    }
}

internal fun DashboardUiState.contentOrNull(): DashboardContent? = when (val current = contentState) {
    is DlaFlowUiState.Content -> current.data
    is DlaFlowUiState.Offline -> current.lastContent
    else -> null
}

internal data class DashboardLoadRequest(
    val requestId: Long,
    internal val sessionKey: String,
)

internal sealed interface DashboardAction {
    data object Refresh : DashboardAction
    data object ScanPackage : DashboardAction
    data object OpenProductWork : DashboardAction
    data object OpenStatistics : DashboardAction
    data object OpenProducts : DashboardAction
    data object OpenNotifications : DashboardAction
    data class TakePhoto(val taskId: String) : DashboardAction
    data class PickPhoto(val taskId: String) : DashboardAction
    data class CompletePhotoTask(val taskId: String) : DashboardAction
}

internal enum class DashboardFeedback { REFRESHING, REFRESHED, LOAD_FAILED }
