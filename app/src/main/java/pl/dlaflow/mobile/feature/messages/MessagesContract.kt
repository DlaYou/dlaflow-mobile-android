package pl.dlaflow.mobile.feature.messages

import java.util.Locale
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal const val MESSAGES_PAGE_LIMIT = 20
internal const val MESSAGES_DETAIL_PAGE_LIMIT = 100

internal enum class MessagesFilter { ALL, UNREAD }

internal enum class MessagesChannel(val queryValue: String) {
    ALL("all"),
    MARKETPLACE("marketplace"),
    STORE("store"),
    EMAIL("email"),
    SOCIAL("social"),
}

internal enum class MessageDirection { INBOUND, OUTBOUND, UNKNOWN }

internal data class MessageAttachment(
    val id: String,
    val filename: String,
    val contentType: String,
    val size: Long,
    val status: String,
    val url: String,
)

internal data class MessagePreview(
    val body: String,
    val direction: MessageDirection,
    val messageAt: String,
)

internal data class MessageListItem(
    val id: String,
    val providerId: String,
    val integrationId: String,
    val providerLabel: String,
    val customerName: String,
    val customerLogin: String,
    val subject: String,
    val preview: MessagePreview?,
    val lastMessageAt: String,
    val messageCount: Int,
    val orderId: String?,
    val orderNumber: String?,
    val readAt: String?,
    val status: String,
    val channel: MessagesChannel,
) {
    val isUnread: Boolean
        get() = readAt == null && status.trim().lowercase(Locale.ROOT) in setOf("unread", "new")

    val isNew: Boolean
        get() = isUnread && status.trim().equals("new", ignoreCase = true)
}

internal data class MessageBubble(
    val id: String,
    val author: String,
    val direction: MessageDirection,
    val body: String,
    val messageAt: String,
    val status: String,
    val attachments: List<MessageAttachment>,
    val requestId: String? = null,
)

internal data class MessageOperation(
    val operationId: String,
    val messageId: String?,
    val queued: Boolean,
    val duplicate: Boolean,
    val status: String,
)

internal data class MessageThreadDetail(
    val id: String,
    val providerId: String,
    val integrationId: String,
    val providerLabel: String,
    val customerName: String,
    val customerLogin: String,
    val customerEmail: String?,
    val subject: String,
    val lastMessageAt: String,
    val readAt: String?,
    val status: String,
    val orderId: String?,
    val orderNumber: String?,
    val messages: List<MessageBubble>,
    val nextCursor: String?,
    val customerContext: MessageCustomerContext?,
    val relatedOrder: MessageRelatedOrder? = null,
)

internal data class MessageRelatedOrder(
    val id: String,
    val orderNumber: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val statusTone: String,
    val statusColor: String,
    val items: List<MessageRelatedOrderItem>,
)

internal data class MessageRelatedOrderItem(
    val name: String,
    val image: String,
    val sku: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
)

internal data class MessageCustomerContext(
    val orderCount: Int,
    val totalOrderAmount: Double,
    val currency: String,
    val customerSince: String,
    val activeConversationCount: Int,
)

internal data class MessagesContent(
    val items: List<MessageListItem>,
    val total: Int,
    val nextCursor: String?,
    val unreadCount: Int,
)

internal fun MessagesContent.countFor(filter: MessagesFilter): Int = when (filter) {
    MessagesFilter.ALL -> total.coerceAtLeast(0)
    MessagesFilter.UNREAD -> unreadCount.coerceAtLeast(0)
}

internal data class MessagesQuery(
    val search: String = "",
    val filter: MessagesFilter = MessagesFilter.ALL,
    val channel: MessagesChannel = MessagesChannel.ALL,
)

internal data class MessagesUiState(
    val query: MessagesQuery = MessagesQuery(),
    val listState: DlaFlowUiState<MessagesContent> = DlaFlowUiState.Loading,
    val route: MessagesRoute = MessagesRoute.List,
    val detailState: DlaFlowUiState<MessageThreadDetail>? = null,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isMarkingRead: Boolean = false,
    val isRefreshingThread: Boolean = false,
    val isSendingReply: Boolean = false,
    val activeListRequestId: Long? = null,
    val activeDetailRequestId: Long? = null,
    val activeMutationRequestId: Long? = null,
    val transientMessage: DlaFlowUiMessage? = null,
)

internal sealed interface MessagesRoute {
    data object List : MessagesRoute
    data class Detail(val threadId: String) : MessagesRoute
}

internal fun MessagesUiState.listContentOrNull(): MessagesContent? = when (val current = listState) {
    is DlaFlowUiState.Content -> current.data
    is DlaFlowUiState.Offline -> current.lastContent
    else -> null
}

internal fun MessagesUiState.detailContentOrNull(): MessageThreadDetail? = when (val current = detailState) {
    is DlaFlowUiState.Content -> current.data
    is DlaFlowUiState.Offline -> current.lastContent
    else -> null
}

internal fun MessagesUiState.visibleItems(): List<MessageListItem> =
    listContentOrNull()?.items.orEmpty().filter { item ->
        val filterMatch = query.filter == MessagesFilter.ALL || item.isUnread
        val channelMatch = query.channel == MessagesChannel.ALL || item.channel == query.channel
        filterMatch && channelMatch
    }

internal enum class MessagesListLoadMode { RESET, REFRESH, LOAD_MORE }

internal enum class MessagesDetailLoadMode { INITIAL, REFRESH, LOAD_MORE }

internal enum class MessagesMutationKind { MARK_READ, REFRESH_THREAD, REPLY }

internal data class MessagesListRequest(
    val requestId: Long,
    internal val sessionKey: String,
    val query: MessagesQuery,
    val cursor: String?,
    val mode: MessagesListLoadMode,
)

internal data class MessagesDetailRequest(
    val requestId: Long,
    internal val sessionKey: String,
    val threadId: String,
    val cursor: String? = null,
    val mode: MessagesDetailLoadMode = MessagesDetailLoadMode.INITIAL,
)

internal data class MessagesMutationRequest(
    val requestId: Long,
    internal val sessionKey: String,
    val threadId: String,
    val requestIdempotencyKey: String? = null,
    val kind: MessagesMutationKind = MessagesMutationKind.MARK_READ,
)

internal sealed interface MessagesAction {
    data class SearchChanged(val search: String) : MessagesAction
    data class FilterChanged(val filter: MessagesFilter) : MessagesAction
    data class ChannelChanged(val channel: MessagesChannel) : MessagesAction
    data object Refresh : MessagesAction
    data object LoadMore : MessagesAction
    data class OpenThread(val threadId: String) : MessagesAction
    data object CloseDetail : MessagesAction
    data object MarkThreadRead : MessagesAction
    data object RefreshThread : MessagesAction
    data class SendReply(val body: String, val requestId: String) : MessagesAction
    data class OpenRelatedOrder(val orderNumber: String) : MessagesAction
    data object Retry : MessagesAction
}

internal sealed interface MessagesEffect {
    data class OpenOrder(val orderId: String?, val orderNumber: String?) : MessagesEffect
    data object OpenInbox : MessagesEffect
    data class ShowSafeExplanation(val message: DlaFlowUiMessage) : MessagesEffect
}

internal sealed interface MessagesOperation {
    data class ListReset(val query: MessagesQuery) : MessagesOperation
    data object ListRefresh : MessagesOperation
    data object LoadMore : MessagesOperation
    data class Detail(val threadId: String, val mode: MessagesDetailLoadMode) : MessagesOperation
    data class Mutation(val kind: MessagesMutationKind, val threadId: String, val body: String? = null, val requestId: String? = null) : MessagesOperation
}
