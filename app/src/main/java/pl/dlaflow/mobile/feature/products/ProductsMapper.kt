package pl.dlaflow.mobile.feature.products

import java.util.Locale
import pl.dlaflow.mobile.MobileProduct
import pl.dlaflow.mobile.MobileProductFilter
import pl.dlaflow.mobile.MobileProductVariant
import pl.dlaflow.mobile.MobileProductsPage

internal class InvalidProductsResultException : IllegalStateException()

private const val MOBILE_PRODUCT_MEDIA_PREFIX = "/api/mobile/products/media/"
private const val LEGACY_PRODUCT_MEDIA_PREFIX = "/api/products/media/"
private val SAFE_PRODUCT_MEDIA_FILENAME = Regex(
    "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}\\.(?:avif|gif|jpe?g|png|webp)$",
    RegexOption.IGNORE_CASE,
)

internal fun MobileProductsPage.toProductsContent(): ProductsContent {
    val items = data.map { it.toProductItem() }
    if (items.map { it.id }.toSet().size != items.size) throw InvalidProductsResultException()
    return ProductsContent(
        items = items,
        total = total.takeIf { it >= items.size } ?: throw InvalidProductsResultException(),
        nextCursor = nextCursor?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
        canEdit = canEdit,
    )
}

internal fun MobileProduct.toProductItem(expectedId: String? = null): ProductItem {
    val safeId = id.trim()
    if (safeId.isBlank() || (expectedId != null && safeId != expectedId)) throw InvalidProductsResultException()
    if (!grossPrice.isFinite() || grossPrice < 0.0 || stock < 0 || variantCount < 0) throw InvalidProductsResultException()
    return ProductItem(
        id = safeId,
        name = name.trim().ifBlank { "Produkt" },
        sku = sku.trim(),
        ean = ean.trim(),
        thumbnailUrl = firstMobileProductThumbnail(thumbnailUrl, image),
        grossPrice = grossPrice,
        stock = stock,
        status = normalizedProductStatus(status, lowStock),
        currency = normalizedCurrency(currency),
        variantCount = variantCount,
        lowStock = lowStock,
        editableFields = ProductEditableFields(
            grossPrice = editableFields.grossPrice,
            stock = editableFields.stock,
        ),
    )
}

internal fun MobileProductVariant.toProductVariant(
    expectedProductId: String,
    expectedVariantId: String? = null,
    parentThumbnailUrl: String = "",
): ProductVariant {
    val safeId = id.trim()
    val safeProductId = productId.trim()
    if (
        safeId.isBlank() || safeProductId.isBlank() || safeProductId != expectedProductId ||
        (expectedVariantId != null && safeId != expectedVariantId) ||
        !price.isFinite() || price < 0.0 || stock < 0
    ) throw InvalidProductsResultException()
    return ProductVariant(
        id = safeId,
        productId = safeProductId,
        name = name.trim().ifBlank { "Wariant" },
        sku = sku.trim(),
        ean = ean.trim(),
        thumbnailUrl = firstMobileProductThumbnail(thumbnailUrl, image, parentThumbnailUrl),
        price = price,
        stock = stock,
        status = normalizedProductStatus(status, lowStock = false),
        editableFields = VariantEditableFields(
            price = editableFields.price,
            stock = editableFields.stock,
        ),
    )
}

private fun firstMobileProductThumbnail(vararg candidates: String): String = candidates
    .asSequence()
    .map(::normalizeMobileProductThumbnailUrl)
    .firstOrNull { it.isNotBlank() }
    .orEmpty()

private fun normalizeMobileProductThumbnailUrl(raw: String): String {
    val candidate = raw.trim()
    if (candidate.isBlank()) return ""
    if (candidate.startsWith(MOBILE_PRODUCT_MEDIA_PREFIX)) return candidate
    if (!candidate.startsWith(LEGACY_PRODUCT_MEDIA_PREFIX)) return ""

    val filename = candidate
        .removePrefix(LEGACY_PRODUCT_MEDIA_PREFIX)
        .substringBefore('?')
    if (!SAFE_PRODUCT_MEDIA_FILENAME.matches(filename)) return ""

    return "$MOBILE_PRODUCT_MEDIA_PREFIX$filename?variant=thumb"
}

internal fun List<MobileProductVariant>.toProductVariants(
    expectedProductId: String,
    parentThumbnailUrl: String,
): List<ProductVariant> {
    val items = map {
        it.toProductVariant(
            expectedProductId = expectedProductId,
            parentThumbnailUrl = parentThumbnailUrl,
        )
    }
    if (items.map { it.id }.toSet().size != items.size) throw InvalidProductsResultException()
    return items
}

internal fun ProductsFilter.toMobileProductFilter(): MobileProductFilter = when (this) {
    ProductsFilter.ALL -> MobileProductFilter.ALL
    ProductsFilter.LOW_STOCK -> MobileProductFilter.LOW_STOCK
    ProductsFilter.NO_IMAGE -> MobileProductFilter.NO_IMAGE
    ProductsFilter.HAS_VARIANTS -> MobileProductFilter.HAS_VARIANTS
}

private fun normalizedProductStatus(raw: String, lowStock: Boolean): ProductStatus = when (raw.trim().lowercase(Locale.ROOT)) {
    "out_of_stock", "out-of-stock", "brak stanu" -> ProductStatus("Brak stanu", ProductStatusTone.DANGER)
    "low_stock", "low-stock", "niski stan" -> ProductStatus("Niski stan", ProductStatusTone.WARNING)
    "active", "published", "aktywny" -> if (lowStock) {
        ProductStatus("Niski stan", ProductStatusTone.WARNING)
    } else {
        ProductStatus("Aktywny", ProductStatusTone.SUCCESS)
    }
    "draft", "roboczy" -> ProductStatus("Roboczy", ProductStatusTone.NEUTRAL)
    "inactive", "disabled", "archived", "nieaktywny" -> ProductStatus("Nieaktywny", ProductStatusTone.NEUTRAL)
    "" -> if (lowStock) ProductStatus("Niski stan", ProductStatusTone.WARNING) else ProductStatus("Status nieznany", ProductStatusTone.NEUTRAL)
    else -> ProductStatus("Status nieznany", ProductStatusTone.NEUTRAL)
}

private fun normalizedCurrency(raw: String): String {
    val value = raw.trim().uppercase(Locale.ROOT)
    return value.takeIf { it.length == 3 && it.all(Char::isLetter) } ?: "PLN"
}
