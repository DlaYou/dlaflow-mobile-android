package pl.dlaflow.mobile.feature.orders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal class OrdersStateHolder {
    var state by mutableStateOf(OrdersUiState())
        private set

    private var nextListRequestId = 0L
    private var nextDetailRequestId = 0L
    private var activeListSessionKey: String? = null
    private var activeDetailSessionKey: String? = null
    private var pendingListUnauthorizedRequestId: Long? = null
    private var pendingDetailUnauthorizedRequestId: Long? = null

    fun beginListReset(sessionKey: String, query: OrdersQuery): OrdersListRequest {
        invalidateDetail()
        val request = newListRequest(sessionKey, query, offset = 0, OrdersListLoadMode.RESET)
        state = OrdersUiState(
            query = query,
            listState = DlaFlowUiState.Loading,
            activeListRequestId = request.requestId,
        )
        return request
    }

    fun beginListRefresh(sessionKey: String): OrdersListRequest {
        val content = state.listContentOrNull()
        val request = newListRequest(sessionKey, state.query, offset = 0, OrdersListLoadMode.REFRESH)
        state = state.copy(
            listState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Loading,
            route = OrdersRoute.List,
            detailState = null,
            isRefreshing = content != null,
            isLoadingMore = false,
            activeListRequestId = request.requestId,
            activeDetailRequestId = null,
            transientMessage = null,
        )
        invalidateDetail()
        return request
    }

    fun beginLoadMore(sessionKey: String): OrdersListRequest? {
        if (state.activeListRequestId != null || state.isLoadingMore) return null
        val content = state.listContentOrNull() ?: return null
        val offset = content.nextOffset ?: return null
        val request = newListRequest(sessionKey, state.query, offset, OrdersListLoadMode.LOAD_MORE)
        state = state.copy(
            listState = DlaFlowUiState.Content(content),
            isRefreshing = false,
            isLoadingMore = true,
            activeListRequestId = request.requestId,
            transientMessage = null,
        )
        return request
    }

    fun beginDetailLoad(sessionKey: String, orderNumber: String): OrdersDetailRequest {
        val request = OrdersDetailRequest(
            requestId = ++nextDetailRequestId,
            sessionKey = sessionKey,
            orderNumber = orderNumber,
        )
        activeDetailSessionKey = sessionKey
        pendingDetailUnauthorizedRequestId = null
        state = state.copy(
            route = OrdersRoute.Detail(orderNumber),
            detailState = DlaFlowUiState.Loading,
            activeDetailRequestId = request.requestId,
            transientMessage = null,
        )
        return request
    }

    fun acceptListSuccess(request: OrdersListRequest, content: OrdersListContent): Boolean {
        if (!matches(request)) return false
        val accepted = if (request.mode == OrdersListLoadMode.LOAD_MORE) {
            mergePages(state.listContentOrNull(), content)
        } else {
            content.copy(items = content.items.toList())
        }
        finishList(
            listState = if (accepted.items.isEmpty()) DlaFlowUiState.Empty else DlaFlowUiState.Content(accepted),
        )
        return true
    }

    fun acceptListOffline(request: OrdersListRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        finishList(
            listState = DlaFlowUiState.Offline(state.listContentOrNull()),
            transientMessage = message,
        )
        return true
    }

    fun acceptListFailure(request: OrdersListRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        val content = state.listContentOrNull()
        finishList(
            listState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message),
            transientMessage = message.takeIf { content != null },
        )
        return true
    }

    fun acceptListNoAccess(request: OrdersListRequest): Boolean {
        if (!matches(request)) return false
        setNoAccess()
        return true
    }

    fun acceptListUnauthorized(
        request: OrdersListRequest,
        terminalMessage: DlaFlowUiMessage? = null,
    ): Boolean {
        if (!matches(request)) return false
        val content = state.listContentOrNull()
        activeListSessionKey = null
        pendingListUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        state = state.copy(
            listState = when {
                content != null -> DlaFlowUiState.Content(content)
                terminalMessage != null -> DlaFlowUiState.Error(terminalMessage)
                else -> state.listState
            },
            isRefreshing = false,
            isLoadingMore = false,
            activeListRequestId = null,
            transientMessage = terminalMessage.takeIf { content != null },
        )
        return true
    }

    fun acceptListSessionUnconfirmed(
        request: OrdersListRequest,
        message: DlaFlowUiMessage,
    ): Boolean {
        if (pendingListUnauthorizedRequestId != request.requestId) return false
        pendingListUnauthorizedRequestId = null
        val content = state.listContentOrNull()
        state = state.copy(
            listState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message),
            transientMessage = message.takeIf { content != null },
        )
        return true
    }

    fun acceptDetailSuccess(request: OrdersDetailRequest, content: OrderDetailContent): Boolean {
        if (!matches(request)) return false
        finishDetail(DlaFlowUiState.Content(content.withListImageFallback()))
        return true
    }

    fun acceptDetailOffline(request: OrdersDetailRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        finishDetail(
            detailState = DlaFlowUiState.Offline(state.detailContentOrNull()),
            transientMessage = message,
        )
        return true
    }

    fun acceptDetailFailure(request: OrdersDetailRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        finishDetail(DlaFlowUiState.Error(message))
        return true
    }

    fun acceptDetailNoAccess(request: OrdersDetailRequest): Boolean {
        if (!matches(request)) return false
        setNoAccess()
        return true
    }

    fun acceptDetailUnauthorized(
        request: OrdersDetailRequest,
        terminalMessage: DlaFlowUiMessage? = null,
    ): Boolean {
        if (!matches(request)) return false
        activeDetailSessionKey = null
        pendingDetailUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        state = state.copy(
            detailState = terminalMessage?.let { DlaFlowUiState.Error(it) } ?: state.detailState,
            activeDetailRequestId = null,
            transientMessage = terminalMessage,
        )
        return true
    }

    fun acceptDetailSessionUnconfirmed(
        request: OrdersDetailRequest,
        message: DlaFlowUiMessage,
    ): Boolean {
        if (pendingDetailUnauthorizedRequestId != request.requestId) return false
        pendingDetailUnauthorizedRequestId = null
        state = state.copy(
            detailState = DlaFlowUiState.Error(message),
            transientMessage = message,
        )
        return true
    }

    fun closeDetail() {
        invalidateDetail()
        state = state.copy(
            route = OrdersRoute.List,
            detailState = null,
            activeDetailRequestId = null,
            transientMessage = null,
        )
    }

    fun reset() {
        activeListSessionKey = null
        activeDetailSessionKey = null
        pendingListUnauthorizedRequestId = null
        pendingDetailUnauthorizedRequestId = null
        state = OrdersUiState()
    }

    private fun newListRequest(
        sessionKey: String,
        query: OrdersQuery,
        offset: Int,
        mode: OrdersListLoadMode,
    ): OrdersListRequest {
        val request = OrdersListRequest(
            requestId = ++nextListRequestId,
            sessionKey = sessionKey,
            query = query,
            offset = offset,
            mode = mode,
        )
        activeListSessionKey = sessionKey
        pendingListUnauthorizedRequestId = null
        return request
    }

    private fun matches(request: OrdersListRequest): Boolean =
        state.activeListRequestId == request.requestId && activeListSessionKey == request.sessionKey

    private fun matches(request: OrdersDetailRequest): Boolean =
        state.activeDetailRequestId == request.requestId && activeDetailSessionKey == request.sessionKey

    private fun finishList(
        listState: DlaFlowUiState<OrdersListContent>,
        transientMessage: DlaFlowUiMessage? = null,
    ) {
        activeListSessionKey = null
        pendingListUnauthorizedRequestId = null
        state = state.copy(
            listState = listState,
            isRefreshing = false,
            isLoadingMore = false,
            activeListRequestId = null,
            transientMessage = transientMessage,
        )
    }

    private fun finishDetail(
        detailState: DlaFlowUiState<OrderDetailContent>,
        transientMessage: DlaFlowUiMessage? = null,
    ) {
        activeDetailSessionKey = null
        pendingDetailUnauthorizedRequestId = null
        state = state.copy(
            detailState = detailState,
            activeDetailRequestId = null,
            transientMessage = transientMessage,
        )
    }

    private fun invalidateDetail() {
        activeDetailSessionKey = null
        pendingDetailUnauthorizedRequestId = null
    }

    private fun OrderDetailContent.withListImageFallback(): OrderDetailContent {
        val listItem = state.listContentOrNull()?.items?.firstOrNull { item ->
            item.orderNumber == orderNumber || item.id == id
        } ?: return this
        if (items.isEmpty()) return this

        val enrichedItems = items.mapIndexed { index, item ->
            if (item.image.isCanonicalMobileMediaReference()) {
                item
            } else {
                val matchedProduct = listItem.products.firstOrNull { product ->
                    product.image.isNotBlank() && (
                        item.sku.isNotBlank() && product.sku.equals(item.sku, ignoreCase = true) ||
                            item.name.isNotBlank() && product.name.equals(item.name, ignoreCase = true)
                        )
                }
                item.copy(
                    image = matchedProduct?.image.orEmpty().ifBlank {
                        if (index == 0) listItem.thumbnailUrl else ""
                    },
                )
            }
        }
        return copy(items = enrichedItems)
    }

    private fun String.isCanonicalMobileMediaReference(): Boolean {
        val normalized = trim()
        return normalized.startsWith("/api/mobile/orders/media/") ||
            normalized.startsWith("/api/mobile/products/media/")
    }

    private fun setNoAccess() {
        activeListSessionKey = null
        activeDetailSessionKey = null
        pendingListUnauthorizedRequestId = null
        pendingDetailUnauthorizedRequestId = null
        state = state.copy(
            listState = DlaFlowUiState.NoAccess,
            route = OrdersRoute.List,
            detailState = null,
            isRefreshing = false,
            isLoadingMore = false,
            activeListRequestId = null,
            activeDetailRequestId = null,
            transientMessage = null,
        )
    }

    private fun mergePages(
        current: OrdersListContent?,
        incoming: OrdersListContent,
    ): OrdersListContent {
        val merged = current?.items.orEmpty().toMutableList()
        incoming.items.forEach { candidate ->
            val existingIndex = merged.indexOfFirst { existing -> existing.sameIdentityAs(candidate) }
            if (existingIndex >= 0) {
                merged[existingIndex] = candidate
            } else {
                merged.add(candidate)
            }
        }
        return OrdersListContent(
            items = merged.toList(),
            total = incoming.total,
            nextOffset = incoming.nextOffset,
        )
    }

    private fun OrdersListItem.sameIdentityAs(other: OrdersListItem): Boolean =
        (id.isNotBlank() && other.id.isNotBlank() && id == other.id) ||
            (orderNumber.isNotBlank() && other.orderNumber.isNotBlank() && orderNumber == other.orderNumber)
}
