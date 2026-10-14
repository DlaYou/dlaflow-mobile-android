package pl.dlaflow.mobile.feature.products

import pl.dlaflow.mobile.MobilePhotoTask
import pl.dlaflow.mobile.MobilePhotoTaskMedia
import pl.dlaflow.mobile.MobilePhotoTaskDispatch

internal class InvalidPhotoTaskPayloadException : IllegalArgumentException()

internal fun MobilePhotoTask.toProductPhotoTask(expectedTaskId: String? = null): ProductPhotoTask {
    if (
        id.isBlank() ||
        (expectedTaskId != null && id != expectedTaskId) ||
        maxPhotos < 1 ||
        mediaCount !in 0..maxPhotos ||
        media.size > maxPhotos ||
        media.any { it.id.isBlank() || it.progress !in 0..100 } ||
        media.map(MobilePhotoTaskMedia::id).distinct().size != media.size
    ) {
        throw InvalidPhotoTaskPayloadException()
    }
    return ProductPhotoTask(
        id = id,
        productName = productName,
        productSku = productSku,
        status = status.toPhotoTaskStatus(),
        mediaCount = mediaCount,
        maxPhotos = maxPhotos,
        expiresAt = expiresAt,
        media = media.map { it.toProductPhotoTaskMedia() },
    )
}

internal fun List<MobilePhotoTask>.toActiveProductPhotoTasks(): List<ProductPhotoTask> {
    val mapped = map { it.toProductPhotoTask() }
    if (mapped.map(ProductPhotoTask::id).distinct().size != mapped.size) {
        throw InvalidPhotoTaskPayloadException()
    }
    return mapped
}

internal fun MobilePhotoTaskDispatch.toDispatchedPhotoTask(): ProductPhotoTask? =
    pendingOpenTask?.toProductPhotoTask()

private fun String.toPhotoTaskStatus(): PhotoTaskStatus = when (trim().lowercase()) {
    "pending" -> PhotoTaskStatus.PENDING
    "in_progress", "in-progress", "active" -> PhotoTaskStatus.IN_PROGRESS
    "completed", "complete", "done" -> PhotoTaskStatus.COMPLETED
    else -> PhotoTaskStatus.UNKNOWN
}

private fun MobilePhotoTaskMedia.toProductPhotoTaskMedia(): ProductPhotoTaskMedia = ProductPhotoTaskMedia(
    id = id,
    filename = filename.take(128),
    status = status.toPhotoTaskMediaStatus(),
    progress = progress.coerceIn(0, 100),
    previewUrl = previewUrl.toSafePhotoTaskMediaUrl(),
    url = url.toSafePhotoTaskMediaUrl(),
    errorCode = errorCode?.trim()?.takeIf { it.length <= 128 && it.matches(SAFE_ERROR_CODE) },
)

private fun String.toPhotoTaskMediaStatus(): PhotoTaskMediaStatus = when (trim().uppercase()) {
    "STAGED" -> PhotoTaskMediaStatus.STAGED
    "PREVIEW_READY", "PREVIEW-READY" -> PhotoTaskMediaStatus.PREVIEW_READY
    "READY" -> PhotoTaskMediaStatus.READY
    "FAILED" -> PhotoTaskMediaStatus.FAILED
    else -> PhotoTaskMediaStatus.UNKNOWN
}

private fun String?.toSafePhotoTaskMediaUrl(): String? {
    val value = this?.trim()?.takeIf { it.isNotBlank() && it.length <= 2048 } ?: return null
    return value.takeIf {
        it.startsWith("/api/products/media/") || it.startsWith("/api/mobile/products/media/")
    }
}

private val SAFE_ERROR_CODE = Regex("[A-Za-z0-9_.-]+")
