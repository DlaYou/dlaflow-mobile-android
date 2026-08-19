package pl.dlaflow.mobile.feature.products

import java.util.concurrent.Executor
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal fun interface ProductsScheduledTask {
    fun cancel()
}

internal fun interface ProductsSearchScheduler {
    fun schedule(delayMillis: Long, action: () -> Unit): ProductsScheduledTask
}

internal enum class ProductsFeedback {
    LIST_LOADING,
    LIST_READY,
    LIST_EMPTY,
    VARIANTS_LOADING,
    VARIANTS_READY,
    QUICK_EDIT_SAVING,
    QUICK_EDIT_SAVED,
    LOAD_FAILED,
}

internal class ProductsCoordinator(
    private val stateHolder: ProductsStateHolder,
    private val gateway: ProductsGateway,
    private val executor: Executor,
    private val postToMain: (() -> Unit) -> Unit,
    private val searchScheduler: ProductsSearchScheduler,
    private val onFeedback: (ProductsFeedback) -> Unit,
    private val onUnauthorized: (Throwable, ProductsOperation, Boolean, () -> Unit, () -> Unit) -> Unit,
) {
    private var searchGeneration = 0L
    private var pendingSearch: ProductsScheduledTask? = null

    fun handleAction(
        token: String,
        action: ProductsAction,
        showFeedback: Boolean = true,
    ): Boolean = when (action) {
        is ProductsAction.SearchChanged -> scheduleSearchReset(token, action.search, showFeedback)
        is ProductsAction.FilterChanged -> {
            cancelPendingSearch()
            resetList(token, stateHolder.state.query.copy(filter = action.filter), showFeedback)
            true
        }
        ProductsAction.Refresh,
        ProductsAction.Retry,
        -> {
            cancelPendingSearch()
            refreshList(token, showFeedback)
            true
        }
        ProductsAction.LoadMore -> loadMore(token, showFeedback)
        is ProductsAction.ToggleVariants -> toggleVariants(token, action.productId, showFeedback)
        is ProductsAction.OpenQuickEdit -> stateHolder.openQuickEdit(action.target)
        ProductsAction.CancelQuickEdit -> stateHolder.cancelQuickEdit()
        is ProductsAction.SaveQuickEdit -> {
            val target = stateHolder.state.quickEdit?.target
            if (target == null) false else beginQuickEdit(token, target, action.value, showFeedback)
        }
    }

    fun resetList(token: String, query: ProductsQuery, showFeedback: Boolean) {
        cancelPendingSearch()
        val request = stateHolder.beginListReset(token, query)
        executeList(token, request, showFeedback, allowUnauthorizedRetry = true)
    }

    fun refreshList(token: String, showFeedback: Boolean) {
        cancelPendingSearch()
        val request = stateHolder.beginListRefresh(token)
        executeList(token, request, showFeedback, allowUnauthorizedRetry = true)
    }

    fun loadMore(token: String, showFeedback: Boolean): Boolean {
        val request = stateHolder.beginLoadMore(token) ?: return false
        executeList(token, request, showFeedback, allowUnauthorizedRetry = true)
        return true
    }

    fun toggleVariants(token: String, productId: String, showFeedback: Boolean): Boolean {
        if (productId in stateHolder.state.expandedProductIds) {
            stateHolder.collapseVariants(productId)
            return false
        }
        val request = stateHolder.beginVariantsLoad(token, productId) ?: return false
        executeVariants(token, request, showFeedback, allowUnauthorizedRetry = true)
        return true
    }

    fun quickEditProduct(
        token: String,
        productId: String,
        field: ProductQuickEditField,
        value: Double,
        showFeedback: Boolean,
    ): Boolean = beginQuickEdit(
        token,
        ProductQuickEditTarget.Product(productId, field),
        value,
        showFeedback,
    )

    fun quickEditVariant(
        token: String,
        productId: String,
        variantId: String,
        field: VariantQuickEditField,
        value: Double,
        showFeedback: Boolean,
    ): Boolean = beginQuickEdit(
        token,
        ProductQuickEditTarget.Variant(productId, variantId, field),
        value,
        showFeedback,
    )

    fun reset() {
        cancelPendingSearch()
        stateHolder.reset()
    }

    private fun scheduleSearchReset(token: String, search: String, showFeedback: Boolean): Boolean {
        if (!stateHolder.updateSearchInput(search)) return false
        pendingSearch?.cancel()
        val generation = ++searchGeneration
        pendingSearch = searchScheduler.schedule(SEARCH_DEBOUNCE_MS) {
            postToMain {
                if (generation != searchGeneration) return@postToMain
                pendingSearch = null
                val request = stateHolder.beginListReset(token, stateHolder.state.query)
                executeList(token, request, showFeedback, allowUnauthorizedRetry = true)
            }
        }
        return true
    }

    private fun cancelPendingSearch() {
        searchGeneration += 1
        pendingSearch?.cancel()
        pendingSearch = null
    }

    private fun beginQuickEdit(
        token: String,
        target: ProductQuickEditTarget,
        value: Double,
        showFeedback: Boolean,
    ): Boolean {
        val request = stateHolder.beginQuickEdit(token, target, value) ?: return false
        executeQuickEdit(token, request, showFeedback, allowUnauthorizedRetry = true)
        return true
    }

    private fun executeList(
        token: String,
        request: ProductsListRequest,
        showFeedback: Boolean,
        allowUnauthorizedRetry: Boolean,
    ) {
        if (showFeedback) onFeedback(ProductsFeedback.LIST_LOADING)
        executor.execute {
            runCatching { gateway.loadPage(token, request.query, request.cursor) }
                .onSuccess { content ->
                    postToMain {
                        if (stateHolder.acceptListSuccess(request, content) && showFeedback) {
                            onFeedback(if (content.items.isEmpty()) ProductsFeedback.LIST_EMPTY else ProductsFeedback.LIST_READY)
                        }
                    }
                }
                .onFailure { error ->
                    postToMain { handleListFailure(token, request, error, showFeedback, allowUnauthorizedRetry) }
                }
        }
    }

    private fun executeVariants(
        token: String,
        request: ProductsVariantsRequest,
        showFeedback: Boolean,
        allowUnauthorizedRetry: Boolean,
    ) {
        if (showFeedback) onFeedback(ProductsFeedback.VARIANTS_LOADING)
        executor.execute {
            runCatching { gateway.loadVariants(token, request.productId, request.parentThumbnailUrl) }
                .onSuccess { variants ->
                    postToMain {
                        if (stateHolder.acceptVariantsSuccess(request, variants) && showFeedback) {
                            onFeedback(ProductsFeedback.VARIANTS_READY)
                        }
                    }
                }
                .onFailure { error ->
                    postToMain { handleVariantsFailure(token, request, error, showFeedback, allowUnauthorizedRetry) }
                }
        }
    }

    private fun executeQuickEdit(
        token: String,
        request: ProductsQuickEditRequest,
        showFeedback: Boolean,
        allowUnauthorizedRetry: Boolean,
    ) {
        if (showFeedback) onFeedback(ProductsFeedback.QUICK_EDIT_SAVING)
        executor.execute {
            runCatching {
                when (val target = request.target) {
                    is ProductQuickEditTarget.Product -> gateway.quickEditProduct(
                        token,
                        target.productId,
                        target.field,
                        request.value,
                    )
                    is ProductQuickEditTarget.Variant -> gateway.quickEditVariant(
                        token,
                        target.productId,
                        target.variantId,
                        target.field,
                        request.value,
                        request.parentThumbnailUrl,
                    )
                }
            }.onSuccess { updated ->
                postToMain {
                    val accepted = when (updated) {
                        is ProductItem -> stateHolder.acceptQuickEditProductSuccess(request, updated)
                        is ProductVariant -> stateHolder.acceptQuickEditVariantSuccess(request, updated)
                        else -> false
                    }
                    if (accepted && showFeedback) onFeedback(ProductsFeedback.QUICK_EDIT_SAVED)
                }
            }.onFailure { error ->
                postToMain { handleQuickEditFailure(token, request, error, showFeedback, allowUnauthorizedRetry) }
            }
        }
    }

    private fun handleListFailure(
        token: String,
        request: ProductsListRequest,
        error: Throwable,
        showFeedback: Boolean,
        allowRetry: Boolean,
    ) {
        val failure = mapProductsFailure(error)
        val accepted = when (failure) {
            is ProductsFailure.Unauthorized -> handleListUnauthorized(token, request, error, failure, showFeedback, allowRetry)
            is ProductsFailure.NoAccess -> stateHolder.acceptListNoAccess(request)
            is ProductsFailure.Offline -> stateHolder.acceptListOffline(request, failure.message)
            is ProductsFailure.InvalidResult -> stateHolder.acceptListFailure(request, failure.message)
            is ProductsFailure.Retryable -> stateHolder.acceptListFailure(request, failure.message)
        }
        if (accepted && showFeedback && failure !is ProductsFailure.Unauthorized) onFeedback(ProductsFeedback.LOAD_FAILED)
    }

    private fun handleListUnauthorized(
        token: String,
        request: ProductsListRequest,
        error: Throwable,
        failure: ProductsFailure.Unauthorized,
        showFeedback: Boolean,
        allowRetry: Boolean,
    ): Boolean {
        if (!allowRetry) {
            val accepted = stateHolder.acceptListUnauthorized(request, failure.message)
            if (accepted && showFeedback) onFeedback(ProductsFeedback.LOAD_FAILED)
            return accepted
        }
        if (!stateHolder.acceptListUnauthorized(request)) return false
        val operation = ProductsOperation.List(request.mode, request.query, request.cursor)
        onUnauthorized(
            error,
            operation,
            true,
            {
                val retry = stateHolder.beginListUnauthorizedRetry(request) ?: return@onUnauthorized
                executeList(token, retry, showFeedback, allowUnauthorizedRetry = false)
            },
            {
                if (stateHolder.acceptListSessionUnconfirmed(request, productsSessionUnconfirmedMessage()) && showFeedback) {
                    onFeedback(ProductsFeedback.LOAD_FAILED)
                }
            },
        )
        return true
    }

    private fun handleVariantsFailure(
        token: String,
        request: ProductsVariantsRequest,
        error: Throwable,
        showFeedback: Boolean,
        allowRetry: Boolean,
    ) {
        val failure = mapProductsFailure(error)
        val accepted = when (failure) {
            is ProductsFailure.Unauthorized -> handleVariantsUnauthorized(token, request, error, failure, showFeedback, allowRetry)
            is ProductsFailure.NoAccess -> stateHolder.acceptVariantsFailure(request, DlaFlowUiState.NoAccess)
            is ProductsFailure.Offline -> stateHolder.acceptVariantsFailure(request, DlaFlowUiState.Offline())
            is ProductsFailure.InvalidResult -> stateHolder.acceptVariantsFailure(request, DlaFlowUiState.Error(failure.message))
            is ProductsFailure.Retryable -> stateHolder.acceptVariantsFailure(request, DlaFlowUiState.Error(failure.message))
        }
        if (accepted && showFeedback && failure !is ProductsFailure.Unauthorized) onFeedback(ProductsFeedback.LOAD_FAILED)
    }

    private fun handleVariantsUnauthorized(
        token: String,
        request: ProductsVariantsRequest,
        error: Throwable,
        failure: ProductsFailure.Unauthorized,
        showFeedback: Boolean,
        allowRetry: Boolean,
    ): Boolean {
        if (!allowRetry) {
            val accepted = stateHolder.acceptVariantsUnauthorized(request, failure.message)
            if (accepted && showFeedback) onFeedback(ProductsFeedback.LOAD_FAILED)
            return accepted
        }
        if (!stateHolder.acceptVariantsUnauthorized(request)) return false
        onUnauthorized(
            error,
            ProductsOperation.Variants(request.productId),
            true,
            {
                val retry = stateHolder.beginVariantsUnauthorizedRetry(request) ?: return@onUnauthorized
                executeVariants(token, retry, showFeedback, allowUnauthorizedRetry = false)
            },
            {
                if (stateHolder.acceptVariantsSessionUnconfirmed(request, productsSessionUnconfirmedMessage()) && showFeedback) {
                    onFeedback(ProductsFeedback.LOAD_FAILED)
                }
            },
        )
        return true
    }

    private fun handleQuickEditFailure(
        token: String,
        request: ProductsQuickEditRequest,
        error: Throwable,
        showFeedback: Boolean,
        allowRetry: Boolean,
    ) {
        val failure = mapProductsFailure(error)
        val accepted = when (failure) {
            is ProductsFailure.Unauthorized -> handleQuickEditUnauthorized(token, request, error, failure, showFeedback, allowRetry)
            is ProductsFailure.NoAccess -> stateHolder.acceptQuickEditFailure(request, failure.message, noAccess = true)
            is ProductsFailure.Offline -> stateHolder.acceptQuickEditFailure(request, failure.message)
            is ProductsFailure.InvalidResult -> stateHolder.acceptQuickEditFailure(request, failure.message)
            is ProductsFailure.Retryable -> stateHolder.acceptQuickEditFailure(request, failure.message)
        }
        if (accepted && showFeedback && failure !is ProductsFailure.Unauthorized) onFeedback(ProductsFeedback.LOAD_FAILED)
    }

    private fun handleQuickEditUnauthorized(
        token: String,
        request: ProductsQuickEditRequest,
        error: Throwable,
        failure: ProductsFailure.Unauthorized,
        showFeedback: Boolean,
        allowRetry: Boolean,
    ): Boolean {
        if (!allowRetry) {
            val accepted = stateHolder.acceptQuickEditUnauthorized(request, failure.message)
            if (accepted && showFeedback) onFeedback(ProductsFeedback.LOAD_FAILED)
            return accepted
        }
        if (!stateHolder.acceptQuickEditUnauthorized(request)) return false
        onUnauthorized(
            error,
            ProductsOperation.QuickEdit(request.target, request.value),
            true,
            {
                val retry = stateHolder.beginQuickEditUnauthorizedRetry(request) ?: return@onUnauthorized
                executeQuickEdit(token, retry, showFeedback, allowUnauthorizedRetry = false)
            },
            {
                if (stateHolder.acceptQuickEditSessionUnconfirmed(request, productsSessionUnconfirmedMessage()) && showFeedback) {
                    onFeedback(ProductsFeedback.LOAD_FAILED)
                }
            },
        )
        return true
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
