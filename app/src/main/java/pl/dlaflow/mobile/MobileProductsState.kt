package pl.dlaflow.mobile

import java.net.URLEncoder
import java.util.Locale

const val MOBILE_PRODUCT_QUICK_EDIT_MAX_PRICE = 10_000_000.0
const val MOBILE_PRODUCT_QUICK_EDIT_MAX_STOCK = 10_000_000

fun buildMobileProductsQuery(search: String, filter: MobileProductFilter, cursor: String? = null): String {
    val params = mutableListOf(
        "limit" to "20",
        "countMode" to "window",
        "sort" to "id",
        "direction" to "asc",
    )
    val trimmedSearch = search.trim()
    if (trimmedSearch.isNotBlank()) {
        params.add("search" to trimmedSearch)
    }
    when (filter) {
        MobileProductFilter.ALL -> Unit
        MobileProductFilter.LOW_STOCK -> params.add("stockState" to filter.queryValue)
        MobileProductFilter.NO_IMAGE -> params.add("noImage" to "true")
        MobileProductFilter.HAS_VARIANTS -> params.add("hasVariants" to "true")
    }
    val trimmedCursor = cursor?.trim().orEmpty()
    if (trimmedCursor.isNotBlank()) {
        params.add("cursor" to trimmedCursor)
    }

    return params.joinToString("&") { (key, value) -> "$key=${encodeQueryValue(value)}" }
}

data class MobileProductEditDecision(
    val allowed: Boolean,
    val reason: String,
)

fun canQuickEditProduct(product: MobileProduct, field: MobileProductQuickEditField): MobileProductEditDecision {
    if (product.variantCount > 0) {
        return MobileProductEditDecision(
            allowed = false,
            reason = "Wariantowy produkt edytuj na poziomie wariantu.",
        )
    }

    val fieldEditable = when (field) {
        MobileProductQuickEditField.GROSS_PRICE -> product.editableFields.grossPrice
        MobileProductQuickEditField.STOCK -> product.editableFields.stock
    }
    if (!fieldEditable) {
        return MobileProductEditDecision(
            allowed = false,
            reason = "Brak uprawnienia do edycji tego pola.",
        )
    }

    return MobileProductEditDecision(allowed = true, reason = "")
}

fun replaceMobileProduct(products: List<MobileProduct>, updated: MobileProduct): List<MobileProduct> {
    return products.map { product ->
        if (product.id == updated.id) updated else product
    }
}

fun replaceMobileVariant(
    variants: Map<String, List<MobileProductVariant>>,
    updated: MobileProductVariant,
): Map<String, List<MobileProductVariant>> {
    return variants.mapValues { (productId, productVariants) ->
        if (productId != updated.productId) {
            productVariants
        } else {
            productVariants.map { variant ->
                if (variant.id == updated.id) updated else variant
            }
        }
    }
}

fun quickEditValueAsBodyNumber(fieldIsStock: Boolean, value: Double): Any {
    if (fieldIsStock) {
        if (!value.isFinite() || value < 0.0 || value > MOBILE_PRODUCT_QUICK_EDIT_MAX_STOCK.toDouble() || value % 1.0 != 0.0) {
            throw IllegalArgumentException("Stock value must be a non-negative integer.")
        }

        return value.toInt()
    }

    if (!value.isFinite() || value < 0.0 || value > MOBILE_PRODUCT_QUICK_EDIT_MAX_PRICE) {
        throw IllegalArgumentException("Price value must be a finite non-negative number.")
    }

    return value
}

fun normalizeMobileProductsCursor(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    return trimmed.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
}

fun mobileProductStatusLabel(status: String, lowStock: Boolean): String? {
    val trimmedStatus = status.trim()
    val normalized = trimmedStatus.lowercase(Locale.ROOT)
    if (normalized.isBlank()) {
        return if (lowStock) "Niski stan" else null
    }

    return when (normalized) {
        "brak stanu", "out_of_stock", "out-of-stock" -> "Brak stanu"
        "niski stan", "low_stock", "low-stock" -> "Niski stan"
        "active", "published", "aktywny" -> "Aktywny"
        "draft", "roboczy" -> "Roboczy"
        "inactive", "disabled", "archived", "nieaktywny" -> "Nieaktywny"
        else -> trimmedStatus.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }
}

fun mobileVariantThumbnailUrl(variant: MobileProductVariant, product: MobileProduct): String {
    return variant.thumbnailUrl
        .ifBlank { variant.image }
        .ifBlank { product.thumbnailUrl }
        .ifBlank { product.image }
}

private fun encodeQueryValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
}
