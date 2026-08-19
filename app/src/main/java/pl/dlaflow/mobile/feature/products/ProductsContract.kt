package pl.dlaflow.mobile.feature.products

import pl.dlaflow.mobile.MOBILE_PRODUCT_QUICK_EDIT_MAX_PRICE
import pl.dlaflow.mobile.MOBILE_PRODUCT_QUICK_EDIT_MAX_STOCK
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal enum class ProductsFilter { ALL, LOW_STOCK, NO_IMAGE, HAS_VARIANTS }

internal data class ProductsQuery(
    val search: String = "",
    val filter: ProductsFilter = ProductsFilter.ALL,
)

internal enum class ProductStatusTone { SUCCESS, WARNING, DANGER, NEUTRAL }

internal data class ProductStatus(val label: String, val tone: ProductStatusTone)
internal data class ProductEditableFields(val grossPrice: Boolean, val stock: Boolean)
internal data class VariantEditableFields(val price: Boolean, val stock: Boolean)

internal data class ProductItem(
    val id: String,
    val name: String,
    val sku: String,
    val ean: String,
    val thumbnailUrl: String,
    val grossPrice: Double,
    val stock: Int,
    val status: ProductStatus,
    val currency: String,
    val variantCount: Int,
    val lowStock: Boolean,
    val editableFields: ProductEditableFields,
)

internal fun ProductItem.canQuickEdit(field: ProductQuickEditField): Boolean =
    variantCount == 0 && when (field) {
        ProductQuickEditField.GROSS_PRICE -> editableFields.grossPrice
        ProductQuickEditField.STOCK -> editableFields.stock
    }

internal data class ProductVariant(
    val id: String,
    val productId: String,
    val name: String,
    val sku: String,
    val ean: String,
    val thumbnailUrl: String,
    val price: Double,
    val stock: Int,
    val status: ProductStatus,
    val editableFields: VariantEditableFields,
)

internal fun ProductVariant.canQuickEdit(field: VariantQuickEditField): Boolean = when (field) {
    VariantQuickEditField.PRICE -> editableFields.price
    VariantQuickEditField.STOCK -> editableFields.stock
}

internal data class ProductsContent(
    val items: List<ProductItem>,
    val total: Int,
    val nextCursor: String?,
    val canEdit: Boolean,
)

internal enum class ProductQuickEditField { GROSS_PRICE, STOCK }
internal enum class VariantQuickEditField { PRICE, STOCK }

internal enum class ProductsQuickEditValidationError {
    INVALID_NUMBER,
    NEGATIVE,
    STOCK_NOT_INTEGER,
    TOO_HIGH,
}

internal sealed interface ProductQuickEditTarget {
    data class Product(val productId: String, val field: ProductQuickEditField) : ProductQuickEditTarget
    data class Variant(
        val productId: String,
        val variantId: String,
        val field: VariantQuickEditField,
    ) : ProductQuickEditTarget
}

internal fun validateProductsQuickEditValue(
    target: ProductQuickEditTarget,
    value: Double,
): ProductsQuickEditValidationError? {
    if (!value.isFinite()) return ProductsQuickEditValidationError.INVALID_NUMBER
    if (value < 0.0) return ProductsQuickEditValidationError.NEGATIVE
    val stock = when (target) {
        is ProductQuickEditTarget.Product -> target.field == ProductQuickEditField.STOCK
        is ProductQuickEditTarget.Variant -> target.field == VariantQuickEditField.STOCK
    }
    if (stock && value % 1.0 != 0.0) return ProductsQuickEditValidationError.STOCK_NOT_INTEGER
    val maximum = if (stock) {
        MOBILE_PRODUCT_QUICK_EDIT_MAX_STOCK.toDouble()
    } else {
        MOBILE_PRODUCT_QUICK_EDIT_MAX_PRICE
    }
    return ProductsQuickEditValidationError.TOO_HIGH.takeIf { value > maximum }
}

internal data class ProductQuickEditState(
    val target: ProductQuickEditTarget,
    val value: Double,
    val isSaving: Boolean,
    val message: DlaFlowUiMessage? = null,
)

internal data class ProductsUiState(
    val query: ProductsQuery = ProductsQuery(),
    val listState: DlaFlowUiState<ProductsContent> = DlaFlowUiState.Loading,
    val variants: Map<String, DlaFlowUiState<List<ProductVariant>>> = emptyMap(),
    val expandedProductIds: Set<String> = emptySet(),
    val quickEdit: ProductQuickEditState? = null,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val activeListRequestId: Long? = null,
    val transientMessage: DlaFlowUiMessage? = null,
)

internal fun ProductsUiState.listContentOrNull(): ProductsContent? = when (val current = listState) {
    is DlaFlowUiState.Content -> current.data
    is DlaFlowUiState.Offline -> current.lastContent
    else -> null
}

internal enum class ProductsListLoadMode { RESET, REFRESH, LOAD_MORE }

internal data class ProductsListRequest(
    val requestId: Long,
    internal val sessionKey: String,
    val query: ProductsQuery,
    val cursor: String?,
    val mode: ProductsListLoadMode,
)

internal data class ProductsVariantsRequest(
    val requestId: Long,
    internal val sessionKey: String,
    val productId: String,
    val parentThumbnailUrl: String,
)

internal data class ProductsQuickEditRequest(
    val requestId: Long,
    internal val sessionKey: String,
    val target: ProductQuickEditTarget,
    val value: Double,
    val parentThumbnailUrl: String = "",
)

internal sealed interface ProductsOperation {
    data class List(val mode: ProductsListLoadMode, val query: ProductsQuery, val cursor: String?) : ProductsOperation
    data class Variants(val productId: String) : ProductsOperation
    data class QuickEdit(val target: ProductQuickEditTarget, val value: Double) : ProductsOperation
}

internal sealed interface ProductsAction {
    data class SearchChanged(val search: String) : ProductsAction
    data class FilterChanged(val filter: ProductsFilter) : ProductsAction
    data object Refresh : ProductsAction
    data object LoadMore : ProductsAction
    data class ToggleVariants(val productId: String) : ProductsAction
    data class OpenQuickEdit(val target: ProductQuickEditTarget) : ProductsAction
    data object CancelQuickEdit : ProductsAction
    data class SaveQuickEdit(val value: Double) : ProductsAction
    data object Retry : ProductsAction
}
