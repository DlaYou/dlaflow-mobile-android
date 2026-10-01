package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class MobileNotificationsTest {
    @Test
    fun `notification summary drives visible badge`() {
        assertEquals(NotificationBadgeState.NONE, notificationBadgeState(0, 0))
        assertEquals(NotificationBadgeState.NORMAL, notificationBadgeState(3, 0))
        assertEquals(NotificationBadgeState.ATTENTION, notificationBadgeState(3, 1))
    }

    @Test
    fun `legacy notification policy does not filter panel entries`() {
        assertTrue(shouldShowNativePanelNotification("error", "OPEN_ORDERS"))
        assertTrue(shouldShowNativePanelNotification("warning", "OPEN_PRODUCTS"))
        assertTrue(shouldShowNativePanelNotification("info", "OPEN_MESSAGES"))
        assertTrue(shouldShowNativePanelNotification("info", "OPEN_PHOTO_TASKS"))
        assertTrue(shouldShowNativePanelNotification("success", "OPEN_LOGS_SUMMARY"))
        assertTrue(shouldShowNativePanelNotification("info", "OPEN_LOGS_SUMMARY"))
    }

    @Test
    fun `notification filters keep only matching rows`() {
        val all = listOf(
            testNotification("1", "info", null, "OPEN_MESSAGES"),
            testNotification("2", "warning", null, "OPEN_PRODUCTS"),
            testNotification("3", "info", "2026-07-01T12:10:00.000Z", "OPEN_LOGS_SUMMARY"),
        )

        assertEquals(3, filterNotifications(all, MobileNotificationFilter.ALL).size)
        assertEquals(listOf("2"), filterNotifications(all, MobileNotificationFilter.ATTENTION).map { it.id })
        assertEquals(listOf("1", "2"), filterNotifications(all, MobileNotificationFilter.UNREAD).map { it.id })
    }

    @Test
    fun `shown notification ids are stored as bounded set`() {
        val ids = rememberShownNotificationId("", "n1", maxIds = 2)
            .let { rememberShownNotificationId(it, "n2", maxIds = 2) }
            .let { rememberShownNotificationId(it, "n1", maxIds = 2) }
            .let { rememberShownNotificationId(it, "n3", maxIds = 2) }

        assertTrue(hasShownNotificationId(ids, "n1"))
        assertTrue(hasShownNotificationId(ids, "n3"))
        assertFalse(hasShownNotificationId(ids, "n2"))
        assertFalse(hasShownNotificationId(ids, "n4"))
    }

    @Test
    fun `FCM and polling claim the same panel event only once`() {
        val memory = FakePanelNotificationMemory()
        val notification = testNotification("message:neuIC0O99vRloK-xMK7XGzHP", "info", null, "OPEN_MESSAGES")
        val attempts = AtomicInteger()

        assertTrue(
            deliverPanelNotificationOnce(memory, notification) {
                attempts.incrementAndGet()
                true
            },
        )
        assertFalse(
            deliverPanelNotificationOnce(memory, notification.copy(id = "message:neuIC0O99vRloK-xMK7XGzHP")) {
                attempts.incrementAndGet()
                true
            },
        )

        assertEquals(1, attempts.get())
        assertEquals("message:neuIC0O99vRloK-xMK7XGzHP", memory.shownIds)
    }

    @Test
    fun `failed panel delivery releases the claim for a later retry`() {
        val memory = FakePanelNotificationMemory()
        val notification = testNotification("order:order-1", "info", null, "OPEN_ORDERS")
        val attempts = AtomicInteger()

        assertFalse(
            deliverPanelNotificationOnce(memory, notification) {
                attempts.incrementAndGet()
                false
            },
        )
        assertTrue(
            deliverPanelNotificationOnce(memory, notification) {
                attempts.incrementAndGet()
                true
            },
        )

        assertEquals(2, attempts.get())
        assertEquals("order:order-1", memory.shownIds)
    }

    @Test
    fun `push delivery keys match panel notification ids and reject missing event ids`() {
        assertEquals("order:order-1", pushNotificationDeliveryId("order.created", "order-1"))
        assertEquals("message:neuIC0O99vRloK-xMK7XGzHP", pushNotificationDeliveryId("message.created", "message-1"))
        assertEquals(null, pushNotificationDeliveryId("order.created", " "))
        assertEquals(null, pushNotificationDeliveryId("message.created", ""))
        assertEquals(null, pushNotificationDeliveryId("product.created", "product-1"))
    }

    @Test
    fun `customer message title prefers the channel label from the push payload`() {
        assertEquals(
            "Gmail - Nowa wiadomość",
            customerMessageNotificationTitle("gmail", " Gmail - Nowa wiadomość "),
        )
        assertEquals(
            "Allegro - Nowa wiadomość",
            customerMessageNotificationTitle("allegro", "Allegro - Nowa wiadomość"),
        )
    }

    @Test
    fun `customer message title derives known channel when title is absent`() {
        assertEquals("Gmail - Nowa wiadomość", customerMessageNotificationTitle("gmail", null))
        assertEquals("Allegro - Nowa wiadomość", customerMessageNotificationTitle("ALLEGRO", " "))
        assertEquals("E-mail - Nowa wiadomość", customerMessageNotificationTitle("email", null))
        assertEquals("Social - Nowa wiadomość", customerMessageNotificationTitle("social", null))
    }

    @Test
    fun `customer message title includes sender for Gmail and Allegro`() {
        assertEquals(
            "Gmail - Nowa wiadomość od Anna Kowalska",
            customerMessageNotificationTitle("gmail", "Gmail - Nowa wiadomość", " Anna\nKowalska ", false),
        )
        assertEquals(
            "Allegro - Nowa wiadomość od kupujacy_123",
            customerMessageNotificationTitle("allegro", "Allegro - Nowa wiadomość", "kupujacy_123", false),
        )
    }

    @Test
    fun `customer message title labels a reply with sender`() {
        assertEquals(
            "Allegro - Odpowiedź na wiadomość od kupujacy_123",
            customerMessageNotificationTitle("allegro", "Allegro - Nowa wiadomość", "kupujacy_123", true),
        )
    }

    @Test
    fun `customer message title falls back when sender metadata is absent`() {
        assertEquals(
            "Gmail - Nowa wiadomość",
            customerMessageNotificationTitle("gmail", "Gmail - Nowa wiadomość", null, false),
        )
        assertEquals(
            "Allegro - Odpowiedź na wiadomość",
            customerMessageNotificationTitle("allegro", null, " ", true),
        )
    }

    @Test
    fun `customer message title keeps legacy fallback for unknown or missing channel`() {
        assertEquals(
            "Nowa wiadomość od klienta",
            customerMessageNotificationTitle("future-provider", null),
        )
        assertEquals(
            "Nowa wiadomość od klienta",
            customerMessageNotificationTitle(null, ""),
        )
    }

    @Test
    fun `customer message title strips control whitespace and stays bounded`() {
        val title = customerMessageNotificationTitle(
            "gmail",
            "Gmail\n-\tNowa\u0000 wiadomość" + "x".repeat(200),
        )

        assertEquals(120, title.length)
        assertFalse(title.any(Char::isISOControl))
        assertFalse(title.contains("\n"))
    }

    @Test
    fun `canonical panel notification id overrides the legacy push fallback`() {
        assertEquals(
            "message:panel-canonical-123",
            pushNotificationDeliveryId(
                event = "message.created",
                eventId = "message-1",
                canonicalNotificationId = "message:panel-canonical-123",
            ),
        )
        assertEquals(
            "order:panel-canonical-456",
            pushNotificationDeliveryId(
                event = "order.created",
                eventId = "order-1",
                canonicalNotificationId = "order:panel-canonical-456",
            ),
        )
    }

    @Test
    fun `targeted push is accepted only by the matching paired device`() {
        assertTrue(pushTargetMatchesDevice(targetDeviceId = null, currentDeviceId = "device-a"))
        assertTrue(pushTargetMatchesDevice(targetDeviceId = " ", currentDeviceId = "device-a"))
        assertTrue(pushTargetMatchesDevice(targetDeviceId = "device-a", currentDeviceId = "device-a"))
        assertFalse(pushTargetMatchesDevice(targetDeviceId = "device-old", currentDeviceId = "device-a"))
        assertFalse(pushTargetMatchesDevice(targetDeviceId = "device-a", currentDeviceId = ""))
    }

    @Test
    fun `panel alert notification id keeps distinct hash collision ids`() {
        assertEquals(panelAlertNotificationId("job:100"), panelAlertNotificationId("job:100"))
        assertTrue(panelAlertNotificationId("job:100") != panelAlertNotificationId("job:218"))
    }

    @Test
    fun `canonical orders actions open the orders screen`() {
        assertTrue(isOrdersNotificationAction("OPEN_ORDERS"))
        assertTrue(isOrdersNotificationAction("orders"))
        assertFalse(isOrdersNotificationAction("OPEN_LOGS_SUMMARY"))
    }

    @Test
    fun `customer message actions open the messages screen`() {
        assertTrue(isMessagesNotificationAction("OPEN_MESSAGES"))
        assertTrue(isMessagesNotificationAction("messages"))
        assertFalse(isMessagesNotificationAction("OPEN_LOGS_SUMMARY"))
    }

    @Test
    fun `legacy message notification without mobile action still opens messages`() {
        assertTrue(
            isLegacyMessagesNotification(
                actionType = "OPEN_LOGS_SUMMARY",
                title = "Gmail - Nowa wiadomość",
                description = "Odebrano nową wiadomość.",
            ),
        )
        assertFalse(
            isLegacyMessagesNotification(
                actionType = "OPEN_ORDERS",
                title = "Wiadomości synchronizacji",
                description = "Sprawdź zamówienie.",
            ),
        )
    }

    @Test
    fun `mobile notifications page parses summary actions and read state`() {
        val payload = """
            {
              "data": {
                "attentionCount": 2,
                "unreadAttentionCount": 1,
                "unreadCount": 3,
                "notifications": [
                  {
                    "id": "job:abc",
                    "title": "Zmiana statusu zamówienia: błąd",
                    "description": "Nie udało się zmienić statusu.",
                    "tone": "error",
                    "source": "DlaFlow",
                    "account": "Panel",
                    "occurredAt": "2026-07-01T12:00:00.000Z",
                    "readAt": null,
                    "mobileAction": { "type": "OPEN_ORDERS", "label": "Zobacz zamówienia" }
                  }
                ]
              }
            }
        """.trimIndent()
        val capture = serveJsonOnce(payload) { baseUrl ->
            val page = MobileApiClient(baseUrl).listNotifications("mobile-token", limit = 80)

            assertEquals(2, page.attentionCount)
            assertEquals(1, page.unreadAttentionCount)
            assertEquals(3, page.unreadCount)
            assertEquals("job:abc", page.notifications.single().id)
            assertEquals(null, page.notifications.single().readAt)
            assertEquals("OPEN_ORDERS", page.notifications.single().mobileAction.type)
            assertEquals("Zobacz zamówienia", page.notifications.single().mobileAction.label)
        }

        assertEquals("GET", capture.method)
        assertEquals("/api/mobile/notifications?limit=20", capture.path)
        assertEquals("Bearer mobile-token", capture.authorization)
    }

    @Test
    fun `mark notifications read posts selected ids`() {
        val capture = serveJsonOnce("""{"data":{"notificationIds":["job:abc"],"readAt":"2026-07-01T12:05:00.000Z"}}""") { baseUrl ->
            MobileApiClient(baseUrl).markNotificationsRead("mobile-token", listOf("job:abc"))
        }

        assertEquals("POST", capture.method)
        assertEquals("/api/mobile/notifications/read", capture.path)
        assertEquals("Bearer mobile-token", capture.authorization)
        assertTrue(capture.body.contains("job:abc"))
    }

    private fun serveJsonOnce(responseJson: String, action: (String) -> Unit): CapturedRequest {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newSingleThreadExecutor()
        val capture = java.util.concurrent.atomic.AtomicReference<CapturedRequest>()
        executor.submit {
            server.accept().use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val requestLine = reader.readLine().orEmpty()
                val parts = requestLine.split(" ")
                var authorization = ""
                var contentLength = 0

                generateSequence { reader.readLine() }
                    .takeWhile { it.isNotEmpty() }
                    .forEach { header ->
                        if (header.startsWith("Authorization:", ignoreCase = true)) {
                            authorization = header.substringAfter(":").trim()
                        }
                        if (header.startsWith("Content-Length:", ignoreCase = true)) {
                            contentLength = header.substringAfter(":").trim().toIntOrNull() ?: 0
                        }
                    }

                val body = if (contentLength > 0) {
                    CharArray(contentLength).also { reader.read(it, 0, contentLength) }.concatToString()
                } else {
                    ""
                }
                capture.set(CapturedRequest(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, authorization, body))

                val responseBody = responseJson.toByteArray(Charsets.UTF_8)
                val headers = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${responseBody.size}\r\nConnection: close\r\n\r\n"
                socket.getOutputStream().use { output ->
                    output.write(headers.toByteArray(Charsets.UTF_8))
                    output.write(responseBody)
                }
            }
        }

        try {
            action("http://127.0.0.1:${server.localPort}")

            return requireNotNull(capture.get())
        } finally {
            server.close()
            executor.shutdownNow()
        }
    }

    private data class CapturedRequest(
        val method: String,
        val path: String,
        val authorization: String,
        val body: String,
    )

    private fun testNotification(id: String, tone: String, readAt: String?, action: String) = MobileAssistantNotification(
        id = id,
        title = "Test",
        description = "Opis",
        tone = tone,
        source = "DlaFlow",
        account = "Panel",
        occurredAt = "2026-07-01T12:00:00.000Z",
        readAt = readAt,
        mobileAction = MobileNotificationAction(type = action, label = "Otwórz"),
    )

    private class FakePanelNotificationMemory : PanelNotificationDeliveryMemory {
        var shownIds: String = ""

        override fun readShownPanelAlertIds(): String = shownIds

        override fun saveShownPanelAlertIds(ids: String) {
            shownIds = ids
        }
    }
}
