package pl.dlaflow.mobile.feature.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class ProductsStateHolderTest {
    private val message = DlaFlowUiMessage(1, 2, retryable = true)

    @Test
    fun `new reset rejects stale response and request ids stay monotonic across reset`() {
        val holder = ProductsStateHolder()
        val old = holder.beginListReset("session-a", ProductsQuery(search = "old"))
        val current = holder.beginListReset("session-a", ProductsQuery(search = "new"))

        assertFalse(holder.acceptListSuccess(old, productsContent("old")))
        assertTrue(holder.acceptListSuccess(current, productsContent("new")))
        assertEquals("new", holder.state.listContentOrNull()?.items?.single()?.id)
        holder.reset()
        val afterReset = holder.beginListReset("session-b", ProductsQuery())

        assertTrue(afterReset.requestId > current.requestId)
    }

    @Test
    fun `pagination is single flight per cursor and merges duplicate identity`() {
        val holder = ProductsStateHolder()
        val initial = holder.beginListReset("session-a", ProductsQuery())
        holder.acceptListSuccess(initial, productsContent("p1", nextCursor = "cursor-2"))

        val append = holder.beginLoadMore("session-a")!!
        assertNull(holder.beginLoadMore("session-a"))
        assertTrue(
            holder.acceptListSuccess(
                append,
                ProductsContent(
                    items = listOf(productItem("p1", name = "Nowa nazwa"), productItem("p2")),
                    total = 2,
                    nextCursor = "cursor-2",
                    canEdit = true,
                ),
            ),
        )

        assertEquals(listOf("p1", "p2"), holder.state.listContentOrNull()?.items?.map { it.id })
        assertEquals("Nowa nazwa", holder.state.listContentOrNull()?.items?.first()?.name)
        assertNull(holder.beginLoadMore("session-a"))
    }

    @Test
    fun `collapsed product rejects late variant result`() {
        val holder = loadedHolder()
        val request = holder.beginVariantsLoad("session-a", "p1")!!

        assertTrue(holder.collapseVariants("p1"))
        assertFalse(holder.acceptVariantsSuccess(request, listOf(variantItem("v1", "p1"))))
        assertFalse("p1" in holder.state.expandedProductIds)
        assertFalse(holder.state.variants.containsKey("p1"))
    }

    @Test
    fun `quick edit rejects duplicate submit and stale completion`() {
        val holder = loadedHolder(product = productItem("p1", variantCount = 0))
        val target = ProductQuickEditTarget.Product("p1", ProductQuickEditField.GROSS_PRICE)
        val first = holder.beginQuickEdit("session-a", target, 25.0)!!

        assertNull(holder.beginQuickEdit("session-a", target, 26.0))
        holder.beginListReset("session-a", ProductsQuery(search = "new"))
        assertFalse(holder.acceptQuickEditProductSuccess(first, productItem("p1", grossPrice = 25.0, variantCount = 0)))
    }

    @Test
    fun `unauthorized list retry can begin exactly once`() {
        val holder = ProductsStateHolder()
        val request = holder.beginListReset("session-a", ProductsQuery())

        assertTrue(holder.acceptListUnauthorized(request))
        assertTrue(holder.beginListUnauthorizedRetry(request) != null)
        assertNull(holder.beginListUnauthorizedRetry(request))
    }

    @Test
    fun `quick edit no access preserves products and closes edit capability`() {
        val holder = loadedHolder(product = productItem("p1", variantCount = 0))
        val target = ProductQuickEditTarget.Product("p1", ProductQuickEditField.STOCK)
        val request = holder.beginQuickEdit("session-a", target, 8.0)!!

        assertTrue(holder.acceptQuickEditFailure(request, message, noAccess = true))

        assertEquals("p1", holder.state.listContentOrNull()?.items?.single()?.id)
        assertFalse(holder.state.listContentOrNull()!!.canEdit)
        assertFalse(holder.state.quickEdit!!.isSaving)
        assertEquals(message, holder.state.quickEdit!!.message)
    }

    @Test
    fun `quick edit reports fractional stock and both upper limits without starting requests`() {
        val holder = loadedHolder(productItem("p1", variantCount = 0))
        val stockTarget = ProductQuickEditTarget.Product("p1", ProductQuickEditField.STOCK)
        assertTrue(holder.openQuickEdit(stockTarget))

        assertNull(holder.beginQuickEdit("session-a", stockTarget, 1.5))
        assertEquals(
            R.string.mobile_products_quick_edit_stock_integer,
            holder.state.quickEdit!!.message!!.descriptionRes,
        )
        assertNull(holder.beginQuickEdit("session-a", stockTarget, 10_000_001.0))
        assertEquals(
            R.string.mobile_products_quick_edit_too_high,
            holder.state.quickEdit!!.message!!.descriptionRes,
        )

        assertTrue(holder.cancelQuickEdit())
        val priceTarget = ProductQuickEditTarget.Product("p1", ProductQuickEditField.GROSS_PRICE)
        assertTrue(holder.openQuickEdit(priceTarget))
        assertNull(holder.beginQuickEdit("session-a", priceTarget, 10_000_001.0))
        assertEquals(
            R.string.mobile_products_quick_edit_too_high,
            holder.state.quickEdit!!.message!!.descriptionRes,
        )
    }

    private fun loadedHolder(product: ProductItem = productItem("p1")): ProductsStateHolder {
        val holder = ProductsStateHolder()
        val request = holder.beginListReset("session-a", ProductsQuery())
        holder.acceptListSuccess(
            request,
            ProductsContent(listOf(product), total = 1, nextCursor = null, canEdit = true),
        )
        return holder
    }
}

internal fun productsContent(id: String, nextCursor: String? = null) = ProductsContent(
    items = listOf(productItem(id)),
    total = 1,
    nextCursor = nextCursor,
    canEdit = true,
)

internal fun productItem(
    id: String,
    name: String = "Produkt",
    grossPrice: Double = 19.0,
    variantCount: Int = 1,
) = ProductItem(
    id = id,
    name = name,
    sku = "SKU",
    ean = "",
    thumbnailUrl = "",
    grossPrice = grossPrice,
    stock = 4,
    status = ProductStatus("Aktywny", ProductStatusTone.SUCCESS),
    currency = "PLN",
    variantCount = variantCount,
    lowStock = false,
    editableFields = ProductEditableFields(grossPrice = true, stock = true),
)

internal fun variantItem(id: String, productId: String) = ProductVariant(
    id = id,
    productId = productId,
    name = "Wariant",
    sku = "SKU-V",
    ean = "",
    thumbnailUrl = "",
    price = 20.0,
    stock = 2,
    status = ProductStatus("Aktywny", ProductStatusTone.SUCCESS),
    editableFields = VariantEditableFields(price = true, stock = true),
)
