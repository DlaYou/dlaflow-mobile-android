package pl.dlaflow.mobile.feature.products

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoGalleryBatchUploadCoordinatorTest {
    @Test
    fun `starts selected photos before the first transfer completes`() {
        val executor = Executors.newFixedThreadPool(3)
        val started = CountDownLatch(3)
        val completed = CountDownLatch(1)
        val accepted = mutableListOf<String>()
        try {
            val coordinator = PhotoGalleryBatchUploadCoordinator(
                items = listOf("one", "two", "three"),
                executor = executor,
                maxConcurrency = 3,
                postToMain = { it() },
                upload = { item ->
                    started.countDown()
                    Thread.sleep(75)
                    item
                },
                onAccepted = { item, _, _ -> synchronized(accepted) { accepted += item } },
                onComplete = { completed.countDown() },
            )

            coordinator.start()

            assertTrue("all selected photos should start before the first completes", started.await(2, TimeUnit.SECONDS))
            assertTrue(completed.await(2, TimeUnit.SECONDS))
            assertEquals(setOf("one", "two", "three"), accepted.toSet())
            assertEquals(PhotoGalleryBatchUploadProgress(3, 3, 0), coordinator.progress)
        } finally {
            shutdown(executor)
        }
    }

    @Test
    fun `retries one failed photo three times and continues with the rest`() {
        val executor = Executors.newFixedThreadPool(2)
        val completed = CountDownLatch(1)
        val attempts = ConcurrentHashMap<String, AtomicInteger>()
        try {
            val coordinator = PhotoGalleryBatchUploadCoordinator(
                items = listOf("bad", "good"),
                executor = executor,
                maxConcurrency = 2,
                postToMain = { it() },
                upload = { item ->
                    val attempt = attempts.computeIfAbsent(item) { AtomicInteger() }.incrementAndGet()
                    if (item == "bad") error("synthetic failure $attempt")
                    item
                },
                onComplete = { completed.countDown() },
            )

            coordinator.start()

            assertTrue(completed.await(2, TimeUnit.SECONDS))
            assertEquals(3, attempts.getValue("bad").get())
            assertEquals(1, attempts.getValue("good").get())
            assertEquals(PhotoGalleryBatchUploadProgress(2, 1, 1), coordinator.progress)
        } finally {
            shutdown(executor)
        }
    }

    @Test
    fun `does not retry rejected photo`() {
        val executor = Executors.newFixedThreadPool(1)
        val completed = CountDownLatch(1)
        val attempts = AtomicInteger()
        try {
            val coordinator = PhotoGalleryBatchUploadCoordinator(
                items = listOf("rejected"),
                executor = executor,
                postToMain = { it() },
                upload = { attempts.incrementAndGet(); throw RejectedPhotoUpload },
                isRetryable = { it !== RejectedPhotoUpload },
                onComplete = { completed.countDown() },
            )

            coordinator.start()

            assertTrue(completed.await(2, TimeUnit.SECONDS))
            assertEquals(1, attempts.get())
            assertEquals(PhotoGalleryBatchUploadProgress(1, 0, 1), coordinator.progress)
        } finally {
            shutdown(executor)
        }
    }

    private fun shutdown(executor: ExecutorService) {
        executor.shutdownNow()
        executor.awaitTermination(1, TimeUnit.SECONDS)
    }

    private object RejectedPhotoUpload : Exception()
}
