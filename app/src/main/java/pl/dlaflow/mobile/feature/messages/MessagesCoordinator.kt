package pl.dlaflow.mobile.feature.messages

import java.util.concurrent.Executor

internal class MessagesCoordinator(
    private val stateHolder: MessagesStateHolder,
    private val gateway: MessagesGateway,
    private val executor: Executor,
    private val postToMain: (() -> Unit) -> Unit,
    private val onUnauthorized: (Throwable, MessagesOperation, Boolean, () -> Unit, () -> Unit) -> Unit,
    private val onStateChanged: () -> Unit = {},
) {
    fun open(token: String, query: MessagesQuery = stateHolder.state.query, allowUnauthorizedRetry: Boolean = true): Boolean {
        val request = stateHolder.beginListReset(token, query)
        executeList(token, request, MessagesOperation.ListReset(query), allowUnauthorizedRetry)
        return true
    }

    fun refresh(token: String, allowUnauthorizedRetry: Boolean = true): Boolean {
        val request = stateHolder.beginListRefresh(token)
        executeList(token, request, MessagesOperation.ListRefresh, allowUnauthorizedRetry)
        return true
    }

    fun loadMore(token: String, allowUnauthorizedRetry: Boolean = true): Boolean {
        val request = stateHolder.beginLoadMore(token) ?: return false
        executeList(token, request, MessagesOperation.LoadMore, allowUnauthorizedRetry)
        return true
    }

    fun openThread(token: String, threadId: String, allowUnauthorizedRetry: Boolean = true): Boolean {
        val request = stateHolder.beginDetailLoad(token, threadId)
        executeDetail(token, request, MessagesOperation.Detail(threadId, MessagesDetailLoadMode.INITIAL), allowUnauthorizedRetry)
        return true
    }

    fun loadDetail(token: String, threadId: String, allowUnauthorizedRetry: Boolean = true): Boolean =
        openThread(token, threadId, allowUnauthorizedRetry)

    fun loadMoreDetail(token: String, allowUnauthorizedRetry: Boolean = true): Boolean {
        val request = stateHolder.beginDetailLoadMore(token) ?: return false
        executeDetail(token, request, MessagesOperation.Detail(request.threadId, MessagesDetailLoadMode.LOAD_MORE), allowUnauthorizedRetry)
        return true
    }

    fun markThreadRead(token: String, allowUnauthorizedRetry: Boolean = true): Boolean {
        val request = stateHolder.beginMarkThreadRead(token) ?: return false
        executeMutation(token, request, MessagesOperation.Mutation(request.kind, request.threadId), allowUnauthorizedRetry)
        return true
    }

    fun refreshThread(token: String, allowUnauthorizedRetry: Boolean = true): Boolean {
        val request = stateHolder.beginRefreshThread(token) ?: return false
        executeMutation(token, request, MessagesOperation.Mutation(request.kind, request.threadId), allowUnauthorizedRetry)
        return true
    }

    fun reply(token: String, threadId: String, body: String, requestId: String, allowUnauthorizedRetry: Boolean = true): Boolean {
        val current = stateHolder.state.detailContentOrNull()
        if (current == null || current.id != threadId) return false
        val request = stateHolder.beginReply(token, body, requestId) ?: return false
        executeMutation(
            token,
            request,
            MessagesOperation.Mutation(request.kind, request.threadId, body = body.trim(), requestId = requestId.trim()),
            allowUnauthorizedRetry,
        )
        return true
    }

    fun closeDetail() {
        stateHolder.closeDetail()
        onStateChanged()
    }

    fun retry(token: String, operation: MessagesOperation): Boolean = when (operation) {
        is MessagesOperation.ListReset -> open(token, operation.query, allowUnauthorizedRetry = false)
        MessagesOperation.ListRefresh -> refresh(token, allowUnauthorizedRetry = false)
        MessagesOperation.LoadMore -> loadMore(token, allowUnauthorizedRetry = false)
        is MessagesOperation.Detail -> openThread(token, operation.threadId, allowUnauthorizedRetry = false)
        is MessagesOperation.Mutation -> when (operation.kind) {
            MessagesMutationKind.MARK_READ -> markThreadRead(token, allowUnauthorizedRetry = false)
            MessagesMutationKind.REFRESH_THREAD -> refreshThread(token, allowUnauthorizedRetry = false)
            MessagesMutationKind.REPLY -> {
                val body = operation.body ?: return false
                val requestId = operation.requestId ?: return false
                reply(token, operation.threadId, body, requestId, allowUnauthorizedRetry = false)
            }
        }
    }

    fun reset() {
        stateHolder.reset()
        onStateChanged()
    }

    private fun executeList(token: String, request: MessagesListRequest, operation: MessagesOperation, allowUnauthorizedRetry: Boolean) {
        executor.execute {
            runCatching { gateway.loadPage(token, request.query, request.cursor) }
                .onSuccess { content -> postToMain { if (stateHolder.acceptListSuccess(request, content)) onStateChanged() } }
                .onFailure { error -> postToMain { handleListFailure(token, request, operation, error, allowUnauthorizedRetry) } }
        }
    }

    private fun executeDetail(token: String, request: MessagesDetailRequest, operation: MessagesOperation, allowUnauthorizedRetry: Boolean) {
        executor.execute {
            runCatching { gateway.loadDetail(token, request.threadId, request.cursor) }
                .onSuccess { content -> postToMain { if (stateHolder.acceptDetailSuccess(request, content)) onStateChanged() } }
                .onFailure { error -> postToMain { handleDetailFailure(token, request, operation, error, allowUnauthorizedRetry) } }
        }
    }

    private fun executeMutation(token: String, request: MessagesMutationRequest, operation: MessagesOperation, allowUnauthorizedRetry: Boolean) {
        executor.execute {
            runCatching {
                when (request.kind) {
                    MessagesMutationKind.MARK_READ -> gateway.markRead(token, request.threadId)
                    MessagesMutationKind.REFRESH_THREAD -> gateway.refreshThread(token, request.threadId)
                    MessagesMutationKind.REPLY -> gateway.reply(
                        token,
                        request.threadId,
                        (operation as? MessagesOperation.Mutation)?.body.orEmpty(),
                        request.requestIdempotencyKey.orEmpty(),
                    )
                }
            }.onSuccess { result -> postToMain { if (stateHolder.acceptMutationSuccess(request, result)) onStateChanged() } }
                .onFailure { error -> postToMain { handleMutationFailure(token, request, operation, error, allowUnauthorizedRetry) } }
        }
    }

    private fun handleListFailure(token: String, request: MessagesListRequest, operation: MessagesOperation, error: Throwable, allowUnauthorizedRetry: Boolean) {
        when (val failure = mapMessagesFailure(error)) {
            is MessagesFailure.Unauthorized -> {
                val terminal = failure.message.takeIf { !allowUnauthorizedRetry }
                if (stateHolder.acceptListUnauthorized(request, terminal)) {
                    onUnauthorized(error, operation, allowUnauthorizedRetry,
                        { stateHolder.beginListUnauthorizedRetry(request)?.let { executeList(token, it, operation, false) } },
                        { stateHolder.acceptListSessionUnconfirmed(request, messagesSessionUnconfirmedMessage()); onStateChanged() },
                    )
                }
            }
            MessagesFailure.NoAccess -> if (stateHolder.acceptListNoAccess(request)) onStateChanged()
            is MessagesFailure.Offline -> if (stateHolder.acceptListOffline(request, failure.message)) onStateChanged()
            is MessagesFailure.Retryable -> if (stateHolder.acceptListFailure(request, failure.message)) onStateChanged()
        }
    }

    private fun handleDetailFailure(token: String, request: MessagesDetailRequest, operation: MessagesOperation, error: Throwable, allowUnauthorizedRetry: Boolean) {
        when (val failure = mapMessagesFailure(error)) {
            is MessagesFailure.Unauthorized -> {
                val terminal = failure.message.takeIf { !allowUnauthorizedRetry }
                if (stateHolder.acceptDetailUnauthorized(request, terminal)) {
                    onUnauthorized(error, operation, allowUnauthorizedRetry,
                        { stateHolder.beginDetailUnauthorizedRetry(request)?.let { executeDetail(token, it, operation, false) } },
                        { stateHolder.acceptDetailSessionUnconfirmed(request, messagesSessionUnconfirmedMessage()); onStateChanged() },
                    )
                }
            }
            MessagesFailure.NoAccess -> if (stateHolder.acceptDetailNoAccess(request)) onStateChanged()
            is MessagesFailure.Offline -> if (stateHolder.acceptDetailOffline(request, failure.message)) onStateChanged()
            is MessagesFailure.Retryable -> if (stateHolder.acceptDetailFailure(request, failure.message)) onStateChanged()
        }
    }

    private fun handleMutationFailure(token: String, request: MessagesMutationRequest, operation: MessagesOperation, error: Throwable, allowUnauthorizedRetry: Boolean) {
        when (val failure = mapMessagesFailure(error)) {
            is MessagesFailure.Unauthorized -> {
                val terminal = failure.message.takeIf { !allowUnauthorizedRetry }
                if (stateHolder.acceptMutationUnauthorized(request, terminal)) {
                    onUnauthorized(error, operation, allowUnauthorizedRetry,
                        { stateHolder.beginMutationUnauthorizedRetry(request)?.let { executeMutation(token, it, operation, false) } },
                        { stateHolder.acceptMutationSessionUnconfirmed(request, messagesSessionUnconfirmedMessage()); onStateChanged() },
                    )
                }
            }
            MessagesFailure.NoAccess -> if (stateHolder.acceptMutationNoAccess(request)) onStateChanged()
            is MessagesFailure.Offline -> if (stateHolder.acceptMutationOffline(request, failure.message)) onStateChanged()
            is MessagesFailure.Retryable -> if (stateHolder.acceptMutationFailure(request, failure.message)) onStateChanged()
        }
    }
}
