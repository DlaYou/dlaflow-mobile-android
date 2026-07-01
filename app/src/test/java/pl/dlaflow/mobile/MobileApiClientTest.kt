package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class MobileApiClientTest {
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
}
