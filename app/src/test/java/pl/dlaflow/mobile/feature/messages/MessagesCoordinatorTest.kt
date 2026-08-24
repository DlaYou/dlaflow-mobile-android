package pl.dlaflow.mobile.feature.messages

import java.net.UnknownHostException
import java.util.ArrayDeque
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class MessagesCoordinatorTest {
    @Test
    fun `list, open detail, read, refresh and reply call gateway`() {
        val harness = Harness()
        harness.gateway.pages += page(item("thread-1"))
        assertTrue(harness.coordinator.open("session-a"))
        harness.runAll()
        assertEquals(1, harness.gateway.listCalls)

        harness.gateway.details += detail()
        assertTrue(harness.coordinator.openThread("session-a", "thread-1"))
        harness.runAll()
        assertEquals(1, harness.gateway.detailCalls)

        assertTrue(harness.coordinator.markThreadRead("session-a"))
        harness.runAll()
        assertEquals(listOf("thread-1"), harness.gateway.readThreads)

        assertTrue(harness.coordinator.refreshThread("session-a"))
        harness.runAll()
        assertEquals(listOf("thread-1"), harness.gateway.refreshThreads)
        assertEquals(2, harness.gateway.detailCalls)

        assertTrue(harness.coordinator.reply("session-a", "thread-1", "Dziękuję", "reply-1"))
        harness.runAll()
        assertEquals(listOf("reply-1"), harness.gateway.replyRequestIds)
    }

    @Test
    fun `stale session callback cannot mutate state`() {
        val harness = Harness()
        harness.gateway.pages += page(item("old"))
        harness.coordinator.open("session-a")
        harness.coordinator.reset()
        harness.runAll()
        assertEquals(DlaFlowUiState.Loading, harness.holder.state.listState)
    }

    @Test
    fun `401 invokes confirmation and retries only after session validation`() {
        val harness = Harness(sessionValid = true)
        harness.gateway.listFailures += MobileApiException(401, "AUTH", "unauthorized")
        harness.gateway.pages += page(item("after"))
        harness.coordinator.open("session-a")
        harness.runAll()

        assertEquals(listOf(true), harness.unauthorizedFlags)
        assertEquals(2, harness.gateway.listCalls)
        assertEquals("after", harness.holder.state.listContentOrNull()!!.items.single().id)
    }

    @Test
    fun `unconfirmed 401 does not clear existing content or retry`() {
        val harness = Harness(sessionValid = false)
        harness.gateway.pages += page(item("existing"))
        harness.coordinator.open("session-a")
        harness.runAll()
        harness.gateway.listFailures += MobileApiException(401, "AUTH", "unauthorized")
        harness.coordinator.refresh("session-a")
        harness.runAll()

        assertEquals(2, harness.gateway.listCalls)
        assertEquals("existing", harness.holder.state.listContentOrNull()!!.items.single().id)
    }

    @Test
    fun `offline and forbidden retain or remove state according to failure`() {
        val offline = Harness()
        offline.gateway.pages += page(item("existing"))
        offline.coordinator.open("session-a")
        offline.runAll()
        offline.gateway.listFailures += UnknownHostException()
        offline.coordinator.refresh("session-a")
        offline.runAll()
        assertTrue(offline.holder.state.listState is DlaFlowUiState.Offline<*>)

        val forbidden = Harness()
        forbidden.gateway.listFailures += MobileApiException(403, "FORBIDDEN", "forbidden")
        forbidden.coordinator.open("session-a")
        forbidden.runAll()
        assertEquals(DlaFlowUiState.NoAccess, forbidden.holder.state.listState)
    }

    @Test
    fun `retryable reply failure keeps the optimistic bubble as failed`() {
        val harness = Harness()
        harness.gateway.details += detail()
        assertTrue(harness.coordinator.openThread("session-a", "thread-1"))
        harness.runAll()
        harness.gateway.mutationFailures += IllegalStateException("temporary")

        assertTrue(harness.coordinator.reply("session-a", "thread-1", "Dziękuję", "reply-failed"))
        harness.runAll()

        val bubble = harness.holder.state.detailContentOrNull()!!.messages.single()
        assertEquals("reply-failed", bubble.requestId)
        assertEquals("failed", bubble.status)
        assertTrue(harness.holder.state.transientMessage?.retryable == true)
    }

    @Test
    fun `offline read failure preserves detail content and releases mutation`() {
        val harness = Harness()
        harness.gateway.details += detail()
        assertTrue(harness.coordinator.openThread("session-a", "thread-1"))
        harness.runAll()
        harness.gateway.mutationFailures += UnknownHostException()

        assertTrue(harness.coordinator.markThreadRead("session-a"))
        harness.runAll()

        assertTrue(harness.holder.state.detailState is DlaFlowUiState.Offline<*>)
        assertEquals("thread-1", harness.holder.state.detailContentOrNull()!!.id)
        assertFalse(harness.holder.state.isMarkingRead)
    }

    private class Harness(private val sessionValid: Boolean = true) {
        val holder = MessagesStateHolder()
        val gateway = FakeGateway()
        private val work = ArrayDeque<() -> Unit>()
        private val main = ArrayDeque<() -> Unit>()
        val unauthorizedFlags = mutableListOf<Boolean>()
        val coordinator = MessagesCoordinator(
            stateHolder = holder,
            gateway = gateway,
            executor = Executor { command -> work.addLast { command.run() } },
            postToMain = { main.addLast(it) },
            onUnauthorized = { _, _, allowRetry, onSessionValid, onSessionUnconfirmed ->
                unauthorizedFlags += allowRetry
                if (sessionValid) onSessionValid() else onSessionUnconfirmed()
            },
        )

        fun runAll() {
            while (work.isNotEmpty() || main.isNotEmpty()) {
                while (work.isNotEmpty()) work.removeFirst().invoke()
                while (main.isNotEmpty()) main.removeFirst().invoke()
            }
        }
    }

    private class FakeGateway : MessagesGateway {
        val pages = ArrayDeque<MessagesContent>()
        val details = ArrayDeque<MessageThreadDetail>()
        val listFailures = ArrayDeque<Throwable>()
        val detailFailures = ArrayDeque<Throwable>()
        val mutationFailures = ArrayDeque<Throwable>()
        val readThreads = mutableListOf<String>()
        val refreshThreads = mutableListOf<String>()
        val replyRequestIds = mutableListOf<String>()
        var listCalls = 0
        var detailCalls = 0

        override fun loadPage(token: String, query: MessagesQuery, cursor: String?): MessagesContent {
            listCalls += 1
            if (listFailures.isNotEmpty()) throw listFailures.removeFirst()
            return pages.removeFirst()
        }

        override fun loadDetail(token: String, threadId: String, cursor: String?): MessageThreadDetail {
            detailCalls += 1
            if (detailFailures.isNotEmpty()) throw detailFailures.removeFirst()
            return details.removeFirst()
        }

        override fun markRead(token: String, threadId: String): MessageOperation {
            if (mutationFailures.isNotEmpty()) throw mutationFailures.removeFirst()
            readThreads += threadId
            return MessageOperation("op-read", null, queued = false, duplicate = false, status = "accepted")
        }

        override fun refreshThread(token: String, threadId: String): MessageOperation {
            if (mutationFailures.isNotEmpty()) throw mutationFailures.removeFirst()
            refreshThreads += threadId
            return MessageOperation("op-refresh", null, queued = true, duplicate = false, status = "queued")
        }

        override fun reply(token: String, threadId: String, body: String, requestId: String): MessageOperation {
            if (mutationFailures.isNotEmpty()) throw mutationFailures.removeFirst()
            replyRequestIds += requestId
            return MessageOperation("op-reply", "message-1", queued = true, duplicate = false, status = "queued")
        }
    }

    private fun page(vararg items: MessageListItem) = MessagesContent(items.toList(), items.size, null, items.count(MessageListItem::isUnread))

    private fun item(id: String) = MessageListItem(
        id = id, providerId = "allegro", integrationId = "integration", providerLabel = "Allegro",
        customerName = "Anna", customerLogin = "anna", subject = "Temat", preview = null, lastMessageAt = "",
        messageCount = 1, orderId = null, orderNumber = null, readAt = null, status = "unread", channel = MessagesChannel.ALL,
    )

    private fun detail() = MessageThreadDetail(
        id = "thread-1", providerId = "allegro", integrationId = "integration", providerLabel = "Allegro",
        customerName = "Anna", customerLogin = "anna", customerEmail = null, subject = "Temat", lastMessageAt = "",
        readAt = null, status = "unread", orderId = null, orderNumber = null, messages = emptyList(), nextCursor = null,
        customerContext = null,
    )
}
