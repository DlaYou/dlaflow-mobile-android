package pl.dlaflow.mobile.feature.products

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoGallerySelectionTest {
    @Test
    fun `gallery picker requests multiple image selection`() {
        val source = File("src/main/java/pl/dlaflow/mobile/feature/products/PhotoGallerySelection.kt").readText()

        assertTrue(source.contains("putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)"))
    }

    @Test
    fun `extracts multiple gallery uris in provider order and removes duplicates`() {
        assertEquals(
            listOf("content://photos/1", "content://photos/2", "content://photos/3"),
            galleryUriStrings(
                primary = "content://photos/1",
                clipUris = listOf("content://photos/1", "content://photos/2", "content://photos/1", "content://photos/3"),
            ),
        )
    }

    @Test
    fun `limits selected uris to remaining product photo slots`() {
        val selected = (1..5).map { "content://photos/$it" }

        assertEquals(selected.take(3), limitGalleryUris(selected, remainingSlots = 3))
        assertEquals(emptyList<String>(), limitGalleryUris(selected, remainingSlots = 0))
    }

    @Test
    fun `upload queue exposes one item at a time and tracks successful uploads`() {
        val queue = PhotoGalleryUploadQueue(listOf("one", "two", "three"))

        assertEquals(3, queue.totalCount)
        assertEquals("one", queue.takeNext())
        assertEquals(2, queue.remainingCount)
        queue.markSucceeded()
        assertEquals(1, queue.completedCount)
        assertEquals("two", queue.takeNext())
        assertEquals("three", queue.takeNext())
        assertEquals(null, queue.takeNext())
    }
}
