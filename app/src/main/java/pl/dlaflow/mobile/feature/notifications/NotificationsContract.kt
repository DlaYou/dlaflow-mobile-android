package pl.dlaflow.mobile.feature.notifications

import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal const val NOTIFICATIONS_PAGE_LIMIT = 20

internal enum class NotificationFilter {
    ALL,
    ATTENTION,
    UNREAD,
}

internal enum class NotificationTone {
    Neutral,
    Info,
    Success,
    Attention,
}

internal sealed interface NotificationDestination {
    data object Orders : NotificationDestination
    data object Products : NotificationDestination
    data object Messages : NotificationDestination
    data object PhotoTasks : NotificationDestination
    data object LogsSummary : NotificationDestination
    data object ContactAdmin : NotificationDestination
    data object Unsupported : NotificationDestination
}

internal data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val tone: NotificationTone,
    val source: String,
    val account: String,
    val occurredAt: String,
    val readAt: String?,
    val actionLabel: String?,
    val destination: NotificationDestination,
) {
    val isUnread: Boolean
        get() = readAt.isNullOrBlank()
}

internal data class NotificationsContent(
    val items: List<NotificationItem>,
    val attentionCount: Int,
    val unreadAttentionCount: Int,
    val unreadCount: Int,
)

internal data class NotificationsUiState(
    val notificationsState: DlaFlowUiState<NotificationsContent> = DlaFlowUiState.Loading,
    val dashboardPreview: List<NotificationItem> = emptyList(),
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val isRefreshing: Boolean = false,
    val isMarkingRead: Boolean = false,
    val activeLoadRequestId: Long? = null,
    val activeMutationRequestId: Long? = null,
    val transientMessage: DlaFlowUiMessage? = null,
)

internal fun NotificationsUiState.canonicalContentOrNull(): NotificationsContent? =
    when (val current = notificationsState) {
        is DlaFlowUiState.Content -> current.data
        is DlaFlowUiState.Offline -> current.lastContent
        else -> null
    }

internal fun NotificationsUiState.visibleCanonicalItems(): List<NotificationItem> =
    canonicalContentOrNull()?.items.orEmpty().filteredBy(selectedFilter)

internal fun NotificationsUiState.visibleItems(): List<NotificationItem> {
    val canonical = canonicalContentOrNull()
    if (canonical != null) return canonical.items.filteredBy(selectedFilter)
    if (notificationsState !is DlaFlowUiState.Loading) return emptyList()
    return dashboardPreview.filteredBy(selectedFilter)
}

private fun List<NotificationItem>.filteredBy(filter: NotificationFilter): List<NotificationItem> = when (filter) {
    NotificationFilter.ALL -> this
    NotificationFilter.ATTENTION -> filter { it.tone == NotificationTone.Attention }
    NotificationFilter.UNREAD -> filter(NotificationItem::isUnread)
}

internal data class NotificationsLoadRequest(
    val requestId: Long,
    internal val sessionKey: String,
)

internal data class NotificationsMutationRequest(
    val requestId: Long,
    internal val sessionKey: String,
    val notificationIds: List<String>,
)

internal sealed interface NotificationsAction {
    data object Open : NotificationsAction
    data object Refresh : NotificationsAction
    data class FilterChanged(val filter: NotificationFilter) : NotificationsAction
    data object MarkVisibleRead : NotificationsAction
    data class OpenItem(val notificationId: String) : NotificationsAction
}

internal sealed interface NotificationsEffect {
    data object OpenOrders : NotificationsEffect
    data object OpenProducts : NotificationsEffect
    data object OpenMessages : NotificationsEffect
    data object OpenPhotoTasks : NotificationsEffect
    data class ShowSafeExplanation(val destination: NotificationDestination) : NotificationsEffect
}
