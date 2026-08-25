package pl.dlaflow.mobile.feature.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class MessagesContractTest {
    @Test
    fun `unread and channel filters are applied to canonical content`() {
        val items = listOf(
            item("unread-market", unread = true, channel = MessagesChannel.MARKETPLACE),
            item("read-market", unread = false, channel = MessagesChannel.MARKETPLACE),
            item("unread-email", unread = true, channel = MessagesChannel.EMAIL),
        )
        val state = MessagesUiState(
            query = MessagesQuery(filter = MessagesFilter.UNREAD, channel = MessagesChannel.MARKETPLACE),
            listState = DlaFlowUiState.Content(MessagesContent(items, 3, null, 2)),
        )

        assertEquals(listOf("unread-market"), state.visibleItems().map(MessageListItem::id))
    }

    @Test
    fun `read state requires both unread status and absent read timestamp`() {
        assertTrue(item("a", unread = true).isUnread)
        assertFalse(item("b", unread = false).isUnread)
    }

    @Test
    fun `new status is distinct from a regular unread status`() {
        assertTrue(item("new", unread = true, status = "new").isNew)
        assertFalse(item("unread", unread = true, status = "unread").isNew)
        assertFalse(item("read", unread = false, status = "new").isNew)
    }

    @Test
    fun `filter counts use server totals and stay bounded`() {
        val content = MessagesContent(
            items = emptyList(),
            total = 23,
            nextCursor = null,
            unreadCount = 6,
        )

        assertEquals(23, content.countFor(MessagesFilter.ALL))
        assertEquals(6, content.countFor(MessagesFilter.UNREAD))
        assertEquals(0, MessagesContent(emptyList(), -1, null, -2).countFor(MessagesFilter.ALL))
    }

    @Test
    fun `detail route retains opaque thread id and defaults to list`() {
        assertEquals(MessagesRoute.List, MessagesUiState().route)
        assertEquals("thread/one", MessagesRoute.Detail("thread/one").threadId)
    }

    private fun item(
        id: String,
        unread: Boolean,
        channel: MessagesChannel = MessagesChannel.ALL,
        status: String = if (unread) "unread" else "read",
    ) = MessageListItem(
        id = id,
        providerId = "allegro",
        integrationId = "integration",
        providerLabel = "Allegro",
        customerName = "Anna",
        customerLogin = "anna",
        subject = "Temat",
        preview = null,
        lastMessageAt = "",
        messageCount = 1,
        orderId = null,
        orderNumber = null,
        readAt = if (unread) null else "2026-08-24T11:00:00Z",
        status = status,
        channel = channel,
    )
}
