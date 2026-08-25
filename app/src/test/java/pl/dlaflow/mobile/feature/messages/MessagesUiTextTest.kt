package pl.dlaflow.mobile.feature.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagesUiTextTest {
    @Test
    fun `filter labels stay in business language`() {
        assertEquals("Wszystkie", messagesFilterLabel(MessagesFilter.ALL))
        assertEquals("Nieprzeczytane", messagesFilterLabel(MessagesFilter.UNREAD))
        assertEquals("Wszystkie kanały", messagesChannelLabel(MessagesChannel.ALL))
        assertEquals("Allegro", messagesChannelLabel(MessagesChannel.MARKETPLACE))
    }

    @Test
    fun `message status badge text is only exposed for unread states`() {
        assertEquals("Nowe", messageStatusBadgeLabel(isNew = true, isUnread = true))
        assertEquals("Nieprzeczytane", messageStatusBadgeLabel(isNew = false, isUnread = true))
        assertEquals(null, messageStatusBadgeLabel(isNew = false, isUnread = false))
    }

    @Test
    fun `blank reply cannot be sent and bounded reply can be sent`() {
        assertFalse(canSendMessageReply(" ", sending = false))
        assertFalse(canSendMessageReply("Dziękuję", sending = true))
        assertTrue(canSendMessageReply("Dziękuję", sending = false))
        assertFalse(canSendMessageReply("x".repeat(2001), sending = false))
    }

    @Test
    fun `message timestamps use safe fallback`() {
        assertEquals("24.08, 12:30", messageTimestampLabel("2026-08-24T12:30:00Z", java.time.ZoneOffset.UTC))
        assertEquals("", messageTimestampLabel("not-a-date", java.time.ZoneOffset.UTC))
    }
}
