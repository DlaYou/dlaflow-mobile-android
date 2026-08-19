package pl.dlaflow.mobile

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import pl.dlaflow.mobile.feature.products.PhotoUploadSource

class MobilePhotoUploadPreparationTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `preparation creates repeatable bounded private source and sanitizes metadata`() {
        val destination = temporaryFolder.newFile("prepared-photo")
        val payload = "synthetic-photo".toByteArray()
        val result = prepareMobilePhotoUpload(ByteArrayInputStream(payload), destination, "capture-1", "photo\"\r\nunsafe.jpg", "IMAGE/JPEG")
        val source = (result as MobilePhotoUploadPreparationResult.Ready).source
        assertEquals(payload.size.toLong(), source.byteCount)
        assertEquals("photo___unsafe.jpg", source.safeFileName)
        assertEquals("image/jpeg", source.safeMimeType)
        assertTrue(source.openStream().use { it.readBytes().contentEquals(payload) })
        assertTrue(source.openStream().use { it.readBytes().contentEquals(payload) })
        source.dispose()
        source.dispose()
        assertFalse(destination.exists())
        assertThrows(IllegalStateException::class.java) { source.openStream() }
    }

    @Test
    fun `prepared source implements upload contract and accepts heic and heif`() {
        listOf("image/heic", "image/heif").forEach { mimeType ->
            val destination = temporaryFolder.newFile("prepared-${mimeType.substringAfter('/')}")
            val result = prepareMobilePhotoUpload(
                ByteArrayInputStream(byteArrayOf(1, 2, 3)),
                destination,
                "capture-$mimeType",
                "photo.heic",
                mimeType,
            )

            val source = (result as MobilePhotoUploadPreparationResult.Ready).source
            assertTrue(source is PhotoUploadSource)
            assertEquals(mimeType, source.safeMimeType)
            (source as PhotoUploadSource).dispose()
        }
    }

    @Test
    fun `preparation rejects empty and oversized content and accepts exact limit`() {
        val emptyFile = temporaryFolder.newFile("empty")
        assertTrue(prepareMobilePhotoUpload(ByteArrayInputStream(ByteArray(0)), emptyFile, "a", "", "image/jpeg") is MobilePhotoUploadPreparationResult.Empty)
        assertFalse(emptyFile.exists())

        val oversizedFile = temporaryFolder.newFile("oversized")
        assertTrue(prepareMobilePhotoUpload(RepeatingInputStream(MOBILE_PHOTO_UPLOAD_MAX_BYTES + 1), oversizedFile, "b", "photo.jpg", "image/jpeg") is MobilePhotoUploadPreparationResult.TooLarge)
        assertFalse(oversizedFile.exists())

        val limitFile = temporaryFolder.newFile("limit")
        val ready = prepareMobilePhotoUpload(RepeatingInputStream(MOBILE_PHOTO_UPLOAD_MAX_BYTES), limitFile, "c", "photo.jpg", "image/jpeg") as MobilePhotoUploadPreparationResult.Ready
        assertEquals(MOBILE_PHOTO_UPLOAD_MAX_BYTES, ready.source.byteCount)
        ready.source.dispose()
        assertFalse(limitFile.exists())
    }

    @Test
    fun `invalid file metadata falls back without header injection`() {
        val destination = temporaryFolder.newFile("fallback")
        val ready = prepareMobilePhotoUpload(ByteArrayInputStream(byteArrayOf(1)), destination, "d", "", "text/plain\r\nInjected: yes") as MobilePhotoUploadPreparationResult.Ready
        assertEquals("zdjecie-z-telefonu.jpg", ready.source.safeFileName)
        assertEquals("application/octet-stream", ready.source.safeMimeType)
        ready.source.dispose()
    }
}

private class RepeatingInputStream(private var remaining: Long) : java.io.InputStream() {
    override fun read(): Int = if (remaining-- > 0) 7 else -1
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (remaining <= 0) return -1
        val count = minOf(remaining, length.toLong()).toInt()
        java.util.Arrays.fill(buffer, offset, offset + count, 7.toByte())
        remaining -= count
        return count
    }
}
