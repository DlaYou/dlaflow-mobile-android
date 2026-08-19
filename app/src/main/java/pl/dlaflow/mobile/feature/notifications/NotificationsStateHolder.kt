package pl.dlaflow.mobile.feature.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal class NotificationsStateHolder {
    var state by mutableStateOf(NotificationsUiState())
        private set

    private var nextLoadRequestId = 0L
    private var nextMutationRequestId = 0L
    private var activeLoadSessionKey: String? = null
    private var activeMutationSessionKey: String? = null
    private var pendingLoadUnauthorizedRequestId: Long? = null
    private var pendingMutationUnauthorizedRequestId: Long? = null

    fun setDashboardPreview(items: List<NotificationItem>) {
        if (state.notificationsState !is DlaFlowUiState.Loading || state.canonicalContentOrNull() != null) return
        state = state.copy(
            dashboardPreview = items
                .asSequence()
                .filter { it.id.isNotBlank() }
                .distinctBy(NotificationItem::id)
                .map { it.copy() }
                .toList(),
        )
    }

    fun selectFilter(filter: NotificationFilter) {
        state = state.copy(selectedFilter = filter)
    }

    fun beginLoad(sessionKey: String): NotificationsLoadRequest? {
        if (state.isMarkingRead || pendingMutationUnauthorizedRequestId != null) return null
        return startLoad(sessionKey)
    }

    fun acceptLoadSuccess(request: NotificationsLoadRequest, content: NotificationsContent): Boolean {
        if (!matches(request)) return false
        val acceptedContent = content.copy(items = content.items.map { it.copy() })
        finishLoad(
            notificationsState = if (acceptedContent.items.isEmpty()) {
                DlaFlowUiState.Empty
            } else {
                DlaFlowUiState.Content(acceptedContent)
            },
        )
        return true
    }

    fun acceptLoadOffline(request: NotificationsLoadRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        finishLoad(
            notificationsState = DlaFlowUiState.Offline(state.canonicalContentOrNull()),
            transientMessage = message,
        )
        return true
    }

    fun acceptLoadFailure(request: NotificationsLoadRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        val content = state.canonicalContentOrNull()
        finishLoad(
            notificationsState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message),
            transientMessage = message.takeIf { content != null },
        )
        return true
    }

    fun acceptLoadNoAccess(request: NotificationsLoadRequest): Boolean {
        if (!matches(request)) return false
        finishLoad(DlaFlowUiState.NoAccess)
        return true
    }

    fun acceptLoadUnauthorized(
        request: NotificationsLoadRequest,
        terminalMessage: DlaFlowUiMessage? = null,
    ): Boolean {
        if (!matches(request)) return false
        activeLoadSessionKey = null
        pendingLoadUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        val content = state.canonicalContentOrNull()
        state = state.copy(
            notificationsState = when {
                content != null -> DlaFlowUiState.Content(content)
                terminalMessage != null -> DlaFlowUiState.Error(terminalMessage)
                else -> state.notificationsState
            },
            dashboardPreview = if (terminalMessage == null && content == null) {
                state.dashboardPreview
            } else {
                emptyList()
            },
            isRefreshing = false,
            activeLoadRequestId = null,
            transientMessage = terminalMessage.takeIf { content != null },
        )
        return true
    }

    fun beginLoadUnauthorizedRetry(request: NotificationsLoadRequest): NotificationsLoadRequest? {
        if (pendingLoadUnauthorizedRequestId != request.requestId) return null
        pendingLoadUnauthorizedRequestId = null
        if (state.isMarkingRead || pendingMutationUnauthorizedRequestId != null) return null
        return startLoad(request.sessionKey)
    }

    fun acceptLoadSessionUnconfirmed(
        request: NotificationsLoadRequest,
        message: DlaFlowUiMessage,
    ): Boolean {
        if (pendingLoadUnauthorizedRequestId != request.requestId) return false
        pendingLoadUnauthorizedRequestId = null
        val content = state.canonicalContentOrNull()
        state = state.copy(
            notificationsState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message),
            dashboardPreview = emptyList(),
            isRefreshing = false,
            activeLoadRequestId = null,
            transientMessage = message.takeIf { content != null },
        )
        return true
    }

    fun beginMarkVisibleRead(sessionKey: String): NotificationsMutationRequest? {
        if (
            state.activeLoadRequestId != null ||
            pendingLoadUnauthorizedRequestId != null ||
            state.isMarkingRead ||
            pendingMutationUnauthorizedRequestId != null
        ) {
            return null
        }
        val ids = state.visibleCanonicalItems()
            .asSequence()
            .filter(NotificationItem::isUnread)
            .map { it.id.trim() }
            .filter(String::isNotEmpty)
            .distinct()
            .take(NOTIFICATIONS_PAGE_LIMIT)
            .toList()
        if (ids.isEmpty()) return null
        return startMutation(sessionKey, ids)
    }

    fun acceptMutationSuccess(request: NotificationsMutationRequest): Boolean {
        if (!matches(request)) return false
        finishMutation()
        return true
    }

    fun acceptMutationOffline(
        request: NotificationsMutationRequest,
        message: DlaFlowUiMessage,
    ): Boolean {
        if (!matches(request)) return false
        val content = state.canonicalContentOrNull()
        activeMutationSessionKey = null
        pendingMutationUnauthorizedRequestId = null
        state = state.copy(
            notificationsState = DlaFlowUiState.Offline(content),
            isMarkingRead = false,
            activeMutationRequestId = null,
            transientMessage = message,
        )
        return true
    }

    fun acceptMutationFailure(
        request: NotificationsMutationRequest,
        message: DlaFlowUiMessage,
    ): Boolean {
        if (!matches(request)) return false
        val content = state.canonicalContentOrNull()
        activeMutationSessionKey = null
        pendingMutationUnauthorizedRequestId = null
        state = state.copy(
            notificationsState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message),
            isMarkingRead = false,
            activeMutationRequestId = null,
            transientMessage = message.takeIf { content != null },
        )
        return true
    }

    fun acceptMutationNoAccess(request: NotificationsMutationRequest): Boolean {
        if (!matches(request)) return false
        activeMutationSessionKey = null
        pendingMutationUnauthorizedRequestId = null
        state = state.copy(
            notificationsState = DlaFlowUiState.NoAccess,
            dashboardPreview = emptyList(),
            isMarkingRead = false,
            activeMutationRequestId = null,
            transientMessage = null,
        )
        return true
    }

    fun acceptMutationUnauthorized(
        request: NotificationsMutationRequest,
        terminalMessage: DlaFlowUiMessage? = null,
    ): Boolean {
        if (!matches(request)) return false
        activeMutationSessionKey = null
        pendingMutationUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        state = state.copy(
            isMarkingRead = terminalMessage == null,
            activeMutationRequestId = null,
            transientMessage = terminalMessage,
        )
        return true
    }

    fun beginMutationUnauthorizedRetry(request: NotificationsMutationRequest): NotificationsMutationRequest? {
        if (pendingMutationUnauthorizedRequestId != request.requestId) return null
        pendingMutationUnauthorizedRequestId = null
        if (state.activeLoadRequestId != null || state.canonicalContentOrNull() == null) return null
        return startMutation(request.sessionKey, request.notificationIds)
    }

    fun acceptMutationSessionUnconfirmed(
        request: NotificationsMutationRequest,
        message: DlaFlowUiMessage,
    ): Boolean {
        if (pendingMutationUnauthorizedRequestId != request.requestId) return false
        pendingMutationUnauthorizedRequestId = null
        state = state.copy(
            isMarkingRead = false,
            activeMutationRequestId = null,
            transientMessage = message,
        )
        return true
    }

    fun effectFor(notificationId: String): NotificationsEffect? {
        val item = state.visibleItems().firstOrNull { it.id == notificationId } ?: return null
        return when (val destination = item.destination) {
            NotificationDestination.Orders -> NotificationsEffect.OpenOrders
            NotificationDestination.Products -> NotificationsEffect.OpenProducts
            NotificationDestination.Messages -> NotificationsEffect.OpenMessages
            NotificationDestination.PhotoTasks -> NotificationsEffect.OpenPhotoTasks
            NotificationDestination.LogsSummary,
            NotificationDestination.ContactAdmin,
            NotificationDestination.Unsupported,
            -> NotificationsEffect.ShowSafeExplanation(destination)
        }
    }

    fun reset() {
        activeLoadSessionKey = null
        activeMutationSessionKey = null
        pendingLoadUnauthorizedRequestId = null
        pendingMutationUnauthorizedRequestId = null
        state = NotificationsUiState()
    }

    private fun startLoad(sessionKey: String): NotificationsLoadRequest {
        val request = NotificationsLoadRequest(++nextLoadRequestId, sessionKey)
        val content = state.canonicalContentOrNull()
        activeLoadSessionKey = sessionKey
        pendingLoadUnauthorizedRequestId = null
        state = state.copy(
            notificationsState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Loading,
            isRefreshing = content != null,
            activeLoadRequestId = request.requestId,
            transientMessage = null,
        )
        return request
    }

    private fun startMutation(
        sessionKey: String,
        notificationIds: List<String>,
    ): NotificationsMutationRequest {
        val request = NotificationsMutationRequest(
            requestId = ++nextMutationRequestId,
            sessionKey = sessionKey,
            notificationIds = notificationIds.toList(),
        )
        activeMutationSessionKey = sessionKey
        pendingMutationUnauthorizedRequestId = null
        state = state.copy(
            isMarkingRead = true,
            activeMutationRequestId = request.requestId,
            transientMessage = null,
        )
        return request
    }

    private fun finishLoad(
        notificationsState: DlaFlowUiState<NotificationsContent>,
        transientMessage: DlaFlowUiMessage? = null,
    ) {
        activeLoadSessionKey = null
        pendingLoadUnauthorizedRequestId = null
        state = state.copy(
            notificationsState = notificationsState,
            dashboardPreview = emptyList(),
            isRefreshing = false,
            activeLoadRequestId = null,
            transientMessage = transientMessage,
        )
    }

    private fun finishMutation() {
        activeMutationSessionKey = null
        pendingMutationUnauthorizedRequestId = null
        state = state.copy(
            isMarkingRead = false,
            activeMutationRequestId = null,
            transientMessage = null,
        )
    }

    private fun matches(request: NotificationsLoadRequest): Boolean =
        state.activeLoadRequestId == request.requestId && activeLoadSessionKey == request.sessionKey

    private fun matches(request: NotificationsMutationRequest): Boolean =
        state.activeMutationRequestId == request.requestId && activeMutationSessionKey == request.sessionKey
}
