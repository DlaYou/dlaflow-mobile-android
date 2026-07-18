package pl.dlaflow.mobile.feature.orders

import java.net.UnknownHostException
import java.util.ArrayDeque
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class OrdersCoordinatorTest {
    @Test
    fun `list and detail results reach state only on main queue`() {
        val harness = OrdersCoordinatorHarness()

        harness.coordinator.resetList("session-a", OrdersQuery(), showFeedback = true)
        harness.executor.runNext()
        assertEquals(DlaFlowUiState.Loading, harness.holder.state.listState)
        harness.mainQueue.runNext()
        assertEquals("ORD-1001", harness.holder.state.listContentOrNull()!!.items.single().orderNumber)

        harness.coordinator.loadDetail("session-a", "ORD-1001", showFeedback = true)
        harness.executor.runNext()
        assertEquals(DlaFlowUiState.Loading, harness.holder.state.detailState)
        harness.mainQueue.runNext()

        assertEquals("ORD-1001", harness.holder.state.detailContentOrNull()!!.orderNumber)
        assertEquals(
            listOf(
                OrdersFeedback.LIST_LOADING,
                OrdersFeedback.LIST_READY,
                OrdersFeedback.DETAIL_LOADING,
                OrdersFeedback.DETAIL_READY,
            ),
            harness.feedback,
        )
    }

    @Test
    fun `new list request wins when callbacks finish out of order`() {
        val harness = OrdersCoordinatorHarness(
            pageForQuery = { query ->
                ordersPage(coordinatorOrder(id = query.search, orderNumber = query.search))
            },
        )

        harness.coordinator.resetList("session-a", OrdersQuery(search = "OLD"), showFeedback = false)
        harness.coordinator.resetList("session-a", OrdersQuery(search = "NEW"), showFeedback = false)
        harness.executor.runLast()
        harness.executor.runNext()
        harness.mainQueue.runAll()

        assertEquals("NEW", harness.holder.state.listContentOrNull()!!.items.single().orderNumber)
    }

    @Test
    fun `closing detail before callback prevents stale reopen`() {
        val harness = OrdersCoordinatorHarness()
        harness.coordinator.loadDetail("session-a", "ORD-1001", showFeedback = true)

        harness.coordinator.closeDetail()
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(OrdersRoute.List, harness.holder.state.route)
        assertEquals(null, harness.holder.state.detailState)
        assertEquals(
            listOf(OrdersFeedback.DETAIL_LOADING, OrdersFeedback.DETAIL_CLOSED),
            harness.feedback,
        )
    }

    @Test
    fun `offline list failure is retained as typed state`() {
        val error = UnknownHostException("offline")
        val harness = OrdersCoordinatorHarness(pageForQuery = { throw error })

        harness.coordinator.resetList("session-a", OrdersQuery(), showFeedback = true)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertTrue(harness.holder.state.listState is DlaFlowUiState.Offline)
        assertEquals(listOf(OrdersFeedback.LIST_LOADING, OrdersFeedback.LOAD_FAILED), harness.feedback)
    }

    @Test
    fun `forbidden detail makes whole feature no access`() {
        val harness = OrdersCoordinatorHarness(
            detailForOrder = { throw MobileApiException(403, "MOBILE_DEVICE_REQUIRED", "forbidden") },
        )

        harness.coordinator.loadDetail("session-a", "ORD-1001", showFeedback = false)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(DlaFlowUiState.NoAccess, harness.holder.state.listState)
        assertEquals(OrdersRoute.List, harness.holder.state.route)
    }

    @Test
    fun `accepted unauthorized includes exact operation and retry allowance`() {
        val error = MobileApiException(401, "AUTH_REQUIRED", "expired")
        val harness = OrdersCoordinatorHarness(detailForOrder = { throw error })

        harness.coordinator.loadDetail(
            token = "session-a",
            orderNumber = "ORD-1001",
            showFeedback = false,
            allowUnauthorizedRetry = true,
        )
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(1, harness.unauthorized.size)
        assertSame(error, harness.unauthorized.single().error)
        assertEquals(OrdersLoadOperation.Detail("ORD-1001"), harness.unauthorized.single().operation)
        assertTrue(harness.unauthorized.single().allowRetry)
        assertEquals(OrdersRoute.Detail("ORD-1001"), harness.holder.state.route)
        assertEquals(DlaFlowUiState.Loading, harness.holder.state.detailState)
        assertEquals(null, harness.holder.state.activeDetailRequestId)
    }

    @Test
    fun `load more refuses overlap before scheduling second request`() {
        val harness = OrdersCoordinatorHarness(
            pageForQuery = { ordersPage(coordinatorOrder(), nextOffset = 20) },
        )
        harness.coordinator.resetList("session-a", OrdersQuery(), showFeedback = false)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertTrue(harness.coordinator.loadMore("session-a", showFeedback = false))
        assertFalse(harness.coordinator.loadMore("session-a", showFeedback = false))
        assertEquals(1, harness.executor.size)
    }

    @Test
    fun `retry after unauthorized ends second unauthorized as terminal error`() {
        val error = MobileApiException(401, "AUTH_REQUIRED", "expired")
        val harness = OrdersCoordinatorHarness(detailForOrder = { throw error })
        val operation = OrdersLoadOperation.Detail("ORD-1001")

        harness.coordinator.loadDetail(
            token = "session-a",
            orderNumber = "ORD-1001",
            showFeedback = false,
            allowUnauthorizedRetry = true,
        )
        harness.executor.runNext()
        harness.mainQueue.runNext()
        harness.coordinator.retry("session-a", operation, showFeedback = false)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(2, harness.unauthorized.size)
        assertTrue(harness.unauthorized.first().allowRetry)
        assertFalse(harness.unauthorized.last().allowRetry)
        val terminal = harness.holder.state.detailState as DlaFlowUiState.Error
        assertFalse(terminal.message.retryable)
        assertEquals(null, harness.holder.state.activeDetailRequestId)
    }

    @Test
    fun `unconfirmed session check ends initial list spinner with retryable error`() {
        val error = MobileApiException(401, "AUTH_REQUIRED", "expired")
        val harness = OrdersCoordinatorHarness(pageForQuery = { throw error })

        harness.coordinator.resetList(
            token = "session-a",
            query = OrdersQuery(search = "current"),
            showFeedback = false,
            allowUnauthorizedRetry = true,
        )
        harness.executor.runNext()
        harness.mainQueue.runNext()
        harness.unauthorized.single().onSessionUnconfirmed()

        val terminal = harness.holder.state.listState as DlaFlowUiState.Error
        assertTrue(terminal.message.retryable)
        assertEquals(OrdersQuery(search = "current"), harness.holder.state.query)
        assertEquals(null, harness.holder.state.activeListRequestId)
    }

    @Test
    fun `late unconfirmed callback cannot overwrite a newer list result`() {
        val error = MobileApiException(401, "AUTH_REQUIRED", "expired")
        var shouldFail = true
        val harness = OrdersCoordinatorHarness(
            pageForQuery = {
                if (shouldFail) throw error
                ordersPage(coordinatorOrder(orderNumber = "NEW"))
            },
        )

        harness.coordinator.resetList("session-a", OrdersQuery(search = "old"), showFeedback = false)
        harness.executor.runNext()
        harness.mainQueue.runNext()
        val lateCallback = harness.unauthorized.single().onSessionUnconfirmed
        shouldFail = false
        harness.coordinator.resetList("session-a", OrdersQuery(search = "new"), showFeedback = false)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        lateCallback()

        assertEquals("NEW", harness.holder.state.listContentOrNull()!!.items.single().orderNumber)
    }

    @Test
    fun `not found detail is a terminal business error`() {
        val harness = OrdersCoordinatorHarness(
            detailForOrder = { throw MobileApiException(404, "ORDER_NOT_FOUND", "missing") },
        )

        harness.coordinator.loadDetail("session-a", "MISSING", showFeedback = true)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        val terminal = harness.holder.state.detailState as DlaFlowUiState.Error
        assertFalse(terminal.message.retryable)
        assertTrue(harness.unauthorized.isEmpty())
        assertEquals(
            listOf(OrdersFeedback.DETAIL_LOADING, OrdersFeedback.LOAD_FAILED),
            harness.feedback,
        )
    }
}

private class OrdersCoordinatorHarness(
    pageForQuery: (OrdersQuery) -> OrdersListContent = { ordersPage(coordinatorOrder()) },
    detailForOrder: (String) -> OrderDetailContent = { orderDetailDto().toOrderDetailContent() },
) {
    val holder = OrdersStateHolder()
    val executor = OrdersQueuedExecutor()
    val mainQueue = OrdersMainQueue()
    val feedback = mutableListOf<OrdersFeedback>()
    val unauthorized = mutableListOf<OrdersUnauthorizedEvent>()
    val coordinator = OrdersCoordinator(
        stateHolder = holder,
        gateway = object : OrdersGateway {
            override fun loadPage(token: String, query: OrdersQuery, offset: Int): OrdersListContent {
                return pageForQuery(query)
            }

            override fun loadDetail(token: String, orderNumber: String): OrderDetailContent =
                detailForOrder(orderNumber)
        },
        executor = executor,
        postToMain = mainQueue::post,
        onFeedback = feedback::add,
        onUnauthorized = { error, operation, allowRetry, onSessionUnconfirmed ->
            unauthorized += OrdersUnauthorizedEvent(error, operation, allowRetry, onSessionUnconfirmed)
        },
    )
}

private data class OrdersUnauthorizedEvent(
    val error: Throwable,
    val operation: OrdersLoadOperation,
    val allowRetry: Boolean,
    val onSessionUnconfirmed: () -> Unit,
)

private fun ordersPage(
    vararg orders: OrdersListItem,
    nextOffset: Int? = null,
) = OrdersListContent(
    items = orders.toList(),
    nextOffset = nextOffset,
    total = orders.size,
)

private fun coordinatorOrder(
    id: String = "order-id-1",
    orderNumber: String = "ORD-1001",
) = OrdersListItem(
    id = id,
    orderNumber = orderNumber,
    amount = 123.45,
    currency = "PLN",
    customer = "Klient testowy",
    channel = "Panel",
    createdAt = "",
    itemCount = 1,
    productSummary = "Produkt testowy",
    paymentStatus = "Opłacone",
    paymentTone = "success",
    phone = "",
    shippingMethod = "Paczkomat",
    status = "new",
    statusTone = "info",
    thumbnailUrl = "",
    badges = OrdersBadges(0, 0, 0),
)

private class OrdersQueuedExecutor : Executor {
    private val tasks = ArrayDeque<Runnable>()
    val size: Int get() = tasks.size
    override fun execute(command: Runnable) {
        tasks.addLast(command)
    }
    fun runNext() = tasks.removeFirst().run()
    fun runLast() = tasks.removeLast().run()
}

private class OrdersMainQueue {
    private val tasks = ArrayDeque<() -> Unit>()
    fun post(task: () -> Unit) {
        tasks.addLast(task)
    }
    fun runNext() = tasks.removeFirst().invoke()
    fun runAll() {
        while (tasks.isNotEmpty()) runNext()
    }
}
