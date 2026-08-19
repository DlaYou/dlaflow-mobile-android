package pl.dlaflow.mobile.feature.notifications

import java.net.UnknownHostException
import java.util.ArrayDeque
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class NotificationsCoordinatorTest {
    @Test
    fun `opening loads canonical inbox and never marks preview read`() {
        val harness = Harness()
        harness.gateway.pages += content(listOf(item("canonical")))

        assertTrue(harness.coordinator.open("session", listOf(item("preview"))))
        assertEquals(0, harness.gateway.markedIds.size)
        harness.runAll()

        assertEquals(listOf("canonical"), harness.holder.state.canonicalContentOrNull()?.items?.map(NotificationItem::id))
        assertEquals(0, harness.gateway.markedIds.size)
    }

    @Test
    fun `accepted read success updates summary and schedules canonical reload`() {
        val harness = Harness()
        harness.gateway.pages += content(listOf(item("unread")))
        harness.gateway.pages += content(listOf(item("unread", readAt = "2026-07-18T11:00:00.000Z")))
        harness.coordinator.open("session", emptyList())
        harness.runAll()

        assertTrue(harness.coordinator.markVisibleRead("session"))
        harness.runAll()

        assertEquals(listOf(listOf("unread")), harness.gateway.markedIds)
        assertEquals(1, harness.readSummaryUpdates)
        assertFalse(harness.holder.state.canonicalContentOrNull()?.items?.single()?.isUnread ?: true)
    }

    @Test
    fun `stale read success causes no summary update and no reload`() {
        val harness = Harness()
        harness.gateway.pages += content(listOf(item("unread")))
        harness.coordinator.open("session", emptyList())
        harness.runAll()
        assertTrue(harness.coordinator.markVisibleRead("session"))
        harness.holder.reset()

        harness.runAll()

        assertEquals(0, harness.readSummaryUpdates)
        assertEquals(1, harness.gateway.loadCalls)
    }

    @Test
    fun `load failures preserve offline distinction and fail closed on forbidden`() {
        val offline = Harness().also { it.gateway.loadFailure = UnknownHostException() }
        offline.coordinator.open("session", listOf(item("preview")))
        offline.runAll()
        assertTrue(offline.holder.state.notificationsState is DlaFlowUiState.Offline)
        assertTrue(offline.holder.state.dashboardPreview.isEmpty())

        val forbidden = Harness().also { it.gateway.loadFailure = apiError(403) }
        forbidden.coordinator.open("session", listOf(item("preview")))
        forbidden.runAll()
        assertTrue(forbidden.holder.state.notificationsState is DlaFlowUiState.NoAccess)
        assertFalse(forbidden.coordinator.markVisibleRead("session"))
    }

    @Test
    fun `401 confirms session once and retries through a new current request`() {
        val harness = Harness(sessionValid = true)
        harness.gateway.loadFailures += apiError(401)
        harness.gateway.pages += content(listOf(item("after-retry")))

        harness.coordinator.open("session", listOf(item("preview")))
        harness.runAll()

        assertEquals(listOf(true), harness.unauthorizedRetryFlags)
        assertEquals(2, harness.gateway.loadCalls)
        assertEquals("after-retry", harness.holder.state.canonicalContentOrNull()?.items?.single()?.id)
        assertTrue(harness.holder.state.dashboardPreview.isEmpty())
    }

    @Test
    fun `unconfirmed session terminally removes preview without retrying canonical request`() {
        val harness = Harness(sessionValid = false)
        harness.gateway.loadFailures += apiError(401)

        harness.coordinator.open("session", listOf(item("preview")))
        harness.runAll()

        assertEquals(1, harness.gateway.loadCalls)
        assertTrue(harness.holder.state.notificationsState is DlaFlowUiState.Error)
        assertTrue(harness.holder.state.dashboardPreview.isEmpty())
    }

    @Test
    fun `offline and retryable read failures retain canonical content without side effects`() {
        listOf<Throwable>(UnknownHostException(), IllegalStateException("temporary"))
            .forEach { failure ->
                val harness = Harness()
                harness.gateway.pages += content(listOf(item("canonical")))
                harness.coordinator.open("session", emptyList())
                harness.runAll()
                harness.gateway.markFailure = failure

                assertTrue(harness.coordinator.markVisibleRead("session"))
                harness.runAll()

                assertEquals("canonical", harness.holder.state.canonicalContentOrNull()?.items?.single()?.id)
                assertFalse(harness.holder.state.isMarkingRead)
                assertTrue(harness.holder.state.transientMessage?.retryable == true)
                assertEquals(0, harness.readSummaryUpdates)
                assertEquals(1, harness.gateway.loadCalls)
                assertTrue(harness.gateway.markedIds.isEmpty())
            }
    }

    @Test
    fun `forbidden read failure removes canonical content and releases mutation loader`() {
        val harness = Harness()
        harness.gateway.pages += content(listOf(item("canonical")))
        harness.coordinator.open("session", emptyList())
        harness.runAll()
        harness.gateway.markFailure = apiError(403)

        assertTrue(harness.coordinator.markVisibleRead("session"))
        harness.runAll()

        assertTrue(harness.holder.state.notificationsState is DlaFlowUiState.NoAccess)
        assertFalse(harness.holder.state.isMarkingRead)
        assertEquals(0, harness.readSummaryUpdates)
        assertEquals(1, harness.gateway.loadCalls)
        assertTrue(harness.gateway.markedIds.isEmpty())
    }

    @Test
    fun `401 read failure confirms session retries once and reloads canonical content`() {
        val harness = Harness(sessionValid = true)
        harness.gateway.pages += content(listOf(item("unread")))
        harness.gateway.pages += content(listOf(item("unread", readAt = "2026-07-18T11:00:00.000Z")))
        harness.coordinator.open("session", emptyList())
        harness.runAll()
        harness.gateway.markFailures += apiError(401)

        assertTrue(harness.coordinator.markVisibleRead("session"))
        harness.runAll()

        assertEquals(listOf(true), harness.unauthorizedRetryFlags)
        assertEquals(listOf(listOf("unread")), harness.gateway.markedIds)
        assertEquals(1, harness.readSummaryUpdates)
        assertEquals(2, harness.gateway.loadCalls)
        assertFalse(harness.holder.state.canonicalContentOrNull()?.items?.single()?.isUnread ?: true)
    }

    @Test
    fun `unconfirmed 401 read failure releases loader without summary update or reload`() {
        val harness = Harness(sessionValid = false)
        harness.gateway.pages += content(listOf(item("unread")))
        harness.coordinator.open("session", emptyList())
        harness.runAll()
        harness.gateway.markFailures += apiError(401)

        assertTrue(harness.coordinator.markVisibleRead("session"))
        harness.runAll()

        assertEquals(listOf(true), harness.unauthorizedRetryFlags)
        assertFalse(harness.holder.state.isMarkingRead)
        assertTrue(harness.holder.state.transientMessage != null)
        assertEquals(0, harness.readSummaryUpdates)
        assertEquals(1, harness.gateway.loadCalls)
        assertTrue(harness.gateway.markedIds.isEmpty())
    }

    private class Harness(
        private val sessionValid: Boolean = true,
    ) {
        val holder = NotificationsStateHolder()
        val gateway = FakeGateway()
        private val work = ArrayDeque<() -> Unit>()
        private val main = ArrayDeque<() -> Unit>()
        var readSummaryUpdates = 0
        val unauthorizedRetryFlags = mutableListOf<Boolean>()
        val coordinator = NotificationsCoordinator(
            stateHolder = holder,
            gateway = gateway,
            executor = Executor { command -> work.addLast { command.run() } },
            postToMain = { main.addLast(it) },
            onEffect = {},
            onReadStateChanged = { readSummaryUpdates += 1 },
            onUnauthorized = { _, allowRetry, onSessionValid, onSessionUnconfirmed ->
                unauthorizedRetryFlags += allowRetry
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

    private class FakeGateway : NotificationsGateway {
        val pages = ArrayDeque<NotificationsContent>()
        val markedIds = mutableListOf<List<String>>()
        val loadFailures = ArrayDeque<Throwable>()
        val markFailures = ArrayDeque<Throwable>()
        var loadFailure: Throwable? = null
        var markFailure: Throwable? = null
        var loadCalls = 0

        override fun load(token: String): NotificationsContent {
            loadCalls += 1
            if (loadFailures.isNotEmpty()) throw loadFailures.removeFirst()
            loadFailure?.let { throw it }
            return pages.removeFirst()
        }

        override fun markRead(token: String, notificationIds: List<String>) {
            if (markFailures.isNotEmpty()) throw markFailures.removeFirst()
            markFailure?.let { throw it }
            markedIds += notificationIds.toList()
        }
    }

    companion object {
        private fun content(items: List<NotificationItem>) = NotificationsContent(
            items = items,
            attentionCount = items.count { it.tone == NotificationTone.Attention },
            unreadAttentionCount = items.count { it.tone == NotificationTone.Attention && it.isUnread },
            unreadCount = items.count(NotificationItem::isUnread),
        )

        private fun item(id: String, readAt: String? = null) = NotificationItem(
            id = id,
            title = "Tytuł",
            description = "Opis",
            tone = NotificationTone.Info,
            source = "Panel",
            account = "Sklep",
            occurredAt = "2026-07-18T10:00:00.000Z",
            readAt = readAt,
            actionLabel = "Otwórz",
            destination = NotificationDestination.Orders,
        )

        private fun apiError(status: Int) = MobileApiException(status, "TEST", "raw")
    }
}
