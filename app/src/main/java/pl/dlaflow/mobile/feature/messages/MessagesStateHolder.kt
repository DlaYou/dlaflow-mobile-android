package pl.dlaflow.mobile.feature.messages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal class MessagesStateHolder {
    var state by mutableStateOf(MessagesUiState())
        private set

    private var nextListRequestId = 0L
    private var nextDetailRequestId = 0L
    private var nextMutationRequestId = 0L
    private var activeListSessionKey: String? = null
    private var activeDetailSessionKey: String? = null
    private var activeMutationSessionKey: String? = null
    private var pendingListUnauthorizedRequestId: Long? = null
    private var pendingDetailUnauthorizedRequestId: Long? = null
    private var pendingMutationUnauthorizedRequestId: Long? = null
    private val inFlightListCursors = mutableSetOf<String>()
    private val consumedListCursors = mutableSetOf<String>()
    private val inFlightDetailCursors = mutableSetOf<String>()
    private val consumedDetailCursors = mutableSetOf<String>()
    private val seenReplyRequestIds = mutableSetOf<String>()

    fun beginListReset(sessionKey: String, query: MessagesQuery): MessagesListRequest {
        invalidateDetail()
        invalidateMutation()
        inFlightListCursors.clear()
        consumedListCursors.clear()
        val normalizedQuery = query.copy(search = query.search.trim())
        val request = newListRequest(sessionKey, normalizedQuery, null, MessagesListLoadMode.RESET)
        state = MessagesUiState(
            query = normalizedQuery,
            listState = DlaFlowUiState.Loading,
            route = MessagesRoute.List,
            activeListRequestId = request.requestId,
        )
        return request
    }

    fun beginListRefresh(sessionKey: String): MessagesListRequest {
        val content = state.listContentOrNull()
        val request = newListRequest(sessionKey, state.query, null, MessagesListLoadMode.REFRESH)
        state = state.copy(
            listState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Loading,
            route = MessagesRoute.List,
            detailState = null,
            isRefreshing = content != null,
            isLoadingMore = false,
            activeListRequestId = request.requestId,
            activeDetailRequestId = null,
            transientMessage = null,
        )
        invalidateDetail()
        invalidateMutation()
        return request
    }

    fun beginLoadMore(sessionKey: String): MessagesListRequest? {
        if (state.activeListRequestId != null || state.isLoadingMore) return null
        val content = state.listContentOrNull() ?: return null
        val cursor = content.nextCursor?.trim()?.takeIf(String::isNotBlank) ?: return null
        if (cursor in inFlightListCursors || cursor in consumedListCursors) return null
        inFlightListCursors += cursor
        val request = newListRequest(sessionKey, state.query, cursor, MessagesListLoadMode.LOAD_MORE)
        state = state.copy(
            listState = DlaFlowUiState.Content(content),
            isLoadingMore = true,
            isRefreshing = false,
            activeListRequestId = request.requestId,
            transientMessage = null,
        )
        return request
    }

    fun acceptListSuccess(request: MessagesListRequest, content: MessagesContent): Boolean {
        if (!matches(request)) return false
        val accepted = if (request.mode == MessagesListLoadMode.LOAD_MORE) {
            mergeListPages(state.listContentOrNull(), content)
        } else {
            content.copy(items = content.items.distinctBy(MessageListItem::id))
        }
        request.cursor?.let {
            inFlightListCursors -= it
            consumedListCursors += it
        }
        finishList(if (accepted.items.isEmpty()) DlaFlowUiState.Empty else DlaFlowUiState.Content(accepted))
        return true
    }

    fun acceptListOffline(request: MessagesListRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        releaseListCursor(request)
        finishList(DlaFlowUiState.Offline(state.listContentOrNull()), message)
        return true
    }

    fun acceptListFailure(request: MessagesListRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        releaseListCursor(request)
        val content = state.listContentOrNull()
        finishList(
            content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message),
            message.takeIf { content != null },
        )
        return true
    }

    fun acceptListNoAccess(request: MessagesListRequest): Boolean {
        if (!matches(request)) return false
        releaseListCursor(request)
        setNoAccess()
        return true
    }

    fun acceptListUnauthorized(request: MessagesListRequest, terminalMessage: DlaFlowUiMessage? = null): Boolean {
        if (!matches(request)) return false
        val content = state.listContentOrNull()
        activeListSessionKey = null
        pendingListUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        if (terminalMessage != null) releaseListCursor(request)
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

    fun beginListUnauthorizedRetry(request: MessagesListRequest): MessagesListRequest? {
        if (pendingListUnauthorizedRequestId != request.requestId) return null
        pendingListUnauthorizedRequestId = null
        val retry = newListRequest(request.sessionKey, request.query, request.cursor, request.mode)
        request.cursor?.let { inFlightListCursors += it }
        state = state.copy(
            activeListRequestId = retry.requestId,
            isRefreshing = request.mode == MessagesListLoadMode.REFRESH && state.listContentOrNull() != null,
            isLoadingMore = request.mode == MessagesListLoadMode.LOAD_MORE,
            transientMessage = null,
        )
        return retry
    }

    fun acceptListSessionUnconfirmed(request: MessagesListRequest, message: DlaFlowUiMessage): Boolean {
        if (pendingListUnauthorizedRequestId != request.requestId) return false
        pendingListUnauthorizedRequestId = null
        releaseListCursor(request)
        val content = state.listContentOrNull()
        state = state.copy(
            listState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message),
            isRefreshing = false,
            isLoadingMore = false,
            activeListRequestId = null,
            transientMessage = message.takeIf { content != null },
        )
        return true
    }

    fun beginDetailLoad(sessionKey: String, threadId: String): MessagesDetailRequest {
        return startDetail(sessionKey, threadId.trim(), cursor = null, mode = MessagesDetailLoadMode.INITIAL)
    }

    fun beginDetailRefresh(sessionKey: String): MessagesDetailRequest? {
        if (state.activeMutationRequestId != null) return null
        val current = state.detailContentOrNull() ?: return null
        return startDetail(sessionKey, current.id, null, MessagesDetailLoadMode.REFRESH)
    }

    fun beginDetailLoadMore(sessionKey: String): MessagesDetailRequest? {
        if (state.activeDetailRequestId != null || state.activeMutationRequestId != null || state.detailState == null) return null
        val current = state.detailContentOrNull() ?: return null
        val cursor = current.nextCursor?.trim()?.takeIf(String::isNotBlank) ?: return null
        if (cursor in inFlightDetailCursors || cursor in consumedDetailCursors) return null
        inFlightDetailCursors += cursor
        return startDetail(sessionKey, current.id, cursor, MessagesDetailLoadMode.LOAD_MORE)
    }

    fun acceptDetailSuccess(request: MessagesDetailRequest, content: MessageThreadDetail): Boolean {
        if (!matches(request) || content.id != request.threadId) return false
        val accepted = if (request.mode == MessagesDetailLoadMode.LOAD_MORE) {
            mergeDetailPages(state.detailContentOrNull(), content)
        } else {
            content.copy(messages = content.messages.distinctBy(MessageBubble::id))
        }
        request.cursor?.let {
            inFlightDetailCursors -= it
            consumedDetailCursors += it
        }
        finishDetail(DlaFlowUiState.Content(accepted))
        return true
    }

    fun acceptDetailOffline(request: MessagesDetailRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        releaseDetailCursor(request)
        finishDetail(DlaFlowUiState.Offline(state.detailContentOrNull()), message)
        return true
    }

    fun acceptDetailFailure(request: MessagesDetailRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        releaseDetailCursor(request)
        val content = state.detailContentOrNull()
        finishDetail(content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message), message.takeIf { content != null })
        return true
    }

    fun acceptDetailNoAccess(request: MessagesDetailRequest): Boolean {
        if (!matches(request)) return false
        releaseDetailCursor(request)
        setNoAccess()
        return true
    }

    fun acceptDetailUnauthorized(request: MessagesDetailRequest, terminalMessage: DlaFlowUiMessage? = null): Boolean {
        if (!matches(request)) return false
        activeDetailSessionKey = null
        pendingDetailUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        if (terminalMessage != null) releaseDetailCursor(request)
        val content = state.detailContentOrNull()
        state = state.copy(
            detailState = when {
                content != null -> DlaFlowUiState.Content(content)
                terminalMessage != null -> DlaFlowUiState.Error(terminalMessage)
                else -> state.detailState
            },
            activeDetailRequestId = null,
            isRefreshingThread = false,
            transientMessage = terminalMessage.takeIf { content != null },
        )
        return true
    }

    fun beginDetailUnauthorizedRetry(request: MessagesDetailRequest): MessagesDetailRequest? {
        if (pendingDetailUnauthorizedRequestId != request.requestId) return null
        pendingDetailUnauthorizedRequestId = null
        val retry = startDetail(request.sessionKey, request.threadId, request.cursor, request.mode)
        request.cursor?.let { inFlightDetailCursors += it }
        return retry
    }

    fun acceptDetailSessionUnconfirmed(request: MessagesDetailRequest, message: DlaFlowUiMessage): Boolean {
        if (pendingDetailUnauthorizedRequestId != request.requestId) return false
        pendingDetailUnauthorizedRequestId = null
        releaseDetailCursor(request)
        val content = state.detailContentOrNull()
        finishDetail(content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message), message.takeIf { content != null })
        return true
    }

    fun beginMarkThreadRead(sessionKey: String): MessagesMutationRequest? {
        val threadId = state.detailContentOrNull()?.id ?: return null
        return beginMutation(sessionKey, threadId, MessagesMutationKind.MARK_READ, null)
    }

    fun beginRefreshThread(sessionKey: String): MessagesMutationRequest? {
        val threadId = state.detailContentOrNull()?.id ?: return null
        return beginMutation(sessionKey, threadId, MessagesMutationKind.REFRESH_THREAD, null)
    }

    fun beginReply(sessionKey: String, body: String, requestId: String): MessagesMutationRequest? {
        val detail = state.detailContentOrNull() ?: return null
        val safeBody = body.trim()
        val safeRequestId = requestId.trim()
        if (safeBody.isBlank() || safeBody.length > 2_000 || safeRequestId.isBlank() || safeRequestId.length > 120) return null
        if (seenReplyRequestIds.contains(safeRequestId) || detail.messages.any { it.requestId == safeRequestId }) return null
        seenReplyRequestIds += safeRequestId
        val request = beginMutation(sessionKey, detail.id, MessagesMutationKind.REPLY, safeRequestId)
            ?: run {
                seenReplyRequestIds -= safeRequestId
                return null
            }
        val bubble = MessageBubble(
            id = "local:$safeRequestId",
            author = "Ty",
            direction = MessageDirection.OUTBOUND,
            body = safeBody,
            messageAt = "",
            status = "queued",
            attachments = emptyList(),
            requestId = safeRequestId,
        )
        state = state.copy(
            detailState = DlaFlowUiState.Content(detail.copy(messages = detail.messages + bubble)),
            isSendingReply = true,
        )
        return request
    }

    fun acceptMutationSuccess(request: MessagesMutationRequest, operation: MessageOperation): Boolean {
        if (!matches(request)) return false
        val detail = state.detailContentOrNull()
        when (request.kind) {
            MessagesMutationKind.MARK_READ -> {
                updateReadState(request.threadId)
            }
            MessagesMutationKind.REFRESH_THREAD -> Unit
            MessagesMutationKind.REPLY -> if (detail != null && request.requestIdempotencyKey != null) {
                val key = request.requestIdempotencyKey
                val current = detail.messages.map { bubble ->
                    if (bubble.requestId == key) bubble.copy(
                        id = operation.messageId ?: bubble.id,
                        status = if (operation.queued) "queued" else operation.status.ifBlank { "sent" },
                    ) else bubble
                }
                state = state.copy(detailState = DlaFlowUiState.Content(detail.copy(messages = current)))
            }
        }
        finishMutation()
        return true
    }

    fun acceptMutationOffline(request: MessagesMutationRequest, message: DlaFlowUiMessage): Boolean =
        acceptMutationFailure(request, message, offline = true)

    fun acceptMutationFailure(request: MessagesMutationRequest, message: DlaFlowUiMessage): Boolean {
        return acceptMutationFailure(request, message, offline = false)
    }

    private fun acceptMutationFailure(request: MessagesMutationRequest, message: DlaFlowUiMessage, offline: Boolean): Boolean {
        if (!matches(request)) return false
        if (request.kind == MessagesMutationKind.REPLY) {
            val detail = state.detailContentOrNull()
            val key = request.requestIdempotencyKey
            if (detail != null && key != null) {
                state = state.copy(
                    detailState = DlaFlowUiState.Content(detail.copy(messages = detail.messages.map {
                        if (it.requestId == key) it.copy(status = "failed") else it
                    })),
                    transientMessage = message,
                )
            }
        } else {
            state = state.copy(
                detailState = DlaFlowUiState.Offline(state.detailContentOrNull()).takeIf { offline }
                    ?: state.detailContentOrNull()?.let { DlaFlowUiState.Content(it) },
                transientMessage = message,
            )
        }
        finishMutation()
        return true
    }

    fun acceptMutationNoAccess(request: MessagesMutationRequest): Boolean {
        if (!matches(request)) return false
        setNoAccess()
        return true
    }

    fun acceptMutationUnauthorized(request: MessagesMutationRequest, terminalMessage: DlaFlowUiMessage? = null): Boolean {
        if (!matches(request)) return false
        activeMutationSessionKey = null
        pendingMutationUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        state = state.copy(
            isMarkingRead = request.kind == MessagesMutationKind.MARK_READ && terminalMessage == null,
            isRefreshingThread = request.kind == MessagesMutationKind.REFRESH_THREAD && terminalMessage == null,
            isSendingReply = request.kind == MessagesMutationKind.REPLY && terminalMessage == null,
            activeMutationRequestId = null,
            transientMessage = terminalMessage,
        )
        return true
    }

    fun beginMutationUnauthorizedRetry(request: MessagesMutationRequest): MessagesMutationRequest? {
        if (pendingMutationUnauthorizedRequestId != request.requestId) return null
        pendingMutationUnauthorizedRequestId = null
        return beginMutation(request.sessionKey, request.threadId, request.kind, request.requestIdempotencyKey)
    }

    fun acceptMutationSessionUnconfirmed(request: MessagesMutationRequest, message: DlaFlowUiMessage): Boolean {
        if (pendingMutationUnauthorizedRequestId != request.requestId) return false
        pendingMutationUnauthorizedRequestId = null
        state = state.copy(
            isMarkingRead = false,
            isRefreshingThread = false,
            isSendingReply = false,
            activeMutationRequestId = null,
            transientMessage = message,
        )
        return true
    }

    fun closeDetail() {
        invalidateDetail()
        invalidateMutation()
        state = state.copy(route = MessagesRoute.List, detailState = null, activeDetailRequestId = null, transientMessage = null)
    }

    fun reset() {
        activeListSessionKey = null
        activeDetailSessionKey = null
        activeMutationSessionKey = null
        pendingListUnauthorizedRequestId = null
        pendingDetailUnauthorizedRequestId = null
        pendingMutationUnauthorizedRequestId = null
        inFlightListCursors.clear()
        consumedListCursors.clear()
        inFlightDetailCursors.clear()
        consumedDetailCursors.clear()
        seenReplyRequestIds.clear()
        state = MessagesUiState()
    }

    private fun beginMutation(
        sessionKey: String,
        threadId: String,
        kind: MessagesMutationKind,
        idempotencyKey: String?,
    ): MessagesMutationRequest? {
        if (state.activeMutationRequestId != null || state.activeDetailRequestId != null || threadId.isBlank()) return null
        val request = MessagesMutationRequest(
            requestId = ++nextMutationRequestId,
            sessionKey = sessionKey,
            threadId = threadId,
            requestIdempotencyKey = idempotencyKey,
            kind = kind,
        )
        activeMutationSessionKey = sessionKey
        pendingMutationUnauthorizedRequestId = null
        state = state.copy(
            isMarkingRead = kind == MessagesMutationKind.MARK_READ,
            isRefreshingThread = kind == MessagesMutationKind.REFRESH_THREAD,
            isSendingReply = kind == MessagesMutationKind.REPLY,
            activeMutationRequestId = request.requestId,
            transientMessage = null,
        )
        return request
    }

    private fun startDetail(sessionKey: String, threadId: String, cursor: String?, mode: MessagesDetailLoadMode): MessagesDetailRequest {
        require(threadId.isNotBlank()) { "Thread id must be non-blank." }
        invalidateMutation()
        val request = MessagesDetailRequest(++nextDetailRequestId, sessionKey, threadId, cursor, mode)
        activeDetailSessionKey = sessionKey
        pendingDetailUnauthorizedRequestId = null
        val content = state.detailContentOrNull()
        state = state.copy(
            route = MessagesRoute.Detail(threadId),
            detailState = when (mode) {
                MessagesDetailLoadMode.LOAD_MORE,
                MessagesDetailLoadMode.REFRESH,
                -> content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Loading
                MessagesDetailLoadMode.INITIAL -> DlaFlowUiState.Loading
            },
            isRefreshingThread = mode == MessagesDetailLoadMode.REFRESH && content != null,
            isLoadingMore = mode == MessagesDetailLoadMode.LOAD_MORE,
            activeDetailRequestId = request.requestId,
            transientMessage = null,
        )
        return request
    }

    private fun newListRequest(sessionKey: String, query: MessagesQuery, cursor: String?, mode: MessagesListLoadMode): MessagesListRequest {
        val request = MessagesListRequest(++nextListRequestId, sessionKey, query, cursor, mode)
        activeListSessionKey = sessionKey
        pendingListUnauthorizedRequestId = null
        return request
    }

    private fun matches(request: MessagesListRequest): Boolean =
        state.activeListRequestId == request.requestId && activeListSessionKey == request.sessionKey

    private fun matches(request: MessagesDetailRequest): Boolean =
        state.activeDetailRequestId == request.requestId && activeDetailSessionKey == request.sessionKey

    private fun matches(request: MessagesMutationRequest): Boolean =
        state.activeMutationRequestId == request.requestId && activeMutationSessionKey == request.sessionKey

    private fun finishList(listState: DlaFlowUiState<MessagesContent>, transientMessage: DlaFlowUiMessage? = null) {
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

    private fun finishDetail(detailState: DlaFlowUiState<MessageThreadDetail>, transientMessage: DlaFlowUiMessage? = null) {
        activeDetailSessionKey = null
        pendingDetailUnauthorizedRequestId = null
        state = state.copy(
            detailState = detailState,
            isRefreshingThread = false,
            isLoadingMore = false,
            activeDetailRequestId = null,
            transientMessage = transientMessage,
        )
    }

    private fun finishMutation() {
        activeMutationSessionKey = null
        pendingMutationUnauthorizedRequestId = null
        state = state.copy(
            isMarkingRead = false,
            isRefreshingThread = false,
            isSendingReply = false,
            activeMutationRequestId = null,
        )
    }

    private fun invalidateDetail() {
        activeDetailSessionKey = null
        pendingDetailUnauthorizedRequestId = null
        state = state.copy(detailState = null, activeDetailRequestId = null)
    }

    private fun invalidateMutation() {
        activeMutationSessionKey = null
        pendingMutationUnauthorizedRequestId = null
        state = state.copy(
            isMarkingRead = false,
            isRefreshingThread = false,
            isSendingReply = false,
            activeMutationRequestId = null,
        )
    }

    private fun releaseListCursor(request: MessagesListRequest) { request.cursor?.let(inFlightListCursors::remove) }
    private fun releaseDetailCursor(request: MessagesDetailRequest) { request.cursor?.let(inFlightDetailCursors::remove) }

    private fun mergeListPages(current: MessagesContent?, incoming: MessagesContent): MessagesContent {
        val merged = current?.items.orEmpty().toMutableList()
        incoming.items.forEach { item ->
            val index = merged.indexOfFirst { it.id == item.id }
            if (index >= 0) merged[index] = item else merged += item
        }
        return incoming.copy(items = merged)
    }

    private fun mergeDetailPages(current: MessageThreadDetail?, incoming: MessageThreadDetail): MessageThreadDetail {
        if (current == null) return incoming
        val merged = current.messages.toMutableList()
        incoming.messages.forEach { bubble ->
            val index = merged.indexOfFirst { it.id == bubble.id }
            if (index >= 0) merged[index] = bubble else merged += bubble
        }
        return incoming.copy(messages = merged)
    }

    private fun updateReadState(threadId: String) {
        val detail = state.detailContentOrNull()
        val updatedDetail = detail?.takeIf { it.id == threadId }?.copy(status = "read")
        val listContent = state.listContentOrNull()
        val updatedList = listContent?.copy(items = listContent.items.map { item ->
            if (item.id == threadId) item.copy(status = "read") else item
        })
        state = state.copy(
            detailState = updatedDetail?.let { DlaFlowUiState.Content(it) } ?: state.detailState,
            listState = updatedList?.let { DlaFlowUiState.Content(it) } ?: state.listState,
        )
    }

    private fun setNoAccess() {
        activeListSessionKey = null
        activeDetailSessionKey = null
        activeMutationSessionKey = null
        pendingListUnauthorizedRequestId = null
        pendingDetailUnauthorizedRequestId = null
        pendingMutationUnauthorizedRequestId = null
        state = state.copy(
            listState = DlaFlowUiState.NoAccess,
            route = MessagesRoute.List,
            detailState = null,
            isRefreshing = false,
            isLoadingMore = false,
            isMarkingRead = false,
            isRefreshingThread = false,
            isSendingReply = false,
            activeListRequestId = null,
            activeDetailRequestId = null,
            activeMutationRequestId = null,
            transientMessage = null,
        )
    }
}
