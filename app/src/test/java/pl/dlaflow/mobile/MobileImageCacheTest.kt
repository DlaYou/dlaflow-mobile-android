package pl.dlaflow.mobile

import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileImageCacheTest {
    @Test
    fun `fresh media is reused from memory and variants remain separate`() {
        val root = Files.createTempDirectory("mobile-media-cache").toFile()
        val requests = AtomicInteger(0)
        val cache = MobileImageCache(
            cacheDir = root,
            sessionScope = "session-a",
            nowMillis = { 1_000L },
            diskMaxBytes = 0,
        )

        try {
            val load: (String?) -> MobileMediaResponse? = {
                requests.incrementAndGet()
                MobileMediaResponse(byteArrayOf(1, 2), "\"v1\"", 60_000L, false)
            }

            assertEquals(listOf<Byte>(1, 2), cache.get("/api/mobile/products/media/a.webp?variant=thumb", load)?.toList())
            assertEquals(listOf<Byte>(1, 2), cache.get("/api/mobile/products/media/a.webp?variant=thumb", load)?.toList())
            assertEquals(listOf<Byte>(1, 2), cache.get("/api/mobile/products/media/a.webp?variant=original", load)?.toList())
            assertEquals(2, requests.get())
            assertEquals(1L, cache.stats().hits)
            assertEquals(2L, cache.stats().misses)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `concurrent requests for one key share one network call`() {
        val root = Files.createTempDirectory("mobile-media-cache").toFile()
        val requests = AtomicInteger(0)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val cache = MobileImageCache(root, "session-a", diskMaxBytes = 0)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val load: (String?) -> MobileMediaResponse? = {
                requests.incrementAndGet()
                started.countDown()
                assertTrue(release.await(2, TimeUnit.SECONDS))
                MobileMediaResponse(byteArrayOf(4, 5), null, 60_000L, false)
            }
            val first = executor.submit<ByteArray?> { cache.get("/api/mobile/orders/media/a.webp?variant=thumb", load) }
            assertTrue(started.await(2, TimeUnit.SECONDS))
            val second = executor.submit<ByteArray?> { cache.get("/api/mobile/orders/media/a.webp?variant=thumb", load) }
            release.countDown()

            assertEquals(listOf<Byte>(4, 5), first.get(2, TimeUnit.SECONDS)?.toList())
            assertEquals(listOf<Byte>(4, 5), second.get(2, TimeUnit.SECONDS)?.toList())
            assertEquals(1, requests.get())
        } finally {
            release.countDown()
            executor.shutdownNow()
            root.deleteRecursively()
        }
    }

    @Test
    fun `failed network response is not cached`() {
        val root = Files.createTempDirectory("mobile-media-cache").toFile()
        val requests = AtomicInteger(0)
        val cache = MobileImageCache(root, "session-a", diskMaxBytes = 0)

        try {
            assertNull(cache.get("/api/mobile/products/media/a.webp?variant=thumb", {
                requests.incrementAndGet()
                null
            }))
            assertEquals(listOf<Byte>(7), cache.get("/api/mobile/products/media/a.webp?variant=thumb", {
                requests.incrementAndGet()
                MobileMediaResponse(byteArrayOf(7), null, 60_000L, false)
            })?.toList())
            assertEquals(2, requests.get())
            assertEquals(1L, cache.stats().failures)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `disk cache serves fresh entry and revalidates stale entry`() {
        val root = Files.createTempDirectory("mobile-media-cache").toFile()
        var now = 1_000L
        val path = "/api/mobile/products/media/a.webp?variant=thumb"
        val writer = MobileImageCache(root, "session-a", nowMillis = { now })

        try {
            writer.get(path) {
                MobileMediaResponse(byteArrayOf(8, 9), "\"v1\"", 1_000L, false)
            }

            now = 1_500L
            val freshReaderRequests = AtomicInteger(0)
            val freshReader = MobileImageCache(root, "session-a", nowMillis = { now })
            assertEquals(listOf<Byte>(8, 9), freshReader.get(path) {
                freshReaderRequests.incrementAndGet()
                null
            }?.toList())
            assertEquals(0, freshReaderRequests.get())

            now = 3_000L
            val staleReader = MobileImageCache(root, "session-a", nowMillis = { now })
            assertEquals(listOf<Byte>(8, 9), staleReader.get(path) { etag ->
                assertEquals("\"v1\"", etag)
                MobileMediaResponse(null, "\"v1\"", 2_000L, true)
            }?.toList())
            assertEquals(1L, staleReader.stats().revalidations)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `different session scope never reads another session disk entry`() {
        val root = Files.createTempDirectory("mobile-media-cache").toFile()
        val path = "/api/mobile/products/media/a.webp?variant=thumb"
        val writer = MobileImageCache(root, "session-a", nowMillis = { 1_000L })
        val requests = AtomicInteger(0)

        try {
            writer.get(path) { MobileMediaResponse(byteArrayOf(3), null, 60_000L, false) }
            val reader = MobileImageCache(root, "session-b", nowMillis = { 1_000L })
            assertNotNull(reader.get(path) {
                requests.incrementAndGet()
                MobileMediaResponse(byteArrayOf(6), null, 60_000L, false)
            })
            assertEquals(1, requests.get())
            assertEquals(listOf<Byte>(6), reader.get(path) { error("memory entry should be reused") }?.toList())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `memory cache evicts least recently used bytes`() {
        val root = Files.createTempDirectory("mobile-media-cache").toFile()
        val requests = AtomicInteger(0)
        val cache = MobileImageCache(
            cacheDir = root,
            sessionScope = "session-a",
            memoryMaxBytes = 4,
            memoryMaxEntries = 2,
            diskMaxBytes = 0,
        )

        try {
            fun load(bytes: ByteArray): (String?) -> MobileMediaResponse? = {
                requests.incrementAndGet()
                MobileMediaResponse(bytes, null, 60_000L, false)
            }
            cache.get("/api/mobile/products/media/a.webp?variant=thumb", load(byteArrayOf(1, 2, 3)))
            cache.get("/api/mobile/products/media/b.webp?variant=thumb", load(byteArrayOf(4, 5, 6)))
            assertEquals(listOf<Byte>(1, 2, 3), cache.get("/api/mobile/products/media/a.webp?variant=thumb", load(byteArrayOf(1, 2, 3)))?.toList())
            assertEquals(listOf<Byte>(4, 5, 6), cache.get("/api/mobile/products/media/b.webp?variant=thumb", load(byteArrayOf(4, 5, 6)))?.toList())
            assertEquals(4, requests.get())
        } finally {
            root.deleteRecursively()
        }
    }
}
