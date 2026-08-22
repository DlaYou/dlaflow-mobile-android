package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileImagePrefetchTest {
    @Test
    fun `prefetch keeps only six unique thumbnail variants`() {
        val urls = buildList {
            add("/api/mobile/products/media/first.webp?variant=thumb")
            add("/api/mobile/products/media/first.webp?variant=thumb")
            add("/api/mobile/products/media/original.webp?variant=original")
            repeat(7) { index ->
                add("/api/mobile/products/media/$index.webp?variant=thumb")
            }
        }

        val selected = mobileThumbnailPrefetchUrls(urls)

        assertEquals(6, selected.size)
        assertEquals(selected.distinct(), selected)
        assertTrue(selected.all { it.contains("variant=thumb") })
        assertEquals("/api/mobile/products/media/first.webp?variant=thumb", selected.first())
    }

    @Test
    fun `prefetch can be disabled with a nonpositive limit`() {
        assertEquals(emptyList<String>(), mobileThumbnailPrefetchUrls(listOf("/api/mobile/products/media/a.webp?variant=thumb"), 0))
    }
}
