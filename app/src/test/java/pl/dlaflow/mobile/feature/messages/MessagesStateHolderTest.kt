package pl.dlaflow.mobile.feature.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class MessagesStateHolderTest {
    private val message = DlaFlowUiMessage(1, 2, retryable = true)

    @Test
    fun `stale request and session callbacks do not replace current list`() {
        val holder = MessagesStateHolder()
        val stale = holder.beginListReset("session-a", MessagesQuery())
        val current = holder.beginListReset("session-a", MessagesQuery(search = "new"))
        val before = holder.state

        assertFalse(holder.acceptListSuccess(stale, content(item("old"))))
        assertFalse(holder.acceptListFailure(current.copy(sessionKey = "session-b"), message))
        assertEquals(before, holder.state)
        assertTrue(holder.acceptListSuccess(current, content(item("new"))))
        assertEquals(listOf("new"), holder.state.listContentOrNull()!!.items.map(MessageListItem::id))
    }

    @Test
    fun `refresh preserves content while offline and retryable failures`() {
        val holder = holderWithContent()
        val previous = holder.state.listContentOrNull()!!
        val refresh = holder.beginListRefresh("session-a")

        assertEquals(previous, holder.state.listContentOrNull())
        assertTrue(holder.state.isRefreshing)
        assertTrue(holder.acceptListOffline(refresh, message))
        assertEquals(DlaFlowUiState.Offline(previous), holder.state.listState)

        val retry = holder.beginListRefresh("session-a")
        assertTrue(holder.acceptListFailure(retry, message))
        assertEquals(previous, holder.state.listContentOrNull())
        assertFalse(holder.state.isRefreshing)
    }

    @Test
    fun `list cursor pagination merges by thread id and guards duplicate cursor`() {
        val holder = MessagesStateHolder()
        val initial = holder.beginListReset("session-a", MessagesQuery())
        holder.acceptListSuccess(initial, content(item("one"), nextCursor = "next"))

        val more = holder.beginLoadMore("session-a")
        assertTrue(more != null)
        assertNull(holder.beginLoadMore("session-a"))
        assertTrue(holder.acceptListSuccess(more!!, content(item("one", subject = "updated"), item("two"))))

        assertEquals(listOf("updated", "two"), holder.state.listContentOrNull()!!.items.map(MessageListItem::subject))
        assertNull(holder.beginLoadMore("session-a"))
    }

    @Test
    fun `opening and closing detail invalidate stale callback`() {
        val holder = holderWithContent()
        val request = holder.beginDetailLoad("session-a", "thread-1")
        assertEquals(MessagesRoute.Detail("thread-1"), holder.state.route)
        holder.closeDetail()

        assertEquals(MessagesRoute.List, holder.state.route)
        assertNull(holder.state.detailState)
        assertFalse(holder.acceptDetailSuccess(request, detail()))
    }

    @Test
    fun `detail cursor pagination merges messages`() {
        val holder = MessagesStateHolder()
        val initial = holder.beginDetailLoad("session-a", "thread-1")
        holder.acceptDetailSuccess(initial, detail(messages = listOf(bubble("one")), nextCursor = "next"))

        val more = holder.beginDetailLoadMore("session-a")
        assertTrue(more != null)
        assertTrue(holder.acceptDetailSuccess(more!!, detail(messages = listOf(bubble("two")))))
        assertEquals(listOf("one", "two"), holder.state.detailContentOrNull()!!.messages.map(MessageBubble::id))
    }

    @Test
    fun `read mutation marks current thread read without losing content`() {
        val holder = MessagesStateHolder()
        val detailRequest = holder.beginDetailLoad("session-a", "thread-1")
        holder.acceptDetailSuccess(detailRequest, detail(status = "unread"))
        val read = holder.beginMarkThreadRead("session-a")

        assertTrue(read != null)
        assertTrue(holder.acceptMutationSuccess(read!!, operation()))
        assertEquals("read", holder.state.detailContentOrNull()!!.status)
        assertFalse(holder.state.isMarkingRead)
    }

    @Test
    fun `refresh operation keeps detail content and resets refresh flag`() {
        val holder = MessagesStateHolder()
        val request = holder.beginDetailLoad("session-a", "thread-1")
        holder.acceptDetailSuccess(request, detail())
        val refresh = holder.beginRefreshThread("session-a")

        assertTrue(refresh != null)
        assertTrue(holder.state.isRefreshingThread)
        assertTrue(holder.acceptMutationSuccess(refresh!!, operation(queued = true)))
        assertEquals("thread-1", holder.state.detailContentOrNull()!!.id)
        assertFalse(holder.state.isRefreshingThread)
    }

    @Test
    fun `reply is optimistic and duplicate request id remains one bubble`() {
        val holder = MessagesStateHolder()
        val detailRequest = holder.beginDetailLoad("session-a", "thread-1")
        holder.acceptDetailSuccess(detailRequest, detail())
        val first = holder.beginReply("session-a", "Dziękuję", "reply-123")

        assertTrue(first != null)
        assertNull(holder.beginReply("session-a", "Dziękuję", "reply-123"))
        assertEquals(1, holder.state.detailContentOrNull()!!.messages.count { it.requestId == "reply-123" })
        assertTrue(holder.acceptMutationSuccess(first!!, operation(messageId = "server-1", queued = true)))
        assertEquals("queued", holder.state.detailContentOrNull()!!.messages.last().status)
    }

    @Test
    fun `closing detail rejects a late mutation callback`() {
        val holder = MessagesStateHolder()
        val detailRequest = holder.beginDetailLoad("session-a", "thread-1")
        holder.acceptDetailSuccess(detailRequest, detail())
        val reply = holder.beginReply("session-a", "Dziękuję", "reply-late")

        holder.closeDetail()

        assertFalse(holder.acceptMutationSuccess(reply!!, operation()))
        assertNull(holder.state.detailContentOrNull())
        assertFalse(holder.state.isSendingReply)
    }

    @Test
    fun `unauthorized retry can be accepted once without clearing content`() {
        val holder = holderWithContent()
        val request = holder.beginListRefresh("session-a")
        assertTrue(holder.acceptListUnauthorized(request))
        assertEquals(listOf("thread-1"), holder.state.listContentOrNull()!!.items.map(MessageListItem::id))
        val retry = holder.beginListUnauthorizedRetry(request)
        assertTrue(retry != null)
        assertNull(holder.beginListUnauthorizedRetry(request))
    }

    private fun holderWithContent() = MessagesStateHolder().also { holder ->
        val request = holder.beginListReset("session-a", MessagesQuery())
        holder.acceptListSuccess(request, content(item("thread-1")))
    }

    private fun content(vararg items: MessageListItem, nextCursor: String? = null) = MessagesContent(
        items = items.toList(), total = items.size, nextCursor = nextCursor, unreadCount = items.count(MessageListItem::isUnread),
    )

    private fun item(id: String, subject: String = id) = MessageListItem(
        id = id, providerId = "allegro", integrationId = "integration", providerLabel = "Allegro",
        customerName = "Anna", customerLogin = "anna", subject = subject, preview = null, lastMessageAt = "",
        messageCount = 1, orderId = null, orderNumber = null, readAt = null, status = "unread", channel = MessagesChannel.ALL,
    )

    private fun detail(
        status: String = "unread",
        messages: List<MessageBubble> = emptyList(),
        nextCursor: String? = null,
    ) = MessageThreadDetail(
        id = "thread-1", providerId = "allegro", integrationId = "integration", providerLabel = "Allegro",
        customerName = "Anna", customerLogin = "anna", customerEmail = null, subject = "Temat", lastMessageAt = "",
        readAt = null, status = status, orderId = null, orderNumber = null, messages = messages, nextCursor = nextCursor,
        customerContext = null,
    )

    private fun bubble(id: String) = MessageBubble(id, "Anna", MessageDirection.INBOUND, id, "", "received", emptyList())

    private fun operation(messageId: String? = null, queued: Boolean = false) = MessageOperation(
        operationId = "operation", messageId = messageId, queued = queued, duplicate = false, status = "accepted",
    )
}
