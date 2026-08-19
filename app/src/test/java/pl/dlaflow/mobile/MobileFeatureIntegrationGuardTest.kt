package pl.dlaflow.mobile

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MobileFeatureIntegrationGuardTest {
    @Test
    fun `order list and detail parsers preserve deadline and status color`() {
        withSingleJsonResponse(
            """{
                "data":[{
                    "id":"order-list",
                    "shippingDeadlineAt":"2026-08-20T10:15:00.000Z",
                    "statusColor":"#112233"
                }],
                "meta":{"count":1,"limit":20,"offset":0,"total":1}
            }""".trimIndent(),
        ) { client, request ->
            val order: MobileOrderListItem = client.listOrders(
                token = "synthetic-token",
                search = "",
                filter = MobileOrderFilter.ALL,
            ).data.single()

            assertEquals("GET", request.method)
            assertEquals("/api/mobile/orders?limit=20&offset=0", request.path)
            assertEquals("2026-08-20T10:15:00.000Z", order.shippingDeadlineAt)
            assertEquals("#112233", order.statusColor)
        }

        withSingleJsonResponse(
            """{
                "data":{
                    "id":"order-detail",
                    "shippingDeadlineAt":"2026-08-21T11:30:00.000Z",
                    "statusColor":"#445566"
                }
            }""".trimIndent(),
        ) { client, request ->
            val order: MobileOrderDetail = client.getOrder("synthetic-token", "order-detail")

            assertEquals("GET", request.method)
            assertEquals("/api/mobile/orders/order-detail", request.path)
            assertEquals("2026-08-21T11:30:00.000Z", order.shippingDeadlineAt)
            assertEquals("#445566", order.statusColor)
        }
    }

    @Test
    fun `push installation uses the current device scoped put contract`() {
        withSingleJsonResponse("{}") { client, request ->
            client.updatePushInstallation(
                token = "synthetic-token",
                deviceId = "device one/two",
                installationId = "installation-123",
            )

            assertEquals("PUT", request.method)
            assertEquals(
                "/api/mobile/devices/device+one%2Ftwo/push-installation",
                request.path,
            )
            assertEquals("Bearer synthetic-token", request.authorization)
            assertEquals("installation-123", JSONObject(request.body).getString("installationId"))
        }
    }

    @Test
    fun `notification preferences remain the final native notification filter`() {
        val defaults = MobileNotificationPreferences.defaults()
        assertTrue(MobileNotificationCategory.entries.all(defaults::isEnabled))

        val preferences = defaults
            .withEnabled(MobileNotificationCategory.NEW_ORDERS, false)
            .withEnabled(MobileNotificationCategory.PHOTO_TASKS, false)
        assertEquals(
            preferences,
            parseMobileNotificationPreferences(serializeMobileNotificationPreferences(preferences)),
        )

        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Nowe zamówienie", "info", "OPEN_ORDERS"),
                preferences,
            ),
        )
        assertTrue(
            shouldShowNativePanelNotification(
                testNotification("Wiadomość od klienta", "info", "OPEN_MESSAGES"),
                preferences,
            ),
        )
        assertTrue(
            shouldShowNativePanelNotification(
                testNotification("Problem synchronizacji", "warning", "OPEN_LOGS_SUMMARY"),
                preferences,
            ),
        )
        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Informacja z panelu", "info", "OPEN_LOGS_SUMMARY"),
                preferences,
            ),
        )
        assertFalse(shouldShowNativePhotoTaskNotification(preferences))
    }

    @Test
    fun `orders scanner keeps the compact strip and excludes the historical card`() {
        val scannerSource = File(
            "src/main/java/pl/dlaflow/mobile/feature/orders/OrdersPackageScannerStrip.kt",
        ).readText()

        assertTrue(scannerSource.contains("internal fun OrdersPackageScannerStrip("))
        assertTrue(scannerSource.contains("BoxWithConstraints"))
        assertTrue(
            Regex("maxWidth\\s*<\\s*360\\.dp\\s*\\|\\|\\s*LocalDensity\\.current\\.fontScale\\s*>=\\s*1\\.2f")
                .containsMatchIn(scannerSource),
        )
        assertTrue(scannerSource.contains("ScannerStripCopy("))
        assertTrue(scannerSource.contains("ScannerStripAction("))

        val historicalCardSources = File("src/main/java")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.readText().contains("ScannerFeatureCard") }
            .map { it.relativeTo(File("src/main/java")).invariantSeparatorsPath }
            .toList()
        assertTrue(
            "Historical ScannerFeatureCard must remain absent, found in $historicalCardSources",
            historicalCardSources.isEmpty(),
        )
    }

    private fun testNotification(
        title: String,
        tone: String,
        actionType: String,
    ) = MobileAssistantNotification(
        id = title,
        title = title,
        description = "Opis",
        tone = tone,
        source = "DlaFlow",
        account = "Panel",
        occurredAt = "2026-08-19T08:00:00Z",
        readAt = null,
        mobileAction = MobileNotificationAction(actionType, "Otwórz"),
    )

    private fun withSingleJsonResponse(
        responseJson: String,
        action: (MobileApiClient, CapturedRequest) -> Unit,
    ) {
        val request = CapturedRequest()
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newSingleThreadExecutor()
        val responseFuture = executor.submit {
            server.accept().use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val requestLine = reader.readLine().orEmpty().split(" ")
                request.method = requestLine.getOrElse(0) { "" }
                request.path = requestLine.getOrElse(1) { "" }

                val headers = generateSequence { reader.readLine() }
                    .takeWhile { it.isNotEmpty() }
                    .mapNotNull { line ->
                        val separator = line.indexOf(':')
                        if (separator <= 0) null else line.substring(0, separator).lowercase() to line.substring(separator + 1).trim()
                    }
                    .toMap()
                request.authorization = headers["authorization"].orEmpty()
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                if (contentLength > 0) {
                    val body = CharArray(contentLength)
                    var offset = 0
                    while (offset < body.size) {
                        val read = reader.read(body, offset, body.size - offset)
                        if (read < 0) break
                        offset += read
                    }
                    request.body = String(body, 0, offset)
                }

                val responseBody = responseJson.toByteArray(Charsets.UTF_8)
                val responseHeaders =
                    "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${responseBody.size}\r\nConnection: close\r\n\r\n"
                socket.getOutputStream().use { output ->
                    output.write(responseHeaders.toByteArray(Charsets.UTF_8))
                    output.write(responseBody)
                }
            }
        }

        try {
            action(MobileApiClient("http://127.0.0.1:${server.localPort}"), request)
            responseFuture.get(2, TimeUnit.SECONDS)
        } finally {
            server.close()
            executor.shutdownNow()
        }
    }

    private class CapturedRequest {
        @Volatile var method: String = ""
        @Volatile var path: String = ""
        @Volatile var authorization: String = ""
        @Volatile var body: String = ""
    }
}
