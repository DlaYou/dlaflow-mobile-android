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

class MobileNotificationsTest {
    @Test
    fun `notification summary drives visible badge`() {
        assertEquals(NotificationBadgeState.NONE, notificationBadgeState(0, 0))
        assertEquals(NotificationBadgeState.NORMAL, notificationBadgeState(3, 0))
        assertEquals(NotificationBadgeState.ATTENTION, notificationBadgeState(3, 1))
    }

    @Test
    fun `urgent notification policy is limited to actionable items`() {
        assertTrue(shouldShowNativePanelNotification("error", "OPEN_ORDERS"))
        assertTrue(shouldShowNativePanelNotification("warning", "OPEN_PRODUCTS"))
        assertTrue(shouldShowNativePanelNotification("info", "OPEN_MESSAGES"))
        assertTrue(shouldShowNativePanelNotification("info", "OPEN_PHOTO_TASKS"))
        assertFalse(shouldShowNativePanelNotification("success", "OPEN_LOGS_SUMMARY"))
        assertFalse(shouldShowNativePanelNotification("info", "OPEN_LOGS_SUMMARY"))
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
}
