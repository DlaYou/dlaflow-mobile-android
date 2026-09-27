package pl.dlaflow.mobile.feature.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.MobileMessage
import pl.dlaflow.mobile.MobileMessageAttachment
import pl.dlaflow.mobile.MobileMessageBuyer
import pl.dlaflow.mobile.MobileMessageCustomerContext
import pl.dlaflow.mobile.MobileMessageOrderLink
import pl.dlaflow.mobile.MobileMessagePreview
import pl.dlaflow.mobile.MobileMessageRelatedOffer
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

    @Test
    fun `message text keeps Polish characters and removes provider html from body preview and subject`() {
        val mojibake = "Dzie\u00C5\u201E dobry za\u00C5\u00BC\u00C3\u00B3\u00C5\u201A\u00C4\u2021"
        val rawBody = "<p>$mojibake&nbsp;</p><br><strong>Odbiór</strong> &amp; płatność"
        val thread = fixtureThread(
            subject = "&lt;strong&gt;Pytanie o wysyłkę&lt;/strong&gt;",
        ).copy(lastMessage = MobileMessagePreview(rawBody, "inbound", "2026-08-24T10:00:00Z"))

        val listItem = thread.toMessageListItem()
        assertEquals("Pytanie o wysyłkę", listItem.subject)
        assertEquals("Dzień dobry zażółć Odbiór & płatność", listItem.preview?.body)

        val detail = fixtureDetail().copy(
            subject = "<div>Temat zażółć</div>",
            messages = listOf(
                MobileMessage(
                    id = "inbound-1",
                    author = "Klient",
                    direction = "inbound",
                    body = rawBody,
                    messageAt = "2026-08-24T10:00:00Z",
                    status = "received",
                    attachments = emptyList(),
                ),
            ),
        ).toMessageThreadDetail()

        assertEquals("Temat zażółć", detail.subject)
        assertEquals("Dzień dobry zażółć\nOdbiór & płatność", detail.messages.single().body)
    }

    @Test
    fun `message html active content is not shown as visible text`() {
        val bubble = MobileMessage(
            id = "message-unsafe",
            author = "Klient",
            direction = "inbound",
            body = "<script>alert('x')</script><style>.hidden{display:none}</style><div>Bezpieczna treść</div>",
            messageAt = "2026-08-24T10:00:00Z",
            status = "received",
            attachments = emptyList(),
        ).toMessageBubble()

        assertEquals("Bezpieczna treść", bubble.body)
    }

    @Test
    fun `related offer title removes html and keeps order context`() {
        val detail = fixtureDetail().copy(
            orderLink = MobileMessageOrderLink("ORD-1001", "order-1001"),
            customerContext = MobileMessageCustomerContext(1, "2025-01-01", "PLN", 2, 149.99),
            relatedOffer = MobileMessageRelatedOffer(
                offerId = "1234567890",
                title = "&lt;strong&gt;Bluza Classic&lt;/strong&gt;",
                sku = "BLUZA-01",
                image = "/api/mobile/products/media/product.webp?variant=thumb",
            ),
        ).toMessageThreadDetail()

        assertEquals("Bluza Classic", detail.relatedOffer?.title)
        assertEquals("BLUZA-01", detail.relatedOffer?.sku)
        assertEquals("/api/mobile/products/media/product.webp?variant=thumb", detail.relatedOffer?.image)
        assertEquals("1234567890", detail.relatedOffer?.offerId)
        assertEquals("ORD-1001", detail.relatedOrder?.orderNumber)
        assertEquals(2, detail.customerContext?.orderCount)
    }

    @Test
    fun `missing offer catalog entry uses Allegro offer fallback and rejects external image`() {
        val detail = fixtureDetail().copy(
            relatedOffer = MobileMessageRelatedOffer(
                offerId = "1234567890",
                title = " ",
                sku = " ",
                image = "https://outside.example.test/api/mobile/products/media/product.webp?variant=thumb",
            ),
        ).toMessageThreadDetail()

        assertEquals("Oferta Allegro #1234567890", detail.relatedOffer?.title)
        assertEquals("", detail.relatedOffer?.sku)
        assertEquals("", detail.relatedOffer?.image)
        assertEquals("1234567890", detail.relatedOffer?.offerId)
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
