package pl.dlaflow.mobile.feature.products

import pl.dlaflow.mobile.MobilePhotoTask
import pl.dlaflow.mobile.MobilePhotoTaskDispatch

internal class InvalidPhotoTaskPayloadException : IllegalArgumentException()

internal fun MobilePhotoTask.toProductPhotoTask(expectedTaskId: String? = null): ProductPhotoTask {
    if (
        id.isBlank() ||
        (expectedTaskId != null && id != expectedTaskId) ||
        maxPhotos < 1 ||
        mediaCount !in 0..maxPhotos
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
