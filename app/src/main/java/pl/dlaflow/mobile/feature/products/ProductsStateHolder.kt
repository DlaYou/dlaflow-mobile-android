package pl.dlaflow.mobile.feature.products

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal class ProductsStateHolder {
    var state by mutableStateOf(ProductsUiState())
        private set

    private var nextListRequestId = 0L
    private var nextVariantsRequestId = 0L
    private var nextQuickEditRequestId = 0L
    private var activeListSessionKey: String? = null
    private val activeVariantSessions = mutableMapOf<String, Pair<Long, String>>()
    private var activeQuickEditSession: Pair<Long, String>? = null
    private var pendingListUnauthorizedRequestId: Long? = null
    private val pendingVariantUnauthorizedRequestIds = mutableMapOf<String, Long>()
    private var pendingQuickEditUnauthorizedRequestId: Long? = null
    private val inFlightCursors = mutableSetOf<String>()
    private val consumedCursors = mutableSetOf<String>()

    fun updateSearchInput(search: String): Boolean {
        val normalized = search.trim()
        if (normalized == state.query.search) return false
        activeListSessionKey = null
        pendingListUnauthorizedRequestId = null
        invalidateProductChildren()
        inFlightCursors.clear()
        consumedCursors.clear()
        state = state.copy(
            query = state.query.copy(search = normalized),
            variants = emptyMap(),
            expandedProductIds = emptySet(),
            quickEdit = null,
            isRefreshing = false,
            isLoadingMore = false,
            activeListRequestId = null,
            transientMessage = null,
        )
        return true
    }

    fun beginListReset(sessionKey: String, query: ProductsQuery): ProductsListRequest {
        invalidateProductChildren()
        inFlightCursors.clear()
        consumedCursors.clear()
        val normalizedQuery = query.copy(search = query.search.trim())
        val request = newListRequest(sessionKey, normalizedQuery, null, ProductsListLoadMode.RESET)
        state = ProductsUiState(
            query = normalizedQuery,
            listState = DlaFlowUiState.Loading,
            activeListRequestId = request.requestId,
        )
        return request
    }

    fun beginListRefresh(sessionKey: String): ProductsListRequest {
        val content = state.listContentOrNull()
        val request = newListRequest(sessionKey, state.query, null, ProductsListLoadMode.REFRESH)
        state = state.copy(
            listState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Loading,
            isRefreshing = content != null,
            isLoadingMore = false,
            activeListRequestId = request.requestId,
            transientMessage = null,
        )
        return request
    }

    fun beginLoadMore(sessionKey: String): ProductsListRequest? {
        if (state.activeListRequestId != null || state.isLoadingMore) return null
        val content = state.listContentOrNull() ?: return null
        val cursor = content.nextCursor?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (cursor in inFlightCursors || cursor in consumedCursors) return null
        inFlightCursors += cursor
        val request = newListRequest(sessionKey, state.query, cursor, ProductsListLoadMode.LOAD_MORE)
        state = state.copy(
            listState = DlaFlowUiState.Content(content),
            isRefreshing = false,
            isLoadingMore = true,
            activeListRequestId = request.requestId,
            transientMessage = null,
        )
        return request
    }

    fun acceptListSuccess(request: ProductsListRequest, content: ProductsContent): Boolean {
        if (!matches(request)) return false
        val accepted = if (request.mode == ProductsListLoadMode.LOAD_MORE) mergePages(state.listContentOrNull(), content) else content
        request.cursor?.let {
            inFlightCursors -= it
            consumedCursors += it
        }
        val validProductIds = accepted.items.mapTo(mutableSetOf()) { it.id }
        pruneProductChildren(validProductIds)
        finishList(if (accepted.items.isEmpty()) DlaFlowUiState.Empty else DlaFlowUiState.Content(accepted))
        return true
    }

    fun acceptListOffline(request: ProductsListRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        releaseCursor(request)
        finishList(DlaFlowUiState.Offline(state.listContentOrNull()), message)
        return true
    }

    fun acceptListFailure(request: ProductsListRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        releaseCursor(request)
        val content = state.listContentOrNull()
        finishList(
            content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message),
            message.takeIf { content != null },
        )
        return true
    }

    fun acceptListNoAccess(request: ProductsListRequest): Boolean {
        if (!matches(request)) return false
        releaseCursor(request)
        invalidateProductChildren()
        activeListSessionKey = null
        pendingListUnauthorizedRequestId = null
        state = ProductsUiState(query = state.query, listState = DlaFlowUiState.NoAccess)
        return true
    }

    fun acceptListUnauthorized(request: ProductsListRequest, terminalMessage: DlaFlowUiMessage? = null): Boolean {
        if (!matches(request)) return false
        activeListSessionKey = null
        pendingListUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        val content = state.listContentOrNull()
        if (terminalMessage != null) releaseCursor(request)
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

    fun beginListUnauthorizedRetry(request: ProductsListRequest): ProductsListRequest? {
        if (pendingListUnauthorizedRequestId != request.requestId) return null
        pendingListUnauthorizedRequestId = null
        val retry = newListRequest(request.sessionKey, request.query, request.cursor, request.mode)
        state = state.copy(
            activeListRequestId = retry.requestId,
            isRefreshing = request.mode == ProductsListLoadMode.REFRESH && state.listContentOrNull() != null,
            isLoadingMore = request.mode == ProductsListLoadMode.LOAD_MORE,
            transientMessage = null,
        )
        return retry
    }

    fun acceptListSessionUnconfirmed(request: ProductsListRequest, message: DlaFlowUiMessage): Boolean {
        if (pendingListUnauthorizedRequestId != request.requestId) return false
        pendingListUnauthorizedRequestId = null
        releaseCursor(request)
        val content = state.listContentOrNull()
        state = state.copy(
            listState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message),
            transientMessage = message.takeIf { content != null },
        )
        return true
    }

    fun beginVariantsLoad(sessionKey: String, productId: String): ProductsVariantsRequest? {
        val safeId = productId.trim()
        val product = state.listContentOrNull()?.items?.firstOrNull { it.id == safeId } ?: return null
        if (safeId.isBlank() || product.variantCount <= 0 || activeVariantSessions.containsKey(safeId)) return null
        val request = ProductsVariantsRequest(++nextVariantsRequestId, sessionKey, safeId, product.thumbnailUrl)
        activeVariantSessions[safeId] = request.requestId to sessionKey
        pendingVariantUnauthorizedRequestIds -= safeId
        state = state.copy(
            variants = state.variants + (safeId to DlaFlowUiState.Loading),
            expandedProductIds = state.expandedProductIds + safeId,
            transientMessage = null,
        )
        return request
    }

    fun collapseVariants(productId: String): Boolean {
        if (productId !in state.expandedProductIds) return false
        activeVariantSessions -= productId
        pendingVariantUnauthorizedRequestIds -= productId
        state = state.copy(
            variants = state.variants - productId,
            expandedProductIds = state.expandedProductIds - productId,
        )
        return true
    }

    fun acceptVariantsSuccess(request: ProductsVariantsRequest, variants: List<ProductVariant>): Boolean {
        if (!matches(request) || variants.any { it.productId != request.productId }) return false
        finishVariants(request.productId, if (variants.isEmpty()) DlaFlowUiState.Empty else DlaFlowUiState.Content(variants.toList()))
        return true
    }

    fun acceptVariantsFailure(request: ProductsVariantsRequest, stateValue: DlaFlowUiState<List<ProductVariant>>): Boolean {
        if (!matches(request)) return false
        finishVariants(request.productId, stateValue)
        return true
    }

    fun acceptVariantsUnauthorized(request: ProductsVariantsRequest, terminalMessage: DlaFlowUiMessage? = null): Boolean {
        if (!matches(request)) return false
        activeVariantSessions -= request.productId
        if (terminalMessage == null) pendingVariantUnauthorizedRequestIds[request.productId] = request.requestId
        else pendingVariantUnauthorizedRequestIds -= request.productId
        state = state.copy(
            variants = if (terminalMessage == null) state.variants else state.variants +
                (request.productId to DlaFlowUiState.Error(terminalMessage)),
        )
        return true
    }

    fun beginVariantsUnauthorizedRetry(request: ProductsVariantsRequest): ProductsVariantsRequest? {
        if (pendingVariantUnauthorizedRequestIds[request.productId] != request.requestId || request.productId !in state.expandedProductIds) return null
        pendingVariantUnauthorizedRequestIds -= request.productId
        val retry = request.copy(requestId = ++nextVariantsRequestId)
        activeVariantSessions[request.productId] = retry.requestId to retry.sessionKey
        state = state.copy(variants = state.variants + (request.productId to DlaFlowUiState.Loading))
        return retry
    }

    fun acceptVariantsSessionUnconfirmed(request: ProductsVariantsRequest, message: DlaFlowUiMessage): Boolean {
        if (pendingVariantUnauthorizedRequestIds[request.productId] != request.requestId) return false
        pendingVariantUnauthorizedRequestIds -= request.productId
        state = state.copy(variants = state.variants + (request.productId to DlaFlowUiState.Error(message)))
        return true
    }

    fun beginQuickEdit(
        sessionKey: String,
        target: ProductQuickEditTarget,
        value: Double,
    ): ProductsQuickEditRequest? {
        val currentEdit = state.quickEdit
        if (
            currentEdit?.isSaving == true ||
            (currentEdit != null && currentEdit.target != target) ||
            !targetIsEditable(target)
        ) return null
        val validationError = validateProductsQuickEditValue(target, value)
        if (validationError != null) {
            state = state.copy(
                quickEdit = currentEdit?.copy(
                    isSaving = false,
                    message = productsQuickEditValidationMessage(validationError),
                ),
            )
            return null
        }
        val parentThumbnailUrl = when (target) {
            is ProductQuickEditTarget.Product -> ""
            is ProductQuickEditTarget.Variant -> state.listContentOrNull()?.items
                ?.firstOrNull { it.id == target.productId }?.thumbnailUrl.orEmpty()
        }
        val request = ProductsQuickEditRequest(++nextQuickEditRequestId, sessionKey, target, value, parentThumbnailUrl)
        activeQuickEditSession = request.requestId to sessionKey
        pendingQuickEditUnauthorizedRequestId = null
        state = state.copy(quickEdit = ProductQuickEditState(target, value, isSaving = true), transientMessage = null)
        return request
    }

    fun openQuickEdit(target: ProductQuickEditTarget): Boolean {
        if (state.quickEdit?.isSaving == true || !targetIsEditable(target)) return false
        val value = initialValue(target) ?: return false
        state = state.copy(quickEdit = ProductQuickEditState(target, value, isSaving = false), transientMessage = null)
        return true
    }

    fun cancelQuickEdit(): Boolean {
        if (state.quickEdit == null || state.quickEdit?.isSaving == true) return false
        state = state.copy(quickEdit = null)
        return true
    }

    fun acceptQuickEditProductSuccess(request: ProductsQuickEditRequest, updated: ProductItem): Boolean {
        val target = request.target as? ProductQuickEditTarget.Product ?: return false
        if (!matches(request) || updated.id != target.productId) return false
        val content = state.listContentOrNull() ?: return false
        finishQuickEdit(content.copy(items = content.items.map { if (it.id == updated.id) updated else it }))
        return true
    }

    fun acceptQuickEditVariantSuccess(request: ProductsQuickEditRequest, updated: ProductVariant): Boolean {
        val target = request.target as? ProductQuickEditTarget.Variant ?: return false
        if (!matches(request) || updated.id != target.variantId || updated.productId != target.productId) return false
        val current = (state.variants[target.productId] as? DlaFlowUiState.Content)?.data ?: return false
        activeQuickEditSession = null
        pendingQuickEditUnauthorizedRequestId = null
        state = state.copy(
            variants = state.variants + (target.productId to DlaFlowUiState.Content(current.map { if (it.id == updated.id) updated else it })),
            quickEdit = null,
            transientMessage = null,
        )
        return true
    }

    fun acceptQuickEditFailure(request: ProductsQuickEditRequest, message: DlaFlowUiMessage, noAccess: Boolean = false): Boolean {
        if (!matches(request)) return false
        activeQuickEditSession = null
        pendingQuickEditUnauthorizedRequestId = null
        val content = state.listContentOrNull()
        state = state.copy(
            listState = if (noAccess && content != null) DlaFlowUiState.Content(content.copy(canEdit = false)) else state.listState,
            quickEdit = state.quickEdit?.copy(isSaving = false, message = message),
        )
        return true
    }

    fun acceptQuickEditUnauthorized(request: ProductsQuickEditRequest, terminalMessage: DlaFlowUiMessage? = null): Boolean {
        if (!matches(request)) return false
        activeQuickEditSession = null
        pendingQuickEditUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        if (terminalMessage != null) state = state.copy(quickEdit = state.quickEdit?.copy(isSaving = false, message = terminalMessage))
        return true
    }

    fun beginQuickEditUnauthorizedRetry(request: ProductsQuickEditRequest): ProductsQuickEditRequest? {
        if (pendingQuickEditUnauthorizedRequestId != request.requestId) return null
        pendingQuickEditUnauthorizedRequestId = null
        val retry = request.copy(requestId = ++nextQuickEditRequestId)
        activeQuickEditSession = retry.requestId to retry.sessionKey
        state = state.copy(quickEdit = state.quickEdit?.copy(isSaving = true, message = null))
        return retry
    }

    fun acceptQuickEditSessionUnconfirmed(request: ProductsQuickEditRequest, message: DlaFlowUiMessage): Boolean {
        if (pendingQuickEditUnauthorizedRequestId != request.requestId) return false
        pendingQuickEditUnauthorizedRequestId = null
        state = state.copy(quickEdit = state.quickEdit?.copy(isSaving = false, message = message))
        return true
    }

    fun reset() {
        activeListSessionKey = null
        pendingListUnauthorizedRequestId = null
        invalidateProductChildren()
        inFlightCursors.clear()
        consumedCursors.clear()
        state = ProductsUiState()
    }

    private fun newListRequest(sessionKey: String, query: ProductsQuery, cursor: String?, mode: ProductsListLoadMode) =
        ProductsListRequest(++nextListRequestId, sessionKey, query, cursor, mode).also {
            activeListSessionKey = sessionKey
            pendingListUnauthorizedRequestId = null
        }

    private fun matches(request: ProductsListRequest) =
        state.activeListRequestId == request.requestId && activeListSessionKey == request.sessionKey

    private fun matches(request: ProductsVariantsRequest) =
        activeVariantSessions[request.productId] == (request.requestId to request.sessionKey) &&
            request.productId in state.expandedProductIds

    private fun matches(request: ProductsQuickEditRequest) =
        activeQuickEditSession == (request.requestId to request.sessionKey) && state.quickEdit?.target == request.target

    private fun finishList(value: DlaFlowUiState<ProductsContent>, message: DlaFlowUiMessage? = null) {
        activeListSessionKey = null
        pendingListUnauthorizedRequestId = null
        state = state.copy(
            listState = value,
            isRefreshing = false,
            isLoadingMore = false,
            activeListRequestId = null,
            transientMessage = message,
        )
    }

    private fun finishVariants(productId: String, value: DlaFlowUiState<List<ProductVariant>>) {
        activeVariantSessions -= productId
        pendingVariantUnauthorizedRequestIds -= productId
        state = state.copy(variants = state.variants + (productId to value))
    }

    private fun finishQuickEdit(content: ProductsContent) {
        activeQuickEditSession = null
        pendingQuickEditUnauthorizedRequestId = null
        state = state.copy(listState = DlaFlowUiState.Content(content), quickEdit = null, transientMessage = null)
    }

    private fun releaseCursor(request: ProductsListRequest) {
        request.cursor?.let { inFlightCursors -= it }
    }

    private fun mergePages(current: ProductsContent?, incoming: ProductsContent): ProductsContent {
        val merged = current?.items.orEmpty().associateByTo(linkedMapOf()) { it.id }
        incoming.items.forEach { merged[it.id] = it }
        return incoming.copy(items = merged.values.toList(), canEdit = incoming.canEdit)
    }

    private fun invalidateProductChildren() {
        activeVariantSessions.clear()
        pendingVariantUnauthorizedRequestIds.clear()
        activeQuickEditSession = null
        pendingQuickEditUnauthorizedRequestId = null
    }

    private fun pruneProductChildren(validIds: Set<String>) {
        activeVariantSessions.keys.retainAll(validIds)
        pendingVariantUnauthorizedRequestIds.keys.retainAll(validIds)
        state = state.copy(
            variants = state.variants.filterKeys { it in validIds },
            expandedProductIds = state.expandedProductIds.filterTo(mutableSetOf()) { it in validIds },
        )
    }

    private fun targetIsEditable(target: ProductQuickEditTarget): Boolean {
        val content = state.listContentOrNull() ?: return false
        if (!content.canEdit) return false
        return when (target) {
            is ProductQuickEditTarget.Product -> {
                val product = content.items.firstOrNull { it.id == target.productId } ?: return false
                product.canQuickEdit(target.field)
            }
            is ProductQuickEditTarget.Variant -> {
                val variant = (state.variants[target.productId] as? DlaFlowUiState.Content)?.data
                    ?.firstOrNull { it.id == target.variantId && it.productId == target.productId } ?: return false
                variant.canQuickEdit(target.field)
            }
        }
    }

    private fun initialValue(target: ProductQuickEditTarget): Double? = when (target) {
        is ProductQuickEditTarget.Product -> {
            val product = state.listContentOrNull()?.items?.firstOrNull { it.id == target.productId } ?: return null
            when (target.field) {
                ProductQuickEditField.GROSS_PRICE -> product.grossPrice
                ProductQuickEditField.STOCK -> product.stock.toDouble()
            }
        }
        is ProductQuickEditTarget.Variant -> {
            val variant = (state.variants[target.productId] as? DlaFlowUiState.Content)?.data
                ?.firstOrNull { it.id == target.variantId } ?: return null
            when (target.field) {
                VariantQuickEditField.PRICE -> variant.price
                VariantQuickEditField.STOCK -> variant.stock.toDouble()
            }
        }
    }

}
