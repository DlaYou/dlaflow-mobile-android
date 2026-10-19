package pl.dlaflow.mobile.feature.products

import java.util.ArrayDeque
import java.util.concurrent.Executor

internal const val PHOTO_GALLERY_UPLOAD_MAX_CONCURRENCY = 4
internal const val PHOTO_UPLOAD_MAX_ATTEMPTS = 3

internal data class PhotoGalleryBatchUploadProgress(
    val total: Int,
    val accepted: Int,
    val failed: Int,
) {
    val finished: Int get() = accepted + failed
}

/** Uploads selected gallery items independently while bounding active network transfers. */
internal class PhotoGalleryBatchUploadCoordinator<T, R>(
    items: List<T>,
    private val executor: Executor,
    private val maxConcurrency: Int = PHOTO_GALLERY_UPLOAD_MAX_CONCURRENCY,
    private val maxAttempts: Int = PHOTO_UPLOAD_MAX_ATTEMPTS,
    private val retryDelayMs: Long = 500,
    private val postToMain: ((() -> Unit) -> Unit),
    private val upload: (T) -> R,
    private val isRetryable: (Throwable) -> Boolean = { true },
    private val onAccepted: (T, R, PhotoGalleryBatchUploadProgress) -> Unit = { _, _, _ -> },
    private val onFailed: (T, Int, Throwable, PhotoGalleryBatchUploadProgress) -> Unit = { _, _, _, _ -> },
    private val onComplete: (PhotoGalleryBatchUploadProgress) -> Unit = {},
) {
    private val queue = ArrayDeque(items.distinct())
    private val lock = Any()
    private val total = queue.size
    private var accepted = 0
    private var failed = 0
    private var workers = 0
    private var started = false
    private var cancelled = false
    private var completed = false

    init {
        require(maxConcurrency > 0)
        require(maxAttempts > 0)
        require(retryDelayMs >= 0)
    }

    val progress: PhotoGalleryBatchUploadProgress
        get() = synchronized(lock) { currentProgress() }

    fun start() {
        val workerCount: Int
        synchronized(lock) {
            check(!started) { "Batch upload has already started." }
            started = true
            if (queue.isEmpty()) {
                completed = true
                postToMain { onComplete(currentProgress()) }
                return
            }
            workerCount = minOf(maxConcurrency, queue.size)
            workers = workerCount
        }
        repeat(workerCount) { executor.execute(::runWorker) }
    }

    fun cancel() {
        synchronized(lock) {
            cancelled = true
            queue.clear()
        }
    }

    private fun runWorker() {
        while (true) {
            val item = synchronized(lock) {
                if (cancelled || queue.isEmpty()) null else queue.removeFirst()
            } ?: break
            uploadItem(item)
        }

        val shouldComplete = synchronized(lock) {
            workers -= 1
            workers == 0 && !cancelled && !completed
        }
        if (shouldComplete) {
            synchronized(lock) { completed = true }
            postToMain { onComplete(progress) }
        }
    }

    private fun uploadItem(item: T) {
        var attempt = 0
        while (attempt < maxAttempts) {
            attempt += 1
            if (synchronized(lock) { cancelled }) return
            try {
                val result = upload(item)
                val nextProgress = synchronized(lock) {
                    if (cancelled) return
                    accepted += 1
                    currentProgress()
                }
                postToMain { onAccepted(item, result, nextProgress) }
                return
            } catch (error: Exception) {
                if (!isRetryable(error) || attempt >= maxAttempts) {
                    val nextProgress = synchronized(lock) {
                        if (cancelled) return
                        failed += 1
                        currentProgress()
                    }
                    postToMain { onFailed(item, attempt, error, nextProgress) }
                    return
                }
                if (retryDelayMs > 0) {
                    try {
                        Thread.sleep(retryDelayMs)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return
                    }
                }
            }
        }
    }

    private fun currentProgress() = PhotoGalleryBatchUploadProgress(
        total = total,
        accepted = accepted,
        failed = failed,
    )
}
