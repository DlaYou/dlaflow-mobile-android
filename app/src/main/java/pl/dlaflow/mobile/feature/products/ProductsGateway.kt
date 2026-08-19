package pl.dlaflow.mobile.feature.products

import pl.dlaflow.mobile.MobileApiClient
import pl.dlaflow.mobile.MobileProductQuickEditField
import pl.dlaflow.mobile.MobileVariantQuickEditField

internal interface ProductsGateway {
    fun loadPage(token: String, query: ProductsQuery, cursor: String?): ProductsContent
    fun loadVariants(token: String, productId: String, parentThumbnailUrl: String): List<ProductVariant>
    fun quickEditProduct(
        token: String,
        productId: String,
        field: ProductQuickEditField,
        value: Double,
    ): ProductItem
    fun quickEditVariant(
        token: String,
        productId: String,
        variantId: String,
        field: VariantQuickEditField,
        value: Double,
        parentThumbnailUrl: String,
    ): ProductVariant
}

internal class MobileApiProductsGateway(
    private val clientProvider: () -> MobileApiClient,
) : ProductsGateway {
    override fun loadPage(token: String, query: ProductsQuery, cursor: String?): ProductsContent =
        clientProvider().listProducts(token, query.search, query.filter.toMobileProductFilter(), cursor).toProductsContent()

    override fun loadVariants(token: String, productId: String, parentThumbnailUrl: String): List<ProductVariant> =
        clientProvider().listProductVariants(token, productId).toProductVariants(
            expectedProductId = productId,
            parentThumbnailUrl = parentThumbnailUrl,
        )

    override fun quickEditProduct(
        token: String,
        productId: String,
        field: ProductQuickEditField,
        value: Double,
    ): ProductItem = clientProvider().quickEditProduct(
        token,
        productId,
        when (field) {
            ProductQuickEditField.GROSS_PRICE -> MobileProductQuickEditField.GROSS_PRICE
            ProductQuickEditField.STOCK -> MobileProductQuickEditField.STOCK
        },
        value,
    ).toProductItem(expectedId = productId)

    override fun quickEditVariant(
        token: String,
        productId: String,
        variantId: String,
        field: VariantQuickEditField,
        value: Double,
        parentThumbnailUrl: String,
    ): ProductVariant = clientProvider().quickEditProductVariant(
        token,
        productId,
        variantId,
        when (field) {
            VariantQuickEditField.PRICE -> MobileVariantQuickEditField.PRICE
            VariantQuickEditField.STOCK -> MobileVariantQuickEditField.STOCK
        },
        value,
    ).toProductVariant(
        expectedProductId = productId,
        expectedVariantId = variantId,
        parentThumbnailUrl = parentThumbnailUrl,
    )
}
