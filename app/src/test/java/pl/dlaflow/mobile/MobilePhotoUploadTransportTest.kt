package pl.dlaflow.mobile

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilePhotoUploadTransportTest {
    @Test
    fun `photo upload hashes and streams repeatable source with encoded task path`() {
        val payload = "synthetic-image-content".toByteArray()
        val opens = AtomicInteger(0)
        val requestLine = AtomicReference("")
        val requestHeaders = AtomicReference<Map<String, String>>(emptyMap())
        val requestBody = AtomicReference(ByteArray(0))
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newSingleThreadExecutor()
        val responseFuture = executor.submit {
            server.accept().use { socket ->
                val input = socket.getInputStream()
                requestLine.set(readAsciiLine(input))
                val headers = linkedMapOf<String, String>()
                while (true) {
                    val line = readAsciiLine(input)
                    if (line.isEmpty()) break
                    headers[line.substringBefore(":").lowercase()] = line.substringAfter(":").trim()
                }
                requestHeaders.set(headers)
                requestBody.set(input.readNBytes(headers["content-length"]?.toInt() ?: 0))
                val response = """{"data":{"task":{"id":"task-1","productName":"Test","productSku":"SKU","status":"pending","mediaCount":1,"maxPhotos":3,"expiresAt":""}}}""".toByteArray()
                socket.getOutputStream().use { output ->
                    output.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${response.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    output.write(response)
                }
            }
        }
        try {
            val client = MobileApiClient("http://127.0.0.1:${server.localPort}")
            val source = MobilePhotoUploadSource(payload.size.toLong()) {
                opens.incrementAndGet()
                ByteArrayInputStream(payload)
            }
            client.uploadPhotoTaskMedia("mobile-token", "task /1", source, "photo\"\r\nInjected: yes.jpg", "image/jpeg\r\nX-Injected: yes")
            responseFuture.get(2, TimeUnit.SECONDS)
            val expectedHash = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }
            assertEquals(2, opens.get())
            assertEquals("POST /api/mobile/photo-tasks/task%20%2F1/media HTTP/1.1", requestLine.get())
            assertEquals(expectedHash, requestHeaders.get()["x-dlaflow-file-sha256"])
            assertEquals(requestBody.get().size.toString(), requestHeaders.get()["content-length"])
            assertTrue(requestBody.get().toString(Charsets.ISO_8859_1).contains("filename=\"photo___Injected_ yes.jpg\""))
            assertTrue(requestBody.get().toString(Charsets.ISO_8859_1).contains("Content-Type: application/octet-stream\r\n"))
            assertTrue(requestBody.get().containsSubsequence(payload))
        } finally {
            server.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `photo upload rejects oversized source before opening stream`() {
        val opens = AtomicInteger(0)
        val client = MobileApiClient("http://127.0.0.1:1")
        assertThrows(IllegalArgumentException::class.java) {
            client.uploadPhotoTaskMedia("mobile-token", "task-1", MobilePhotoUploadSource(MOBILE_PHOTO_UPLOAD_MAX_BYTES + 1) { opens.incrementAndGet(); ByteArrayInputStream(ByteArray(0)) }, "photo.jpg", "image/jpeg")
        }
        assertEquals(0, opens.get())
    }

    @Test
    fun `photo upload rejects short and long source lengths`() {
        val client = MobileApiClient("http://127.0.0.1:1")
        listOf(4L to byteArrayOf(1, 2, 3), 2L to byteArrayOf(1, 2, 3)).forEach { (declared, bytes) ->
            val opens = AtomicInteger(0)
            assertThrows(IllegalArgumentException::class.java) {
                client.uploadPhotoTaskMedia("mobile-token", "task-1", MobilePhotoUploadSource(declared) { opens.incrementAndGet(); ByteArrayInputStream(bytes) }, "photo.jpg", "image/jpeg")
            }
            assertEquals(1, opens.get())
        }
    }

    private fun readAsciiLine(input: InputStream): String {
        val output = StringBuilder()
        while (true) {
            val value = input.read()
            if (value < 0 || value == '\n'.code) break
            if (value != '\r'.code) output.append(value.toChar())
        }
        return output.toString()
    }

    private fun ByteArray.containsSubsequence(expected: ByteArray): Boolean = indices.any { start ->
        start + expected.size <= size && expected.indices.all { offset -> this[start + offset] == expected[offset] }
    }
}
