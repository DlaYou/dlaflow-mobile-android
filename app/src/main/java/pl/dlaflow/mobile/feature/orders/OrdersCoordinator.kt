package pl.dlaflow.mobile.feature.orders

import java.util.concurrent.Executor

internal enum class OrdersFeedback {
    LIST_LOADING,
    LIST_READY,
    LIST_EMPTY,
    DETAIL_LOADING,
    DETAIL_READY,
    DETAIL_CLOSED,
    LOAD_FAILED,
}

internal sealed interface OrdersLoadOperation {
    data class ListReset(val query: OrdersQuery) : OrdersLoadOperation
    data object ListRefresh : OrdersLoadOperation
    data object LoadMore : OrdersLoadOperation
    data class Detail(val orderNumber: String) : OrdersLoadOperation
}

internal class OrdersCoordinator(
    private val stateHolder: OrdersStateHolder,
    private val gateway: OrdersGateway,
    private val executor: Executor,
    private val postToMain: (() -> Unit) -> Unit,
    private val onFeedback: (OrdersFeedback) -> Unit,
    private val onUnauthorized: (Throwable, OrdersLoadOperation, Boolean, () -> Unit) -> Unit,
) {
    fun resetList(
        token: String,
        query: OrdersQuery,
        showFeedback: Boolean,
        allowUnauthorizedRetry: Boolean = true,
    ) {
        executeList(
            token = token,
            request = stateHolder.beginListReset(token, query),
            operation = OrdersLoadOperation.ListReset(query),
            showFeedback = showFeedback,
            allowUnauthorizedRetry = allowUnauthorizedRetry,
        )
    }

    fun refreshList(
        token: String,
        showFeedback: Boolean,
        allowUnauthorizedRetry: Boolean = true,
    ) {
        executeList(
            token = token,
            request = stateHolder.beginListRefresh(token),
            operation = OrdersLoadOperation.ListRefresh,
            showFeedback = showFeedback,
            allowUnauthorizedRetry = allowUnauthorizedRetry,
        )
    }

    fun loadMore(
        token: String,
        showFeedback: Boolean,
        allowUnauthorizedRetry: Boolean = true,
    ): Boolean {
        val request = stateHolder.beginLoadMore(token) ?: return false
        executeList(
            token = token,
            request = request,
            operation = OrdersLoadOperation.LoadMore,
            showFeedback = showFeedback,
            allowUnauthorizedRetry = allowUnauthorizedRetry,
        )
        return true
    }

    fun loadDetail(
        token: String,
        orderNumber: String,
        showFeedback: Boolean,
        allowUnauthorizedRetry: Boolean = true,
    ) {
        val request = stateHolder.beginDetailLoad(token, orderNumber)
        val operation = OrdersLoadOperation.Detail(orderNumber)
        if (showFeedback) onFeedback(OrdersFeedback.DETAIL_LOADING)
        executor.execute {
            runCatching {
                gateway.loadDetail(token, orderNumber)
            }.onSuccess { content ->
                postToMain {
                    if (stateHolder.acceptDetailSuccess(request, content) && showFeedback) {
                        onFeedback(OrdersFeedback.DETAIL_READY)
                    }
                }
            }.onFailure { error ->
                postToMain {
                    handleDetailFailure(
                        request = request,
                        error = error,
                        operation = operation,
                        showFeedback = showFeedback,
                        allowUnauthorizedRetry = allowUnauthorizedRetry,
                    )
                }
            }
        }
    }

    fun retry(
        token: String,
        operation: OrdersLoadOperation,
        showFeedback: Boolean = true,
    ) {
        when (operation) {
            is OrdersLoadOperation.ListReset -> resetList(
                token = token,
                query = operation.query,
                showFeedback = showFeedback,
                allowUnauthorizedRetry = false,
            )

            OrdersLoadOperation.ListRefresh -> refreshList(
                token = token,
                showFeedback = showFeedback,
                allowUnauthorizedRetry = false,
            )

            OrdersLoadOperation.LoadMore -> loadMore(
                token = token,
                showFeedback = showFeedback,
                allowUnauthorizedRetry = false,
            )

            is OrdersLoadOperation.Detail -> loadDetail(
                token = token,
                orderNumber = operation.orderNumber,
                showFeedback = showFeedback,
                allowUnauthorizedRetry = false,
            )
        }
    }

    fun closeDetail() {
        stateHolder.closeDetail()
        onFeedback(OrdersFeedback.DETAIL_CLOSED)
    }

    fun reset() {
        stateHolder.reset()
    }

    private fun executeList(
        token: String,
        request: OrdersListRequest,
        operation: OrdersLoadOperation,
        showFeedback: Boolean,
        allowUnauthorizedRetry: Boolean,
    ) {
        if (showFeedback) onFeedback(OrdersFeedback.LIST_LOADING)
        executor.execute {
            runCatching {
                gateway.loadPage(token, request.query, request.offset)
            }.onSuccess { content ->
                postToMain {
                    if (stateHolder.acceptListSuccess(request, content) && showFeedback) {
                        onFeedback(
                            if (stateHolder.state.listContentOrNull() == null) {
                                OrdersFeedback.LIST_EMPTY
                            } else {
                                OrdersFeedback.LIST_READY
                            },
                        )
                    }
                }
            }.onFailure { error ->
                postToMain {
                    handleListFailure(
                        request = request,
                        error = error,
                        operation = operation,
                        showFeedback = showFeedback,
                        allowUnauthorizedRetry = allowUnauthorizedRetry,
                    )
                }
            }
        }
    }

    private fun handleListFailure(
        request: OrdersListRequest,
        error: Throwable,
        operation: OrdersLoadOperation,
        showFeedback: Boolean,
        allowUnauthorizedRetry: Boolean,
    ) {
        val failure = mapOrdersFailure(error)
        val accepted = when (failure) {
            is OrdersFailure.Unauthorized -> {
                val terminalMessage = failure.message.takeIf { !allowUnauthorizedRetry }
                val current = stateHolder.acceptListUnauthorized(request, terminalMessage)
                if (current) {
                    onUnauthorized(error, operation, allowUnauthorizedRetry) {
                        stateHolder.acceptListSessionUnconfirmed(
                            request = request,
                            message = ordersSessionUnconfirmedMessage(),
                        )
                    }
                }
                current
            }

            OrdersFailure.NoAccess -> stateHolder.acceptListNoAccess(request)
            is OrdersFailure.Offline -> stateHolder.acceptListOffline(request, failure.message)
            is OrdersFailure.NotFound -> stateHolder.acceptListFailure(request, failure.message)
            is OrdersFailure.Retryable -> stateHolder.acceptListFailure(request, failure.message)
        }
        if (accepted && showFeedback && failure !is OrdersFailure.Unauthorized) {
            onFeedback(OrdersFeedback.LOAD_FAILED)
        }
    }

    private fun handleDetailFailure(
        request: OrdersDetailRequest,
        error: Throwable,
        operation: OrdersLoadOperation,
        showFeedback: Boolean,
        allowUnauthorizedRetry: Boolean,
    ) {
        val failure = mapOrdersFailure(error)
        val accepted = when (failure) {
            is OrdersFailure.Unauthorized -> {
                val terminalMessage = failure.message.takeIf { !allowUnauthorizedRetry }
                val current = stateHolder.acceptDetailUnauthorized(request, terminalMessage)
                if (current) {
                    onUnauthorized(error, operation, allowUnauthorizedRetry) {
                        stateHolder.acceptDetailSessionUnconfirmed(
                            request = request,
                            message = ordersSessionUnconfirmedMessage(),
                        )
                    }
                }
                current
            }

            OrdersFailure.NoAccess -> stateHolder.acceptDetailNoAccess(request)
            is OrdersFailure.Offline -> stateHolder.acceptDetailOffline(request, failure.message)
            is OrdersFailure.NotFound -> stateHolder.acceptDetailFailure(request, failure.message)
            is OrdersFailure.Retryable -> stateHolder.acceptDetailFailure(request, failure.message)
        }
        if (accepted && showFeedback && failure !is OrdersFailure.Unauthorized) {
            onFeedback(OrdersFeedback.LOAD_FAILED)
        }
    }
}
