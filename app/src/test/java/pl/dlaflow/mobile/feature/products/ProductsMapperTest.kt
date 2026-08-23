package pl.dlaflow.mobile.feature.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import pl.dlaflow.mobile.MobileProduct
import pl.dlaflow.mobile.MobileProductEditableFields
import pl.dlaflow.mobile.MobileProductVariant
import pl.dlaflow.mobile.MobileProductVariantEditableFields
import pl.dlaflow.mobile.MobileProductsPage

class ProductsMapperTest {
    @Test
    fun `transport page maps to presentation models without dto leakage`() {
        val content = pageDto().toProductsContent()

        assertEquals("product-1", content.items.single().id)
        assertEquals("EUR", content.items.single().currency)
        assertEquals("/api/mobile/products/media/product.webp", content.items.single().thumbnailUrl)
        assertEquals("Aktywny", content.items.single().status.label)
        assertEquals(ProductStatusTone.SUCCESS, content.items.single().status.tone)
        assertFalse(
            (ProductsUiState::class.java.declaredFields + ProductItem::class.java.declaredFields)
                .any { it.type.name.contains("MobileProduct") },
        )
    }

    @Test
    fun `legacy panel product media reference maps to the mobile thumbnail route`() {
        val item = productDto(
            image = "/api/products/media/product.webp",
            thumbnailUrl = "",
        ).toProductItem()

        assertEquals(
            "/api/mobile/products/media/product.webp?variant=thumb",
            item.thumbnailUrl,
        )
    }

    @Test
    fun `external product media reference is ignored instead of leaving the signed mobile path`() {
        val item = productDto(
            image = "https://cdn.example.test/product.webp",
            thumbnailUrl = "",
        ).toProductItem()

        assertEquals("", item.thumbnailUrl)
    }

    @Test
    fun `product gallery media is used when primary image fields are empty`() {
        val item = productDto(
            image = "",
            thumbnailUrl = "",
            media = listOf("/api/mobile/products/media/flowers.webp?variant=thumb"),
        ).toProductItem()

        assertEquals(
            "/api/mobile/products/media/flowers.webp?variant=thumb",
            item.thumbnailUrl,
        )
    }

    @Test
    fun `absolute canonical mobile media keeps its path for same-origin resolution`() {
        val item = productDto(
            image = "",
            thumbnailUrl = "https://panel.dlayou.pl/api/mobile/products/media/flowers.webp?variant=thumb",
        ).toProductItem()

        assertEquals(
            "https://panel.dlayou.pl/api/mobile/products/media/flowers.webp?variant=thumb",
            item.thumbnailUrl,
        )
    }

    @Test
    fun `unknown product status is closed neutral copy`() {
        val item = productDto(status = "RAW_BACKEND_STATUS").toProductItem()

        assertEquals("Status nieznany", item.status.label)
        assertEquals(ProductStatusTone.NEUTRAL, item.status.tone)
        assertFalse(item.toString().contains("RAW_BACKEND_STATUS"))
    }

    @Test
    fun `variant identity must match requested product`() {
        assertThrows(InvalidProductsResultException::class.java) {
            variantDto(productId = "other-product").toProductVariant(expectedProductId = "product-1")
        }
    }

    @Test
    fun `variant thumbnail falls back to parent presentation thumbnail`() {
        val variant = variantDto().toProductVariant(
            expectedProductId = "product-1",
            parentThumbnailUrl = "/api/mobile/products/media/parent.webp",
        )

        assertEquals("/api/mobile/products/media/parent.webp", variant.thumbnailUrl)
    }

    @Test
    fun `duplicate variant identities fail closed`() {
        assertThrows(InvalidProductsResultException::class.java) {
            listOf(variantDto(id = "variant-1"), variantDto(id = "variant-1")).toProductVariants(
                expectedProductId = "product-1",
                parentThumbnailUrl = "",
            )
        }
    }

    @Test
    fun `invalid numeric and duplicate transport shapes fail closed`() {
        assertThrows(InvalidProductsResultException::class.java) {
            productDto(grossPrice = Double.NaN).toProductItem()
        }
        assertThrows(InvalidProductsResultException::class.java) {
            pageDto(data = listOf(productDto(), productDto())).toProductsContent()
        }
    }

    @Test
    fun `feature filters map only at compatibility boundary`() {
        assertEquals("ALL", ProductsFilter.ALL.toMobileProductFilter().name)
        assertEquals("LOW_STOCK", ProductsFilter.LOW_STOCK.toMobileProductFilter().name)
        assertEquals("NO_IMAGE", ProductsFilter.NO_IMAGE.toMobileProductFilter().name)
        assertEquals("HAS_VARIANTS", ProductsFilter.HAS_VARIANTS.toMobileProductFilter().name)
    }
}

internal fun pageDto(
    data: List<MobileProduct> = listOf(productDto()),
    nextCursor: String? = "cursor-2",
    total: Int = data.size,
    canEdit: Boolean = true,
) = MobileProductsPage(data, nextCursor, total, canEdit)

internal fun productDto(
    id: String = "product-1",
    grossPrice: Double = 19.99,
    status: String = "ACTIVE",
    variantCount: Int = 1,
    image: String = "/api/products/media/product.webp",
    thumbnailUrl: String = "/api/mobile/products/media/product.webp",
    media: List<String> = emptyList(),
) = MobileProduct(
    id = id,
    name = "Produkt testowy",
    sku = "SKU-1",
    ean = "5900000000001",
    image = image,
    thumbnailUrl = thumbnailUrl,
    media = media,
    grossPrice = grossPrice,
    stock = 7,
    status = status,
    currency = "EUR",
    variantCount = variantCount,
    lowStock = false,
    editableFields = MobileProductEditableFields(grossPrice = true, stock = true),
)

internal fun variantDto(
    id: String = "variant-1",
    productId: String = "product-1",
) = MobileProductVariant(
    id = id,
    productId = productId,
    name = "Wariant testowy",
    sku = "SKU-V1",
    ean = "",
    image = "",
    thumbnailUrl = "",
    price = 21.0,
    stock = 3,
    status = "ACTIVE",
    editableFields = MobileProductVariantEditableFields(price = true, stock = true),
)
