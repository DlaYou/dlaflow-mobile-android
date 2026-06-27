package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileProductsStateTest {
    @Test
    fun `query all default has only paging and sort parameters`() {
        val query = buildMobileProductsQuery(
            search = "",
            filter = MobileProductFilter.ALL,
        )

        assertEquals("limit=20&countMode=window&sort=id&direction=asc", query)
    }

    @Test
    fun `search is trimmed and URL encoded`() {
        val query = buildMobileProductsQuery(
            search = "  bialy kubek+XL  ",
            filter = MobileProductFilter.ALL,
        )

        assertEquals("limit=20&countMode=window&sort=id&direction=asc&search=bialy+kubek%2BXL", query)
    }

    @Test
    fun `filters map to product query parameters`() {
        assertEquals(
            "limit=20&countMode=window&sort=id&direction=asc&stockState=low-stock",
            buildMobileProductsQuery("", MobileProductFilter.LOW_STOCK),
        )
        assertEquals(
            "limit=20&countMode=window&sort=id&direction=asc&noImage=true",
            buildMobileProductsQuery("", MobileProductFilter.NO_IMAGE),
        )
        assertEquals(
            "limit=20&countMode=window&sort=id&direction=asc&hasVariants=true",
            buildMobileProductsQuery("", MobileProductFilter.HAS_VARIANTS),
        )
    }

    @Test
    fun `cursor is URL encoded`() {
        val query = buildMobileProductsQuery(
            search = "",
            filter = MobileProductFilter.ALL,
            cursor = "product/id 7",
        )

        assertEquals("limit=20&countMode=window&sort=id&direction=asc&cursor=product%2Fid+7", query)
    }

    @Test
    fun `cursor normalizer rejects blank and JSON null values`() {
        assertEquals(null, normalizeMobileProductsCursor(null))
        assertEquals(null, normalizeMobileProductsCursor(""))
        assertEquals(null, normalizeMobileProductsCursor("   "))
        assertEquals(null, normalizeMobileProductsCursor("null"))
        assertEquals(null, normalizeMobileProductsCursor(" NULL "))
        assertEquals("product-7", normalizeMobileProductsCursor(" product-7 "))
    }

    @Test
    fun `status label preserves out of stock even when low stock flag is true`() {
        assertEquals("Brak stanu", mobileProductStatusLabel("Brak stanu", lowStock = true))
    }

    @Test
    fun `status label falls back to low stock only when API status is blank`() {
        assertEquals("Niski stan", mobileProductStatusLabel("", lowStock = true))
        assertEquals(null, mobileProductStatusLabel(" ", lowStock = false))
    }

    @Test
    fun `variant thumbnail prefers variant image over product image`() {
        val product = mobileProduct(
            image = "/api/products/media/product.webp",
            thumbnailUrl = "/api/mobile/products/media/product.webp?variant=thumb",
        )
        val variant = mobileProductVariant(
            image = "/api/products/media/variant.webp",
            thumbnailUrl = "/api/mobile/products/media/variant.webp?variant=thumb",
        )

        assertEquals(
            "/api/mobile/products/media/variant.webp?variant=thumb",
            mobileVariantThumbnailUrl(variant, product),
        )
    }

    @Test
    fun `variant thumbnail falls back to parent product image`() {
        val product = mobileProduct(
            image = "/api/products/media/product.webp",
            thumbnailUrl = "/api/mobile/products/media/product.webp?variant=thumb",
        )
        val variant = mobileProductVariant(image = "", thumbnailUrl = "")

        assertEquals(
            "/api/mobile/products/media/product.webp?variant=thumb",
            mobileVariantThumbnailUrl(variant, product),
        )
    }

    @Test
    fun `canQuickEditProduct allows editable product without variants`() {
        val decision = canQuickEditProduct(
            product = mobileProduct(
                variantCount = 0,
                editableFields = MobileProductEditableFields(grossPrice = true, stock = true),
            ),
            field = MobileProductQuickEditField.GROSS_PRICE,
        )

        assertTrue(decision.allowed)
        assertEquals("", decision.reason)
    }

    @Test
    fun `canQuickEditProduct rejects aggregate product with variants`() {
        val decision = canQuickEditProduct(
            product = mobileProduct(variantCount = 2),
            field = MobileProductQuickEditField.STOCK,
        )

        assertFalse(decision.allowed)
        assertEquals("Wariantowy produkt edytuj na poziomie wariantu.", decision.reason)
    }

    @Test
    fun `canQuickEditProduct rejects disabled editable field`() {
        val decision = canQuickEditProduct(
            product = mobileProduct(
                editableFields = MobileProductEditableFields(grossPrice = true, stock = false),
            ),
            field = MobileProductQuickEditField.STOCK,
        )

        assertFalse(decision.allowed)
        assertEquals("Brak uprawnienia do edycji tego pola.", decision.reason)
    }

    @Test
    fun `replaceMobileProduct replaces only matching product and preserves others`() {
        val first = mobileProduct(id = "product-1", name = "Pierwszy")
        val second = mobileProduct(id = "product-2", name = "Drugi")
        val updated = first.copy(name = "Po zmianie")

        val products = replaceMobileProduct(listOf(first, second), updated)

        assertEquals(listOf(updated, second), products)
    }

    @Test
    fun `replaceMobileVariant replaces only matching variant and preserves others`() {
        val first = mobileProductVariant(id = "variant-1", productId = "product-1", name = "Pierwszy")
        val second = mobileProductVariant(id = "variant-2", productId = "product-1", name = "Drugi")
        val other = mobileProductVariant(id = "variant-3", productId = "product-2", name = "Inny")
        val updated = first.copy(name = "Po zmianie")

        val variants = replaceMobileVariant(
            variants = mapOf(
                "product-1" to listOf(first, second),
                "product-2" to listOf(other),
            ),
            updated = updated,
        )

        assertEquals(listOf(updated, second), variants["product-1"])
        assertEquals(listOf(other), variants["product-2"])
    }

    @Test
    fun `stock body helper converts whole double to int`() {
        val number = quickEditValueAsBodyNumber(fieldIsStock = true, value = 7.0)

        assertTrue(number is Int)
        assertEquals(7, number)
    }

    @Test
    fun `stock body helper rejects fractional negative and non finite values`() {
        listOf(7.9, -1.0, MOBILE_PRODUCT_QUICK_EDIT_MAX_STOCK + 1.0, Double.NaN).forEach { value ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                quickEditValueAsBodyNumber(fieldIsStock = true, value = value)
            }

            assertEquals("Stock value must be a non-negative integer.", error.message)
        }
    }

    @Test
    fun `price body helper rejects negative and non finite values`() {
        listOf(-1.0, MOBILE_PRODUCT_QUICK_EDIT_MAX_PRICE + 0.01, Double.POSITIVE_INFINITY).forEach { value ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                quickEditValueAsBodyNumber(fieldIsStock = false, value = value)
            }

            assertEquals("Price value must be a finite non-negative number.", error.message)
        }
    }

    private fun mobileProduct(
        id: String = "product-1",
        name: String = "Produkt",
        image: String = "",
        thumbnailUrl: String = "",
        variantCount: Int = 0,
        editableFields: MobileProductEditableFields = MobileProductEditableFields(grossPrice = true, stock = true),
    ): MobileProduct {
        return MobileProduct(
            id = id,
            name = name,
            sku = "SKU-1",
            ean = "",
            image = image,
            thumbnailUrl = thumbnailUrl,
            grossPrice = 19.99,
            stock = 7,
            status = "ACTIVE",
            currency = "PLN",
            variantCount = variantCount,
            lowStock = false,
            editableFields = editableFields,
        )
    }

    private fun mobileProductVariant(
        id: String = "variant-1",
        productId: String = "product-1",
        name: String = "Wariant",
        image: String = "",
        thumbnailUrl: String = "",
    ): MobileProductVariant {
        return MobileProductVariant(
            id = id,
            productId = productId,
            name = name,
            sku = "SKU-V",
            ean = "",
            image = image,
            thumbnailUrl = thumbnailUrl,
            price = 12.5,
            stock = 4,
            status = "ACTIVE",
            editableFields = MobileProductVariantEditableFields(price = true, stock = true),
        )
    }

}
