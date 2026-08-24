package pl.dlaflow.mobile.feature.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.MobileMessage
import pl.dlaflow.mobile.MobileMessageAttachment
import pl.dlaflow.mobile.MobileMessageBuyer
import pl.dlaflow.mobile.MobileMessageOrderLink
import pl.dlaflow.mobile.MobileMessagePreview
import pl.dlaflow.mobile.MobileMessageThread
import pl.dlaflow.mobile.MobileMessageThreadDetail
import pl.dlaflow.mobile.MobileMessagesPage

class MessagesMapperTest {
    @Test
    fun `provider labels and channels are normalized`() {
        val items = MobileMessagesPage(
            items = listOf(
                fixtureThread(providerId = "allegro"),
                fixtureThread(id = "gmail", providerId = "gmail"),
                fixtureThread(id = "woo", providerId = "woocommerce"),
            ),
            total = 3,
            nextCursor = " next ",
            unreadCount = 2,
        ).toMessagesContent().items

        assertEquals("Allegro", items[0].providerLabel)
        assertEquals(MessagesChannel.MARKETPLACE, items[0].channel)
        assertEquals("Gmail", items[1].providerLabel)
        assertEquals(MessagesChannel.EMAIL, items[1].channel)
        assertEquals("WooCommerce", items[2].providerLabel)
        assertEquals(MessagesChannel.STORE, items[2].channel)
    }

    @Test
    fun `blank buyer and order values use safe presentation fallbacks`() {
        val item = fixtureThread(
            buyer = MobileMessageBuyer(name = " ", login = " "),
            orderLink = MobileMessageOrderLink(id = " ", orderId = " "),
            subject = " ",
        ).toMessageListItem()

        assertEquals("Nieznany klient", item.customerName)
        assertNull(item.orderNumber)
        assertNull(item.orderId)
        assertEquals("Bez tematu", item.subject)
    }

    @Test
    fun `direction and attachment metadata are mapped without provider payload`() {
        val detail = fixtureDetail().toMessageThreadDetail()
        val inbound = detail.messages[0]
        val outbound = detail.messages[1]

        assertEquals(MessageDirection.INBOUND, inbound.direction)
        assertEquals(MessageDirection.OUTBOUND, outbound.direction)
        assertEquals("invoice.pdf", inbound.attachments.single().filename)
        assertEquals("application/pdf", inbound.attachments.single().contentType)
        assertEquals(12L, inbound.attachments.single().size)
        assertTrue(inbound.attachments.single().url.startsWith("/api/"))
    }

    @Test
    fun `empty and invalid numeric values are bounded`() {
        val page = MobileMessagesPage(
            items = listOf(fixtureThread(messageCount = -4)),
            total = -1,
            nextCursor = " ",
            unreadCount = -2,
        ).toMessagesContent()
        assertEquals(0, page.total)
        assertEquals(0, page.unreadCount)
        assertNull(page.nextCursor)
        assertEquals(0, page.items.single().messageCount)
    }

    private fun fixtureThread(
        id: String = "thread-1",
        providerId: String = "allegro",
        buyer: MobileMessageBuyer = MobileMessageBuyer("Anna Kowalska", "anna"),
        orderLink: MobileMessageOrderLink? = MobileMessageOrderLink("12345", "order-1"),
        messageCount: Int = 3,
        subject: String = "Pytanie o czas realizacji",
    ) = MobileMessageThread(
        id = id,
        providerId = providerId,
        integrationId = "integration-1",
        buyer = buyer,
        subject = subject,
        lastMessage = MobileMessagePreview("Czy paczka wyjdzie?", "inbound", "2026-08-24T10:00:00Z"),
        lastMessageAt = "2026-08-24T10:00:00Z",
        messageCount = messageCount,
        orderLink = orderLink,
        readAt = null,
        status = "unread",
    )

    private fun fixtureDetail() = MobileMessageThreadDetail(
        id = "thread-1",
        providerId = "gmail",
        integrationId = "integration-1",
        buyer = MobileMessageBuyer("Anna", "anna", "anna@example.test"),
        subject = "Temat",
        lastMessageAt = "2026-08-24T10:00:00Z",
        readAt = null,
        status = "unread",
        orderLink = null,
        customerContext = null,
        messages = listOf(
            MobileMessage(
                id = "inbound-1",
                author = "Anna",
                direction = "inbound",
                body = "Treść",
                messageAt = "2026-08-24T10:00:00Z",
                status = "received",
                attachments = listOf(
                    MobileMessageAttachment("attachment-1", "invoice.pdf", "application/pdf", 12, "ready", "/api/media/invoice.pdf"),
                ),
            ),
            MobileMessage("outbound-1", "Operator", "outbound", "Dziękuję", "2026-08-24T10:02:00Z", "sent", emptyList()),
        ),
        total = 2,
        nextCursor = null,
    )
}
