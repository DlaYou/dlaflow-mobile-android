package pl.dlaflow.mobile

import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import pl.dlaflow.mobile.feature.products.PhotoUploadSource

internal sealed interface MobilePhotoUploadPreparationResult {
    data class Ready(val source: PreparedPhotoUploadSource) : MobilePhotoUploadPreparationResult
    data object Empty : MobilePhotoUploadPreparationResult
    data object TooLarge : MobilePhotoUploadPreparationResult
    data object Unavailable : MobilePhotoUploadPreparationResult
}

internal fun prepareMobilePhotoUpload(
    input: InputStream,
    destination: File,
    sourceId: String,
    fileName: String,
    mimeType: String,
): MobilePhotoUploadPreparationResult {
    destination.parentFile?.mkdirs()
    var total = 0L
    return try {
        destination.outputStream().buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                total += read
                if (total > MOBILE_PHOTO_UPLOAD_MAX_BYTES) return@use
                output.write(buffer, 0, read)
            }
        }
        when {
            total == 0L -> MobilePhotoUploadPreparationResult.Empty.also { destination.delete() }
            total > MOBILE_PHOTO_UPLOAD_MAX_BYTES -> MobilePhotoUploadPreparationResult.TooLarge.also { destination.delete() }
            else -> MobilePhotoUploadPreparationResult.Ready(
                PreparedPhotoUploadSource(
                    sourceId = sourceId,
                    file = destination,
                    lengthBytes = total,
                    safeFileName = sanitizePhotoFileName(fileName),
                    safeMimeType = sanitizePhotoMimeType(mimeType),
                ),
            )
        }
    } catch (_: Exception) {
        destination.delete()
        MobilePhotoUploadPreparationResult.Unavailable
    }
}

internal class PreparedPhotoUploadSource(
    override val sourceId: String,
    private val file: File,
    override val lengthBytes: Long,
    override val safeFileName: String,
    override val safeMimeType: String,
) : PhotoUploadSource {
    private val disposed = AtomicBoolean(false)

    override fun openStream(): InputStream {
        check(!disposed.get()) { "Prepared photo upload source is no longer available." }
        return file.inputStream().buffered()
    }

    override fun dispose() {
        if (disposed.compareAndSet(false, true)) file.delete()
    }

    val byteCount: Long get() = lengthBytes
}

private fun sanitizePhotoFileName(value: String): String = value
    .trim()
    .take(128)
    .replace(Regex("[\\r\\n\\\"\\\\/:;]"), "_")
    .filter { it.code in 32..126 }
    .trim()
    .ifBlank { "zdjecie-z-telefonu.jpg" }

private fun sanitizePhotoMimeType(value: String): String = value
    .trim()
    .lowercase()
    .takeIf { it.matches(Regex("image/[a-z0-9][a-z0-9.+-]{0,63}")) }
    ?: "application/octet-stream"
