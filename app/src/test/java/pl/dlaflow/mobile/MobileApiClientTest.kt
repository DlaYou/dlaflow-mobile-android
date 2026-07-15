package pl.dlaflow.mobile

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class MobileApiClientTest {
    @Test
    fun `mobile media path keeps same origin query and rejects bearer exfiltration targets`() {
        val baseUrl = "https://panel.dlayou.pl"

        assertEquals(
            "/api/mobile/products/media/thumb%20one.webp?width=96&fit=cover",
            resolveMobileMediaPath(
                apiUrl = baseUrl,
                mediaUrl = "https://panel.dlayou.pl/api/mobile/products/media/thumb%20one.webp?width=96&fit=cover",
            ),
        )
        assertEquals(
            "/api/mobile/orders/media/thumb.webp?width=64",
            resolveMobileMediaPath(baseUrl, "/api/mobile/orders/media/thumb.webp?width=64"),
        )
        assertNull(resolveMobileMediaPath(baseUrl, "https://example.test/api/mobile/products/media/thumb.webp"))
        assertNull(resolveMobileMediaPath(baseUrl, "/api/auth/me"))
        assertNull(resolveMobileMediaPath(baseUrl, "//example.test/api/mobile/products/media/thumb.webp"))
    }

    @Test
    fun `mobile media path rejects literal and encoded traversal segments`() {
        val baseUrl = "https://panel.dlayou.pl"

        listOf(
            "/api/mobile/products/media/../secret.webp",
            "/api/mobile/products/media/./thumb.webp",
            "/api/mobile/products/media/%2e%2e/secret.webp",
            "/api/mobile/products/media/%2E/thumb.webp",
            "/api/mobile/products/media/%252e%252e/secret.webp",
            "/api/mobile/products/media/%2e%2e%2fsecret.webp",
            "https://panel.dlayou.pl/api/mobile/products/media/%2E%2E/secret.webp?width=96",
        ).forEach { mediaUrl ->
            assertNull(mediaUrl, resolveMobileMediaPath(baseUrl, mediaUrl))
        }

        assertEquals(
            "/api/mobile/products/media/thumb..final.webp?width=96",
            resolveMobileMediaPath(baseUrl, "/api/mobile/products/media/thumb..final.webp?width=96"),
        )
    }

    @Test
    fun `mobile media rejects declared content length above thumbnail limit`() {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            server.accept().use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                generateSequence { reader.readLine() }.takeWhile { it.isNotEmpty() }.toList()
                val headers = "HTTP/1.1 200 OK\r\nContent-Type: image/webp\r\nContent-Length: 5\r\nConnection: close\r\n\r\n"
                socket.getOutputStream().use { output ->
                    output.write(headers.toByteArray(Charsets.UTF_8))
                    output.write(byteArrayOf(1, 2, 3, 4, 5))
                }
            }
        }

        try {
            val client = MobileApiClient(
                baseUrl = "http://127.0.0.1:${server.localPort}",
                mobileMediaMaxBytes = 4,
            )

            assertNull(client.getMobileMedia("mobile-token", "/api/mobile/products/media/thumb.webp"))
        } finally {
            server.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `mobile media stops chunked stream after thumbnail limit`() {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            server.accept().use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                generateSequence { reader.readLine() }.takeWhile { it.isNotEmpty() }.toList()
                val response = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: image/webp\r\n")
                    append("Transfer-Encoding: chunked\r\n")
                    append("Connection: close\r\n\r\n")
                    append("5\r\n12345\r\n")
                    append("0\r\n\r\n")
                }
                socket.getOutputStream().use { output ->
                    output.write(response.toByteArray(Charsets.UTF_8))
                }
            }
        }

        try {
            val client = MobileApiClient(
                baseUrl = "http://127.0.0.1:${server.localPort}",
                mobileMediaMaxBytes = 4,
            )

            assertNull(client.getMobileMedia("mobile-token", "/api/mobile/orders/media/thumb.webp"))
        } finally {
            server.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `signed mobile media get preserves exact query and adds all authentication headers`() {
        val method = AtomicReference("")
        val path = AtomicReference("")
        val authorization = AtomicReference("")
        val signatureVersion = AtomicReference("")
        val deviceId = AtomicReference("")
        val timestamp = AtomicReference("")
        val nonce = AtomicReference("")
        val bodySha256 = AtomicReference("")
        val signature = AtomicReference("")
        val signer = FakeMobileRequestSigner()
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            server.accept().use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val requestLine = reader.readLine().orEmpty()
                val parts = requestLine.split(" ")
                method.set(parts.getOrElse(0) { "" })
                path.set(parts.getOrElse(1) { "" })

                generateSequence { reader.readLine() }
                    .takeWhile { it.isNotEmpty() }
                    .forEach { header ->
                        when {
                            header.startsWith("Authorization:", ignoreCase = true) -> authorization.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Signature-Version:", ignoreCase = true) -> signatureVersion.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Device-Id:", ignoreCase = true) -> deviceId.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Timestamp:", ignoreCase = true) -> timestamp.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Nonce:", ignoreCase = true) -> nonce.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Body-SHA256:", ignoreCase = true) -> bodySha256.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Signature:", ignoreCase = true) -> signature.set(header.substringAfter(":").trim())
                        }
                    }

                val body = byteArrayOf(1, 2, 3, 4)
                val headers = "HTTP/1.1 200 OK\r\nContent-Type: image/webp\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                socket.getOutputStream().use { output ->
                    output.write(headers.toByteArray(Charsets.UTF_8))
                    output.write(body)
                }
            }
        }

        try {
            val client = MobileApiClient(
                baseUrl = "http://127.0.0.1:${server.localPort}",
                requestSigner = signer,
                deviceIdProvider = { "device-media-123" },
                nowMillis = { 1_783_540_000_123L },
                nonceFactory = { "nonce-media-123456" },
            )
            val pathWithQuery = "/api/mobile/products/media/thumb%20one.webp?width=96&fit=cover"

            val bytes = client.getMobileMedia(token = "mobile-token", pathWithQuery = pathWithQuery)

            val expectedBodyHash = sha256Hex(ByteArray(0))
            assertEquals(listOf<Byte>(1, 2, 3, 4), bytes?.toList())
            assertEquals("GET", method.get())
            assertEquals(pathWithQuery, path.get())
            assertEquals("Bearer mobile-token", authorization.get())
            assertEquals("v1", signatureVersion.get())
            assertEquals("device-media-123", deviceId.get())
            assertEquals("1783540000", timestamp.get())
            assertEquals("nonce-media-123456", nonce.get())
            assertEquals(expectedBodyHash, bodySha256.get())
            assertEquals("fake-signature", signature.get())
            assertEquals(
                "v1\nGET\n$pathWithQuery\n$expectedBodyHash\n1783540000\nnonce-media-123456\ndevice-media-123",
                signer.lastCanonical,
            )
        } finally {
            server.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `unauthorized helper media response returns no bytes`() {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            server.accept().use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                generateSequence { reader.readLine() }.takeWhile { it.isNotEmpty() }.toList()
                val body = """{"error":{"code":"AUTH_REQUIRED"}}""".toByteArray(Charsets.UTF_8)
                val headers = "HTTP/1.1 401 Unauthorized\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                socket.getOutputStream().use { output ->
                    output.write(headers.toByteArray(Charsets.UTF_8))
                    output.write(body)
                }
            }
        }

        try {
            val client = MobileApiClient("http://127.0.0.1:${server.localPort}")

            assertNull(client.getMobileMedia("mobile-token", "/api/mobile/orders/media/thumb.webp"))
        } finally {
            server.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `revoke current device posts to mobile self revoke endpoint with bearer token`() {
        val method = AtomicReference("")
        val path = AtomicReference("")
        val authorization = AtomicReference("")
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            server.accept().use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val requestLine = reader.readLine().orEmpty()
                val parts = requestLine.split(" ")
                method.set(parts.getOrElse(0) { "" })
                path.set(parts.getOrElse(1) { "" })

                generateSequence { reader.readLine() }
                    .takeWhile { it.isNotEmpty() }
                    .forEach { header ->
                        if (header.startsWith("Authorization:", ignoreCase = true)) {
                            authorization.set(header.substringAfter(":").trim())
                        }
                    }

                val body = """{"data":{"device":{"id":"device-1","status":"REVOKED"}}}""".toByteArray(Charsets.UTF_8)
                val headers = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                socket.getOutputStream().use { output ->
                    output.write(headers.toByteArray(Charsets.UTF_8))
                    output.write(body)
                }
            }
        }

        try {
            val client = MobileApiClient("http://127.0.0.1:${server.localPort}")

            client.revokeCurrentDevice("mobile-token")

            assertEquals("POST", method.get())
            assertEquals("/api/mobile/me/revoke", path.get())
            assertEquals("Bearer mobile-token", authorization.get())
        } finally {
            server.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `signed mobile request adds canonical signature headers`() {
        val method = AtomicReference("")
        val path = AtomicReference("")
        val authorization = AtomicReference("")
        val signatureVersion = AtomicReference("")
        val deviceId = AtomicReference("")
        val timestamp = AtomicReference("")
        val nonce = AtomicReference("")
        val bodySha256 = AtomicReference("")
        val signature = AtomicReference("")
        val signer = FakeMobileRequestSigner()
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            server.accept().use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val requestLine = reader.readLine().orEmpty()
                val parts = requestLine.split(" ")
                method.set(parts.getOrElse(0) { "" })
                path.set(parts.getOrElse(1) { "" })

                generateSequence { reader.readLine() }
                    .takeWhile { it.isNotEmpty() }
                    .forEach { header ->
                        when {
                            header.startsWith("Authorization:", ignoreCase = true) -> authorization.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Signature-Version:", ignoreCase = true) -> signatureVersion.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Device-Id:", ignoreCase = true) -> deviceId.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Timestamp:", ignoreCase = true) -> timestamp.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Nonce:", ignoreCase = true) -> nonce.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Body-SHA256:", ignoreCase = true) -> bodySha256.set(header.substringAfter(":").trim())
                            header.startsWith("X-DlaFlow-Signature:", ignoreCase = true) -> signature.set(header.substringAfter(":").trim())
                        }
                    }

                val body = """{"data":{"device":{"id":"device-1","status":"REVOKED"}}}""".toByteArray(Charsets.UTF_8)
                val headers = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                socket.getOutputStream().use { output ->
                    output.write(headers.toByteArray(Charsets.UTF_8))
                    output.write(body)
                }
            }
        }

        try {
            val client = MobileApiClient(
                baseUrl = "http://127.0.0.1:${server.localPort}",
                requestSigner = signer,
                deviceIdProvider = { "device-123" },
                nowMillis = { 1_783_540_000_123L },
                nonceFactory = { "nonce-abc-123456" },
            )

            client.revokeCurrentDevice("mobile-token")

            val expectedBodyHash = sha256Hex("{}".toByteArray(Charsets.UTF_8))
            assertEquals("POST", method.get())
            assertEquals("/api/mobile/me/revoke", path.get())
            assertEquals("Bearer mobile-token", authorization.get())
            assertEquals("v1", signatureVersion.get())
            assertEquals("device-123", deviceId.get())
            assertEquals("1783540000", timestamp.get())
            assertEquals("nonce-abc-123456", nonce.get())
            assertEquals(expectedBodyHash, bodySha256.get())
            assertEquals("fake-signature", signature.get())
            assertEquals(
                "v1\nPOST\n/api/mobile/me/revoke\n$expectedBodyHash\n1783540000\nnonce-abc-123456\ndevice-123",
                signer.lastCanonical,
            )
        } finally {
            server.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `complete pairing sends device public key and signs first session lookup`() {
        val firstBody = AtomicReference("")
        val secondDeviceId = AtomicReference("")
        val signer = FakeMobileRequestSigner(publicKey = "PUBLIC_KEY_BASE64_VALUE_WITH_ENOUGH_LENGTH_1234567890")
        val server = ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            repeat(2) { index ->
                server.accept().use { socket ->
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                    val requestLine = reader.readLine().orEmpty()
                    val headers = mutableMapOf<String, String>()
                    var contentLength = 0
                    generateSequence { reader.readLine() }
                        .takeWhile { it.isNotEmpty() }
                        .forEach { header ->
                            val name = header.substringBefore(":").trim()
                            val value = header.substringAfter(":").trim()
                            headers[name.lowercase()] = value
                            if (name.equals("Content-Length", ignoreCase = true)) {
                                contentLength = value.toIntOrNull() ?: 0
                            }
                        }

                    if (index == 0) {
                        val chars = CharArray(contentLength)
                        reader.read(chars)
                        firstBody.set(String(chars))
                    } else {
                        assertEquals("GET /api/mobile/me HTTP/1.1", requestLine)
                        secondDeviceId.set(headers["x-dlaflow-device-id"].orEmpty())
                    }

                    val body = if (index == 0) {
                        """{"data":{"token":"mobile-token","device":{"id":"paired-device-7","name":"Telefon"}}}"""
                    } else {
                        """{"data":{"device":{"id":"paired-device-7","name":"Telefon"},"tenant":{"name":"Firma"},"user":{"email":"mobile@example.test"}}}"""
                    }.toByteArray(Charsets.UTF_8)
                    val responseHeaders = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                    socket.getOutputStream().use { output ->
                        output.write(responseHeaders.toByteArray(Charsets.UTF_8))
                        output.write(body)
                    }
                }
            }
        }

        try {
            val client = MobileApiClient(
                baseUrl = "http://127.0.0.1:${server.localPort}",
                requestSigner = signer,
                nowMillis = { 1_783_540_000_123L },
                nonceFactory = { "nonce-pairing-123" },
                appVersionCode = 16,
                appVersionName = "0.4.2",
            )

            val session = client.completePairing("ABC-123", "Magazyn")

            val pairingBody = JSONObject(firstBody.get())
            assertEquals("Magazyn", pairingBody.getString("deviceName"))
            assertEquals(16, pairingBody.getInt("appVersionCode"))
            assertEquals("0.4.2", pairingBody.getString("appVersionName"))
            assertEquals(
                "PUBLIC_KEY_BASE64_VALUE_WITH_ENOUGH_LENGTH_1234567890",
                pairingBody.getString("requestSigningPublicKey"),
            )
            assertEquals("paired-device-7", secondDeviceId.get())
            assertEquals("paired-device-7", session.deviceId)
            assertEquals("mobile-token", session.token)
        } finally {
            server.close()
            executor.shutdownNow()
        }
    }

    private class FakeMobileRequestSigner(
        private val publicKey: String = "PUBLIC_KEY_BASE64_VALUE_WITH_ENOUGH_LENGTH",
    ) : MobileRequestSigner {
        var lastCanonical: String = ""

        override fun publicKeySpkiBase64(): String = publicKey

        override fun sign(canonical: String): String {
            lastCanonical = canonical
            return "fake-signature"
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
