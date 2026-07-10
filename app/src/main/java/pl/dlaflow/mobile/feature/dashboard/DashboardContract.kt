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
