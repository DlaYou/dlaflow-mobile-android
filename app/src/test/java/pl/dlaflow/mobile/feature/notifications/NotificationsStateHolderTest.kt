package pl.dlaflow.mobile.feature.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class NotificationsStateHolderTest {
    private val retryable = DlaFlowUiMessage(1, 2, retryable = true)

    @Test
    fun `dashboard preview is presentation only and canonical empty replaces it terminally`() {
        val holder = NotificationsStateHolder()
        holder.setDashboardPreview(listOf(item("preview")))

        assertEquals(listOf("preview"), holder.state.visibleItems().map(NotificationItem::id))
        assertNull(holder.beginMarkVisibleRead("session"))

        val request = requireNotNull(holder.beginLoad("session"))
        assertTrue(holder.acceptLoadSuccess(request, content()))
        assertTrue(holder.state.notificationsState is DlaFlowUiState.Empty)
        assertTrue(holder.state.dashboardPreview.isEmpty())

        holder.setDashboardPreview(listOf(item("late-preview")))
        assertTrue(holder.state.visibleItems().isEmpty())
    }

    @Test
    fun `mark visible derives exact canonical unread ids for current filter and caps them at twenty`() {
        val holder = NotificationsStateHolder()
        val rows = (1..24).map { index ->
            item(
                id = "id-$index",
                tone = if (index % 2 == 0) NotificationTone.Attention else NotificationTone.Info,
                readAt = if (index == 2) "2026-07-18T10:00:00.000Z" else null,
            )
        }
        val load = requireNotNull(holder.beginLoad("session"))
        holder.acceptLoadSuccess(load, content(rows))
        holder.selectFilter(NotificationFilter.ATTENTION)

        val mutation = requireNotNull(holder.beginMarkVisibleRead("session"))

        assertEquals(11, mutation.notificationIds.size)
        assertFalse("id-2" in mutation.notificationIds)
        assertTrue(mutation.notificationIds.all { it.substringAfter('-').toInt() % 2 == 0 })
        assertNull(holder.beginMarkVisibleRead("session"))
    }

    @Test
    fun `mark all visible caps request at endpoint limit`() {
        val holder = loadedHolder((1..30).map { item("id-$it") })

        val request = requireNotNull(holder.beginMarkVisibleRead("session"))

        assertEquals((1..20).map { "id-$it" }, request.notificationIds)
    }

    @Test
    fun `stale load and mutation callbacks are complete no ops`() {
        val holder = NotificationsStateHolder()
        val firstLoad = requireNotNull(holder.beginLoad("session-a"))
        val secondLoad = requireNotNull(holder.beginLoad("session-b"))
        val beforeStaleLoad = holder.state

        assertFalse(holder.acceptLoadSuccess(firstLoad, content(listOf(item("stale")))))
        assertEquals(beforeStaleLoad, holder.state)
        assertTrue(holder.acceptLoadSuccess(secondLoad, content(listOf(item("current")))))

        val mutation = requireNotNull(holder.beginMarkVisibleRead("session-b"))
        holder.reset()
        val beforeStaleMutation = holder.state
        assertFalse(holder.acceptMutationSuccess(mutation))
        assertEquals(beforeStaleMutation, holder.state)

        val afterResetLoad = requireNotNull(holder.beginLoad("session-c"))
        assertTrue(afterResetLoad.requestId > secondLoad.requestId)
        val afterResetMutationHolder = loadedHolder(listOf(item("fresh")))
        val freshMutation = requireNotNull(afterResetMutationHolder.beginMarkVisibleRead("session-c"))
        assertTrue(freshMutation.requestId > 0L)
    }

    @Test
    fun `load callback with current id but different session is a complete no op`() {
        val holder = NotificationsStateHolder()
        val current = requireNotNull(holder.beginLoad("session-a"))
        val wrongSession = NotificationsLoadRequest(
            requestId = current.requestId,
            sessionKey = "session-b",
        )
        val beforeWrongSession = holder.state

        assertFalse(holder.acceptLoadSuccess(wrongSession, content(listOf(item("wrong")))))
        assertEquals(beforeWrongSession, holder.state)
        assertTrue(holder.acceptLoadSuccess(current, content(listOf(item("current")))))
        assertEquals("current", holder.state.canonicalContentOrNull()?.items?.single()?.id)
    }

    @Test
    fun `mutation callback with current id but different session is a complete no op`() {
        val holder = loadedHolder(listOf(item("canonical")))
        val current = requireNotNull(holder.beginMarkVisibleRead("session-a"))
        val wrongSession = NotificationsMutationRequest(
            requestId = current.requestId,
            sessionKey = "session-b",
            notificationIds = current.notificationIds,
        )
        val beforeWrongSession = holder.state

        assertFalse(holder.acceptMutationSuccess(wrongSession))
        assertEquals(beforeWrongSession, holder.state)
        assertTrue(holder.acceptMutationSuccess(current))
        assertFalse(holder.state.isMarkingRead)
    }

    @Test
    fun `offline and retryable failures retain canonical content while no access removes it`() {
        val holder = loadedHolder(listOf(item("canonical")))
        val offlineRequest = requireNotNull(holder.beginLoad("session"))

        assertTrue(holder.acceptLoadOffline(offlineRequest, retryable))
        val offline = holder.state.notificationsState as DlaFlowUiState.Offline
        assertEquals("canonical", offline.lastContent?.items?.single()?.id)
        assertEquals(retryable, holder.state.transientMessage)

        val retryRequest = requireNotNull(holder.beginLoad("session"))
        assertTrue(holder.acceptLoadFailure(retryRequest, retryable))
        assertEquals("canonical", holder.state.canonicalContentOrNull()?.items?.single()?.id)

        val forbiddenRequest = requireNotNull(holder.beginLoad("session"))
        assertTrue(holder.acceptLoadNoAccess(forbiddenRequest))
        assertTrue(holder.state.notificationsState is DlaFlowUiState.NoAccess)
        assertTrue(holder.state.dashboardPreview.isEmpty())
        assertNull(holder.beginMarkVisibleRead("session"))
    }

    @Test
    fun `recoverable read failure retains canonical content and releases mutation loader`() {
        val holder = loadedHolder(listOf(item("canonical")))
        val mutation = requireNotNull(holder.beginMarkVisibleRead("session"))

        assertTrue(holder.acceptMutationOffline(mutation, retryable))

        val offline = holder.state.notificationsState as DlaFlowUiState.Offline
        assertEquals("canonical", offline.lastContent?.items?.single()?.id)
        assertFalse(holder.state.isMarkingRead)
        assertNull(holder.state.activeMutationRequestId)
        assertEquals(retryable, holder.state.transientMessage)
    }

    @Test
    fun `unknown item action emits only safe explanation`() {
        val holder = loadedHolder(
            listOf(item("unknown", destination = NotificationDestination.Unsupported)),
        )

        assertEquals(
            NotificationsEffect.ShowSafeExplanation(NotificationDestination.Unsupported),
            holder.effectFor("unknown"),
        )
        assertNull(holder.effectFor("missing"))
    }

    private fun loadedHolder(items: List<NotificationItem>): NotificationsStateHolder =
        NotificationsStateHolder().also { holder ->
            val request = requireNotNull(holder.beginLoad("session"))
            assertTrue(holder.acceptLoadSuccess(request, content(items)))
        }

    private fun content(items: List<NotificationItem> = emptyList()) = NotificationsContent(
        items = items,
        attentionCount = items.count { it.tone == NotificationTone.Attention },
        unreadAttentionCount = items.count { it.tone == NotificationTone.Attention && it.isUnread },
        unreadCount = items.count(NotificationItem::isUnread),
    )

    private fun item(
        id: String,
        tone: NotificationTone = NotificationTone.Info,
        readAt: String? = null,
        destination: NotificationDestination = NotificationDestination.Orders,
    ) = NotificationItem(
        id = id,
        title = "Tytuł",
        description = "Opis",
        tone = tone,
        source = "Panel",
        account = "Sklep",
        occurredAt = "2026-07-18T10:00:00.000Z",
        readAt = readAt,
        actionLabel = "Otwórz",
        destination = destination,
    )
}
