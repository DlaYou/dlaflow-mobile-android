package pl.dlaflow.mobile.feature.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class OrdersStateHolderTest {
    private val retryableMessage = DlaFlowUiMessage(1, 2, retryable = true)

    @Test
    fun `zero result is business empty while nonempty result is content`() {
        val holder = OrdersStateHolder()
        val emptyRequest = holder.beginListReset("session-a", OrdersQuery())

        assertTrue(holder.acceptListSuccess(emptyRequest, page()))
        assertEquals(DlaFlowUiState.Empty, holder.state.listState)

        val contentRequest = holder.beginListReset("session-a", OrdersQuery(search = "ORD"))
        val content = page(order("id-1", "ORD-1"), total = 1)
        assertTrue(holder.acceptListSuccess(contentRequest, content))
        assertEquals(DlaFlowUiState.Content(content), holder.state.listState)
    }

    @Test
    fun `refresh and offline retain the last list content`() {
        val holder = holderWithContent()
        val previous = holder.state.listContentOrNull()!!

        val refresh = holder.beginListRefresh("session-a")
        assertEquals(previous, holder.state.listContentOrNull())
        assertTrue(holder.state.isRefreshing)

        assertTrue(holder.acceptListOffline(refresh, retryableMessage))
        assertEquals(DlaFlowUiState.Offline(previous), holder.state.listState)
        assertEquals(previous, holder.state.listContentOrNull())
        assertEquals(retryableMessage, holder.state.transientMessage)
        assertFalse(holder.state.isRefreshing)
    }

    @Test
    fun `list request requires newest id and matching session`() {
        val holder = OrdersStateHolder()
        val stale = holder.beginListReset("session-a", OrdersQuery())
        val current = holder.beginListReset("session-a", OrdersQuery(search = "new"))
        val before = holder.state

        assertFalse(holder.acceptListSuccess(stale, page(order("old", "OLD"))))
        assertFalse(holder.acceptListFailure(current.copy(sessionKey = "session-b"), retryableMessage))
        assertEquals(before, holder.state)
        assertTrue(holder.acceptListSuccess(current, page(order("new", "NEW"), total = 1)))
    }

    @Test
    fun `load more rejects overlap and merges pages by id or order number`() {
        val holder = OrdersStateHolder()
        val first = holder.beginListReset("session-a", OrdersQuery())
        holder.acceptListSuccess(
            first,
            page(
                order("id-1", "ORD-1"),
                order("id-2", "ORD-2"),
                total = 4,
                nextOffset = 2,
            ),
        )

        val loadMore = holder.beginLoadMore("session-a")
        assertTrue(loadMore != null)
        assertNull(holder.beginLoadMore("session-a"))
        assertTrue(holder.state.isLoadingMore)
        assertEquals(2, loadMore!!.offset)

        assertTrue(
            holder.acceptListSuccess(
                loadMore,
                page(
                    order("id-1", "ORD-1-duplicate-by-id"),
                    order("different-id", "ORD-2"),
                    order("id-3", "ORD-3"),
                    total = 4,
                    nextOffset = null,
                ),
            ),
        )

        val merged = holder.state.listContentOrNull()!!
        assertEquals(listOf("ORD-1-duplicate-by-id", "ORD-2", "ORD-3"), merged.items.map { it.orderNumber })
        assertFalse(holder.state.isLoadingMore)
        assertNull(holder.beginLoadMore("session-a"))
    }

    @Test
    fun `detail has independent request chain and rejects stale callbacks`() {
        val holder = holderWithContent()
        val stale = holder.beginDetailLoad("session-a", "ORD-1")
        val current = holder.beginDetailLoad("session-a", "ORD-2")

        assertFalse(holder.acceptDetailSuccess(stale, detail("ORD-1")))
        assertFalse(holder.acceptDetailFailure(stale, retryableMessage))
        assertTrue(holder.acceptDetailSuccess(current, detail("ORD-2")))
        assertEquals(DlaFlowUiState.Content(detail("ORD-2")), holder.state.detailState)
        assertEquals(OrdersRoute.Detail("ORD-2"), holder.state.route)
    }

    @Test
    fun `detail fills missing product image from the normalized list item`() {
        val holder = OrdersStateHolder()
        val listRequest = holder.beginListReset("session-a", OrdersQuery())
        assertTrue(
            holder.acceptListSuccess(
                listRequest,
                page(
                    order("id-1", "ORD-1").copy(
                        thumbnailUrl = "/api/mobile/products/media/order.webp",
                        products = listOf(
                            OrdersListProduct(
                                image = "/api/mobile/products/media/product.webp",
                                name = "Produkt",
                                quantity = 1,
                                sku = "SKU-1",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val detailRequest = holder.beginDetailLoad("session-a", "ORD-1")
        val detail = detail("ORD-1").copy(
            items = listOf(
                OrderItem("item-1", "Produkt", "SKU-1", 1, 10.0, 10.0),
            ),
        )

        assertTrue(holder.acceptDetailSuccess(detailRequest, detail))
        assertEquals(
            "/api/mobile/products/media/product.webp",
            holder.state.detailContentOrNull()?.items?.single()?.image,
        )
    }

    @Test
    fun `detail replaces a non mobile product image with the canonical list image`() {
        val holder = OrdersStateHolder()
        val listRequest = holder.beginListReset("session-a", OrdersQuery())
        assertTrue(
            holder.acceptListSuccess(
                listRequest,
                page(
                    order("id-1", "ORD-1").copy(
                        products = listOf(
                            OrdersListProduct(
                                image = "/api/mobile/orders/media/canonical.webp?variant=thumb",
                                name = "Produkt",
                                quantity = 1,
                                sku = "SKU-1",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val detailRequest = holder.beginDetailLoad("session-a", "ORD-1")
        val detail = detail("ORD-1").copy(
            items = listOf(
                OrderItem("item-1", "Produkt", "SKU-1", 1, 10.0, 10.0, "/api/products/media/legacy.webp"),
            ),
        )

        assertTrue(holder.acceptDetailSuccess(detailRequest, detail))
        assertEquals(
            "/api/mobile/orders/media/canonical.webp?variant=thumb",
            holder.state.detailContentOrNull()?.items?.single()?.image,
        )
    }

    @Test
    fun `list and detail requests can complete independently`() {
        val holder = OrdersStateHolder()
        val listRequest = holder.beginListReset("session-a", OrdersQuery())
        val detailRequest = holder.beginDetailLoad("session-a", "ORD-1")

        assertTrue(holder.acceptListSuccess(listRequest, page(order("id-1", "ORD-1"), total = 1)))
        assertEquals(OrdersRoute.Detail("ORD-1"), holder.state.route)
        assertEquals(DlaFlowUiState.Loading, holder.state.detailState)
        assertTrue(holder.acceptDetailSuccess(detailRequest, detail("ORD-1")))
        assertEquals(1, holder.state.listContentOrNull()?.items?.size)
    }

    @Test
    fun `closing while detail loads invalidates its request`() {
        val holder = holderWithContent()
        val request = holder.beginDetailLoad("session-a", "ORD-1")

        holder.closeDetail()

        assertEquals(OrdersRoute.List, holder.state.route)
        assertNull(holder.state.detailState)
        assertNull(holder.state.activeDetailRequestId)
        assertFalse(holder.acceptDetailSuccess(request, detail("ORD-1")))
    }

    @Test
    fun `list reset invalidates detail and returns to list`() {
        val holder = holderWithContent()
        val detailRequest = holder.beginDetailLoad("session-a", "ORD-1")

        val listRequest = holder.beginListReset("session-a", OrdersQuery(filter = OrdersFilter.NEW))

        assertEquals(OrdersRoute.List, holder.state.route)
        assertNull(holder.state.detailState)
        assertFalse(holder.acceptDetailSuccess(detailRequest, detail("ORD-1")))
        assertEquals(OrdersFilter.NEW, listRequest.query.filter)
    }

    @Test
    fun `list refresh preserves an open detail route`() {
        val holder = holderWithContent()
        holder.beginDetailLoad("session-a", "ORD-1")

        holder.beginListRefresh("session-a")

        assertEquals(OrdersRoute.Detail("ORD-1"), holder.state.route)
        assertEquals(DlaFlowUiState.Loading, holder.state.detailState)
    }

    @Test
    fun `whole feature no access invalidates both chains`() {
        val holder = OrdersStateHolder()
        val listRequest = holder.beginListReset("session-a", OrdersQuery())
        val detailRequest = holder.beginDetailLoad("session-a", "ORD-1")

        assertTrue(holder.acceptDetailNoAccess(detailRequest))
        assertEquals(DlaFlowUiState.NoAccess, holder.state.listState)
        assertEquals(OrdersRoute.List, holder.state.route)
        assertNull(holder.state.detailState)
        assertFalse(holder.acceptListSuccess(listRequest, page(order("id-1", "ORD-1"))))
    }

    @Test
    fun `list no access also replaces the entire feature`() {
        val holder = OrdersStateHolder()
        val listRequest = holder.beginListReset("session-a", OrdersQuery())
        val detailRequest = holder.beginDetailLoad("session-a", "ORD-1")

        assertTrue(holder.acceptListNoAccess(listRequest))
        assertEquals(DlaFlowUiState.NoAccess, holder.state.listState)
        assertEquals(OrdersRoute.List, holder.state.route)
        assertFalse(holder.acceptDetailSuccess(detailRequest, detail("ORD-1")))
    }

    @Test
    fun `reset invalidates both chains without reusing ids`() {
        val holder = OrdersStateHolder()
        val oldList = holder.beginListReset("session-a", OrdersQuery())
        val oldDetail = holder.beginDetailLoad("session-a", "ORD-1")

        holder.reset()

        val newList = holder.beginListReset("session-b", OrdersQuery())
        val newDetail = holder.beginDetailLoad("session-b", "ORD-2")
        assertTrue(newList.requestId > oldList.requestId)
        assertTrue(newDetail.requestId > oldDetail.requestId)
        assertFalse(holder.acceptListSuccess(oldList, page(order("old", "OLD"))))
        assertFalse(holder.acceptDetailSuccess(oldDetail, detail("OLD")))
    }

    @Test
    fun `unauthorized validates active request and preserves query for host confirmation`() {
        val holder = OrdersStateHolder()
        val stale = holder.beginListReset("session-a", OrdersQuery())
        val active = holder.beginListReset("session-a", OrdersQuery(search = "current"))

        assertFalse(holder.acceptListUnauthorized(stale))
        assertTrue(holder.acceptListUnauthorized(active))
        assertEquals(OrdersQuery(search = "current"), holder.state.query)
        assertEquals(DlaFlowUiState.Loading, holder.state.listState)
        assertNull(holder.state.activeListRequestId)

        val next = holder.beginListReset("session-b", OrdersQuery())
        assertTrue(next.requestId > active.requestId)
    }

    @Test
    fun `unauthorized load more preserves page and can be retried`() {
        val holder = OrdersStateHolder()
        val initial = holder.beginListReset("session-a", OrdersQuery())
        holder.acceptListSuccess(
            initial,
            page(order("id-1", "ORD-1"), total = 2, nextOffset = 1),
        )
        val unauthorized = holder.beginLoadMore("session-a")!!

        assertTrue(holder.acceptListUnauthorized(unauthorized))

        assertEquals(listOf("ORD-1"), holder.state.listContentOrNull()!!.items.map { it.orderNumber })
        assertEquals(1, holder.state.listContentOrNull()!!.nextOffset)
        assertEquals(1, holder.beginLoadMore("session-a")!!.offset)
    }

    @Test
    fun `second unauthorized ends loading with a non retryable error`() {
        val holder = OrdersStateHolder()
        val request = holder.beginListReset("session-a", OrdersQuery(search = "current"))
        val terminalMessage = DlaFlowUiMessage(3, 4, retryable = false)

        assertTrue(holder.acceptListUnauthorized(request, terminalMessage))

        assertEquals(DlaFlowUiState.Error(terminalMessage), holder.state.listState)
        assertEquals(OrdersQuery(search = "current"), holder.state.query)
        assertNull(holder.state.activeListRequestId)
    }

    private fun holderWithContent(): OrdersStateHolder = OrdersStateHolder().also { holder ->
        val request = holder.beginListReset("session-a", OrdersQuery())
        holder.acceptListSuccess(request, page(order("id-1", "ORD-1"), total = 1))
    }
}

private fun page(
    vararg items: OrdersListItem,
    total: Int = items.size,
    nextOffset: Int? = null,
) = OrdersListContent(items.toList(), total, nextOffset)

private fun order(id: String, number: String) = OrdersListItem(
    id = id,
    orderNumber = number,
    amount = 10.0,
    currency = "PLN",
    customer = "Klient",
    channel = "Panel",
    createdAt = "",
    itemCount = 1,
    productSummary = "Produkt",
    paymentStatus = "Opłacone",
    paymentTone = "success",
    phone = "",
    shippingMethod = "",
    status = "new",
    statusTone = "info",
    thumbnailUrl = "",
    badges = OrdersBadges(0, 0, 0),
)

private fun detail(number: String) = OrderDetailContent(
    id = "detail-$number",
    orderNumber = number,
    amount = 10.0,
    currency = "PLN",
    status = "new",
    statusTone = "info",
    productSummary = "Produkt",
    itemCount = 1,
    customer = OrderCustomer("Klient", "", "", ""),
    delivery = OrderDelivery(OrderAddress("", "", "", "", "", "", "", ""), ""),
    payment = OrderPayment("PLN", "", 10.0, "paid", "success"),
    items = emptyList(),
    shipments = emptyList(),
    messages = emptyList(),
    documentsCount = 0,
    internalNotesCount = 0,
    statusHistoryCount = 0,
)
