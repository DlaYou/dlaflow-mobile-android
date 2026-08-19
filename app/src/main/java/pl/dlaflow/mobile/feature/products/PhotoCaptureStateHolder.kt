package pl.dlaflow.mobile.feature.products

internal enum class PhotoCaptureKind {
    CAMERA,
    GALLERY,
}

internal enum class PhotoCapturePhase {
    LAUNCH_PENDING,
    PREPARING,
}

internal data class PendingPhotoCapture(
    val sessionKey: String,
    val taskId: String,
    val kind: PhotoCaptureKind,
    val sourceId: String,
    val phase: PhotoCapturePhase = PhotoCapturePhase.LAUNCH_PENDING,
)

/** Keeps camera/gallery callbacks bound to the session and source that created them. */
internal class PhotoCaptureStateHolder {
    var pending: PendingPhotoCapture? = null
        private set

    fun begin(
        sessionKey: String,
        taskId: String,
        kind: PhotoCaptureKind,
        sourceId: String,
    ): PendingPhotoCapture? {
        if (pending != null || sessionKey.isBlank() || taskId.isBlank() || sourceId.isBlank()) return null
        return PendingPhotoCapture(sessionKey, taskId, kind, sourceId).also { pending = it }
    }

    fun matches(candidate: PendingPhotoCapture): Boolean = pending == candidate

    fun advance(candidate: PendingPhotoCapture, phase: PhotoCapturePhase): Boolean {
        if (!matches(candidate)) return false
        pending = candidate.copy(phase = phase)
        return true
    }

    fun clear(sourceId: String): Boolean {
        if (pending?.sourceId != sourceId) return false
        pending = null
        return true
    }

    fun reset() {
        pending = null
    }
}
