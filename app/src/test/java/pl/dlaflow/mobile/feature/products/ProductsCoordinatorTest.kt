package pl.dlaflow.mobile.feature.products

import java.util.ArrayDeque
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class ProductsCoordinatorTest {
    @Test
    fun `list result reaches state only through main queue`() {
        val harness = ProductsHarness()
        harness.coordinator.resetList("session-a", ProductsQuery(), showFeedback = true)

        harness.executor.runNext()
        assertTrue(harness.holder.state.listState is DlaFlowUiState.Loading)
        harness.mainQueue.runNext()

        assertEquals("p1", harness.holder.state.listContentOrNull()?.items?.single()?.id)
        assertEquals(listOf(ProductsFeedback.LIST_LOADING, ProductsFeedback.LIST_READY), harness.feedback)
    }

    @Test
    fun `first list 401 confirms and retries exactly once while second is terminal`() {
        val unauthorized = MobileApiException(401, "AUTH_REQUIRED", "private")
        var attempts = 0
        val harness = ProductsHarness(pageLoader = { _, _, _ ->
            attempts += 1
            throw unauthorized
        })
        harness.coordinator.resetList("session-a", ProductsQuery(), showFeedback = false)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(1, harness.authorizations.size)
        assertSame(unauthorized, harness.authorizations.single().error)
        harness.authorizations.single().onSessionValid()
        harness.authorizations.single().onSessionValid()
        assertEquals(1, harness.executor.size)

        harness.executor.runNext()
        harness.mainQueue.runNext()
        assertEquals(2, attempts)
        assertEquals(1, harness.authorizations.size)
        assertTrue(harness.holder.state.listState is DlaFlowUiState.Error)
    }

    @Test
    fun `late 401 confirmation cannot retry after newer list reset`() {
        val unauthorized = MobileApiException(401, "AUTH_REQUIRED", "private")
        var fail = true
        val harness = ProductsHarness(pageLoader = { _, query, _ ->
            if (fail) throw unauthorized
            productsContent(query.search)
        })
        harness.coordinator.resetList("session-a", ProductsQuery(search = "old"), showFeedback = false)
        harness.executor.runNext()
        harness.mainQueue.runNext()
        val stale = harness.authorizations.single().onSessionValid

        fail = false
        harness.coordinator.resetList("session-a", ProductsQuery(search = "new"), showFeedback = false)
        harness.executor.runNext()
        harness.mainQueue.runNext()
        stale()

        assertEquals(0, harness.executor.size)
        assertEquals("new", harness.holder.state.listContentOrNull()?.items?.single()?.id)
    }

    @Test
    fun `variants and quick edit use independent request lanes`() {
        val harness = ProductsHarness()
        harness.coordinator.resetList("session-a", ProductsQuery(), showFeedback = false)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertTrue(harness.coordinator.toggleVariants("session-a", "p1", showFeedback = false))
        assertFalse(
            harness.coordinator.quickEditProduct(
                "session-a",
                "p1",
                ProductQuickEditField.STOCK,
                8.0,
                showFeedback = false,
            ),
        )
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals("v1", (harness.holder.state.variants["p1"] as DlaFlowUiState.Content).data.single().id)
    }

    @Test
    fun `typed actions own query variants and quick edit flow`() {
        val harness = ProductsHarness(pageLoader = { _, query, _ ->
            ProductsContent(
                listOf(productItem(query.search.ifBlank { "p1" }, variantCount = 0)),
                total = 1,
                nextCursor = null,
                canEdit = true,
            )
        })
        assertTrue(harness.coordinator.handleAction("session-a", ProductsAction.SearchChanged(" p2 "), false))
        assertEquals("p2", harness.holder.state.query.search)
        harness.searchScheduler.runLast()
        harness.mainQueue.runNext()
        harness.executor.runNext()
        harness.mainQueue.runNext()
        val target = ProductQuickEditTarget.Product("p2", ProductQuickEditField.GROSS_PRICE)

        assertTrue(harness.coordinator.handleAction("session-a", ProductsAction.OpenQuickEdit(target), false))
        assertTrue(harness.coordinator.handleAction("session-a", ProductsAction.SaveQuickEdit(25.0), false))
        assertFalse(harness.coordinator.handleAction("session-a", ProductsAction.SaveQuickEdit(26.0), false))
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(25.0, harness.holder.state.listContentOrNull()?.items?.single()?.grossPrice)
    }

    @Test
    fun `search debounce coalesces input and stale scheduled reset cannot run`() {
        val harness = ProductsHarness(pageLoader = { _, query, _ -> productsContent(query.search) })

        harness.coordinator.handleAction("session-a", ProductsAction.SearchChanged("a"), false)
        harness.coordinator.handleAction("session-a", ProductsAction.SearchChanged("ab"), false)

        assertEquals("ab", harness.holder.state.query.search)
        assertEquals(0, harness.executor.size)
        harness.searchScheduler.runAll()
        harness.mainQueue.runNext()
        assertEquals(1, harness.executor.size)
        harness.executor.runNext()
        harness.mainQueue.runNext()
        assertEquals("ab", harness.holder.state.listContentOrNull()?.items?.single()?.id)

        harness.coordinator.handleAction("session-a", ProductsAction.SearchChanged("stale"), false)
        harness.coordinator.reset()
        harness.searchScheduler.runAll()
        assertEquals(0, harness.executor.size)
    }

    @Test
    fun `variants 401 retries once only after current confirmation`() {
        val unauthorized = MobileApiException(401, "AUTH_REQUIRED", "private")
        var attempts = 0
        val harness = ProductsHarness(variantsLoader = { _, productId ->
            attempts += 1
            if (attempts == 1) throw unauthorized
            listOf(variantItem("v1", productId))
        })
        harness.coordinator.resetList("session-a", ProductsQuery(), false)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        harness.coordinator.toggleVariants("session-a", "p1", false)
        harness.executor.runNext()
        harness.mainQueue.runNext()
        harness.authorizations.single().onSessionValid()
        harness.authorizations.single().onSessionValid()
        assertEquals(1, harness.executor.size)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(2, attempts)
        assertEquals("v1", (harness.holder.state.variants["p1"] as DlaFlowUiState.Content).data.single().id)
    }

    @Test
    fun `quick edit 401 retry is exactly once and keeps the mutation target`() {
        val unauthorized = MobileApiException(401, "AUTH_REQUIRED", "private")
        var attempts = 0
        val harness = ProductsHarness(
            pageLoader = { _, _, _ ->
                ProductsContent(listOf(productItem("p1", variantCount = 0)), 1, null, true)
            },
            productEditor = { _, productId, _, value ->
                attempts += 1
                if (attempts == 1) throw unauthorized
                productItem(productId, grossPrice = value, variantCount = 0)
            },
        )
        harness.coordinator.resetList("session-a", ProductsQuery(), false)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertTrue(harness.coordinator.quickEditProduct("session-a", "p1", ProductQuickEditField.GROSS_PRICE, 31.0, false))
        harness.executor.runNext()
        harness.mainQueue.runNext()
        harness.authorizations.single().onSessionValid()
        harness.authorizations.single().onSessionValid()
        assertEquals(1, harness.executor.size)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(2, attempts)
        assertEquals(31.0, harness.holder.state.listContentOrNull()?.items?.single()?.grossPrice)
        assertEquals(null, harness.holder.state.quickEdit)
    }
}

private class ProductsHarness(
    pageLoader: (String, ProductsQuery, String?) -> ProductsContent = { _, _, _ -> productsContent("p1") },
    variantsLoader: (String, String) -> List<ProductVariant> = { _, productId -> listOf(variantItem("v1", productId)) },
    productEditor: (String, String, ProductQuickEditField, Double) -> ProductItem = { _, productId, _, value ->
        productItem(productId, grossPrice = value, variantCount = 0)
    },
) {
    val holder = ProductsStateHolder()
    val executor = ProductsQueuedExecutor()
    val mainQueue = ProductsMainQueue()
    val searchScheduler = ProductsManualSearchScheduler()
    val feedback = mutableListOf<ProductsFeedback>()
    val authorizations = mutableListOf<ProductsUnauthorizedEvent>()
    val coordinator = ProductsCoordinator(
        stateHolder = holder,
        gateway = object : ProductsGateway {
            override fun loadPage(token: String, query: ProductsQuery, cursor: String?) = pageLoader(token, query, cursor)
            override fun loadVariants(token: String, productId: String, parentThumbnailUrl: String) = variantsLoader(token, productId)
            override fun quickEditProduct(token: String, productId: String, field: ProductQuickEditField, value: Double) =
                productEditor(token, productId, field, value)
            override fun quickEditVariant(
                token: String,
                productId: String,
                variantId: String,
                field: VariantQuickEditField,
                value: Double,
                parentThumbnailUrl: String,
            ) =
                variantItem(variantId, productId)
        },
        executor = executor,
        postToMain = mainQueue::post,
        searchScheduler = searchScheduler,
        onFeedback = feedback::add,
        onUnauthorized = { error, operation, allowRetry, onSessionValid, onSessionUnconfirmed ->
            authorizations += ProductsUnauthorizedEvent(error, operation, allowRetry, onSessionValid, onSessionUnconfirmed)
        },
    )
}

private data class ProductsUnauthorizedEvent(
    val error: Throwable,
    val operation: ProductsOperation,
    val allowRetry: Boolean,
    val onSessionValid: () -> Unit,
    val onSessionUnconfirmed: () -> Unit,
)

private class ProductsQueuedExecutor : Executor {
    private val tasks = ArrayDeque<Runnable>()
    val size: Int get() = tasks.size
    override fun execute(command: Runnable) { tasks.addLast(command) }
    fun runNext() = tasks.removeFirst().run()
}

private class ProductsMainQueue {
    private val tasks = ArrayDeque<() -> Unit>()
    fun post(task: () -> Unit) { tasks.addLast(task) }
    fun runNext() = tasks.removeFirst().invoke()
}

private class ProductsManualSearchScheduler : ProductsSearchScheduler {
    private data class Pending(var cancelled: Boolean, val action: () -> Unit)
    private val pending = mutableListOf<Pending>()

    override fun schedule(delayMillis: Long, action: () -> Unit): ProductsScheduledTask {
        assertEquals(300L, delayMillis)
        val item = Pending(cancelled = false, action = action)
        pending += item
        return ProductsScheduledTask { item.cancelled = true }
    }

    fun runLast() {
        val item = pending.removeAt(pending.lastIndex)
        if (!item.cancelled) item.action()
    }

    fun runAll() {
        val copy = pending.toList()
        pending.clear()
        copy.filterNot { it.cancelled }.forEach { it.action() }
    }
}
