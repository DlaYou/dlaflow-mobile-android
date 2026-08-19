package pl.dlaflow.mobile.feature.products

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

internal class PhotoTasksCoordinator(
    private val stateHolder: PhotoTasksStateHolder,
    private val gateway: PhotoTasksGateway,
    private val executor: Executor,
    private val postToMain: (() -> Unit) -> Unit,
    private val onEffect: (PhotoTasksEffect) -> Unit,
    private val onUnauthorized: (Throwable, Boolean, () -> Unit, () -> Unit) -> Unit,
    private val onTasksChanged: () -> Unit = {},
) {
    private val uploadSources = mutableMapOf<Long, UploadSourceOwner>()
    private val uploadSourcesLock = Any()

    fun setFallback(task: ProductPhotoTask?) = stateHolder.setFallback(task)

    fun focus(taskId: String?) = stateHolder.focus(taskId)

    fun refresh(token: String, allowUnauthorizedRetry: Boolean = true): Boolean {
        val request = stateHolder.beginRefresh(token) ?: return false
        executeRefresh(
            token = token,
            request = request,
            allowUnauthorizedRetry = allowUnauthorizedRetry,
        )
        return true
    }

    fun handleAction(token: String?, action: PhotoTasksAction) {
        when (action) {
            PhotoTasksAction.Refresh -> token?.let(::refresh)
            is PhotoTasksAction.Focus -> focus(action.taskId)
            is PhotoTasksAction.RequestCamera -> action.taskId.validTaskId()?.let { taskId ->
                if (stateHolder.beginMediaSelection(taskId)) {
                    onEffect(PhotoTasksEffect.LaunchCamera(taskId))
                }
            }
            is PhotoTasksAction.RequestGallery -> action.taskId.validTaskId()?.let { taskId ->
                if (stateHolder.beginMediaSelection(taskId)) {
                    onEffect(PhotoTasksEffect.LaunchGallery(taskId))
                }
            }
            is PhotoTasksAction.Complete -> token?.let { complete(it, action.taskId) }
        }
    }

    fun mediaSelectionCancelled() = stateHolder.cancelMediaSelection()

    fun restoreMediaSelection(taskId: String): Boolean =
        taskId.validTaskId()?.let(stateHolder::restoreMediaSelection) ?: false

    fun discardUploadSource(source: PhotoUploadSource) {
        UploadSourceOwner(source).dispose()
    }

    fun submitUpload(
        token: String,
        taskId: String,
        source: PhotoUploadSource,
        allowUnauthorizedRetry: Boolean = true,
    ): Boolean {
        val owner = UploadSourceOwner(source)
        val normalizedTaskId = taskId.validTaskId()
        if (normalizedTaskId == null || !source.isValid()) {
            stateHolder.rejectUpload(normalizedTaskId, invalidPhotoUploadMessage())
            owner.dispose()
            return false
        }

        val request = if (stateHolder.state.preparingMediaTaskId != null) {
            stateHolder.beginPreparedUpload(token, normalizedTaskId, source.sourceId)
        } else {
            stateHolder.beginUpload(token, normalizedTaskId, source.sourceId)
        }
        if (request == null) {
            if (stateHolder.state.preparingMediaTaskId != null) {
                stateHolder.rejectUpload(normalizedTaskId, invalidPhotoUploadMessage())
            }
            owner.dispose()
            return false
        }
        registerSource(request.requestId, owner)
        executeUpload(token, request, owner, allowUnauthorizedRetry)
        return true
    }

    fun complete(
        token: String,
        taskId: String,
        allowUnauthorizedRetry: Boolean = true,
    ): Boolean {
        val normalizedTaskId = taskId.validTaskId() ?: return false
        val request = stateHolder.beginCompletion(token, normalizedTaskId) ?: return false
        executeCompletion(token, request, allowUnauthorizedRetry)
        return true
    }

    fun pollDispatch(token: String, allowUnauthorizedRetry: Boolean = true): Boolean {
        val request = stateHolder.beginDispatch(token) ?: return false
        executeDispatch(token, request, allowUnauthorizedRetry)
        return true
    }

    fun reset() {
        releaseAllUploadSources()
        stateHolder.reset()
    }

    private fun executeRefresh(
        token: String,
        request: PhotoTasksRefreshRequest,
        allowUnauthorizedRetry: Boolean,
    ) {
        executor.execute {
            runCatching { gateway.loadActive(token) }
                .onSuccess { tasks ->
                    postToMain { stateHolder.acceptRefreshSuccess(request, tasks) }
                }
                .onFailure { error ->
                    postToMain { handleRefreshFailure(token, request, error, allowUnauthorizedRetry) }
                }
        }
    }

    private fun handleRefreshFailure(
        token: String,
        request: PhotoTasksRefreshRequest,
        error: Throwable,
        allowUnauthorizedRetry: Boolean,
    ) {
        when (val failure = mapPhotoTasksFailure(error)) {
            is PhotoTasksFailure.Unauthorized -> {
                val terminal = failure.message.takeIf { !allowUnauthorizedRetry }
                if (!stateHolder.acceptRefreshUnauthorized(request, terminal)) return
                if (allowUnauthorizedRetry) {
                    onUnauthorized(
                        error,
                        true,
                        {
                            stateHolder.beginRefreshUnauthorizedRetry(request)?.let { retry ->
                                executeRefresh(token, retry, allowUnauthorizedRetry = false)
                            }
                        },
                        {
                            stateHolder.acceptRefreshSessionUnconfirmed(
                                request,
                                photoTasksSessionUnconfirmedMessage(),
                            )
                        },
                    )
                }
            }
            is PhotoTasksFailure.NoAccess -> stateHolder.acceptRefreshNoAccess(request)
            is PhotoTasksFailure.Offline -> stateHolder.acceptRefreshOffline(request, failure.message)
            is PhotoTasksFailure.InvalidPayload -> stateHolder.acceptRefreshFailure(request, failure.message)
            is PhotoTasksFailure.Retryable -> stateHolder.acceptRefreshFailure(request, failure.message)
        }
    }

    private fun executeUpload(
        token: String,
        request: PhotoTaskUploadRequest,
        owner: UploadSourceOwner,
        allowUnauthorizedRetry: Boolean,
    ) {
        executor.execute {
            if (!isRegisteredSource(request.requestId, owner)) return@execute
            runCatching { gateway.upload(token, request.taskId, owner.source) }
                .onSuccess { task ->
                    postToMain {
                        val accepted = stateHolder.acceptUploadSuccess(request, task)
                        releaseSource(request.requestId, owner)
                        if (accepted) onTasksChanged()
                    }
                }
                .onFailure { error ->
                    postToMain { handleUploadFailure(token, request, owner, error, allowUnauthorizedRetry) }
                }
        }
    }

    private fun handleUploadFailure(
        token: String,
        request: PhotoTaskUploadRequest,
        owner: UploadSourceOwner,
        error: Throwable,
        allowUnauthorizedRetry: Boolean,
    ) {
        when (val failure = mapPhotoTasksFailure(error)) {
            is PhotoTasksFailure.Unauthorized -> {
                val terminal = failure.message.takeIf { !allowUnauthorizedRetry }
                if (!stateHolder.acceptUploadUnauthorized(request, terminal)) {
                    releaseSource(request.requestId, owner)
                    return
                }
                if (!allowUnauthorizedRetry) {
                    releaseSource(request.requestId, owner)
                    return
                }
                onUnauthorized(
                    error,
                    true,
                    {
                        val retry = stateHolder.beginUploadUnauthorizedRetry(request)
                        if (retry != null && transferSource(request.requestId, retry.requestId, owner)) {
                            executeUpload(token, retry, owner, allowUnauthorizedRetry = false)
                        }
                    },
                    {
                        if (
                            stateHolder.acceptUploadSessionUnconfirmed(
                                request,
                                photoTasksSessionUnconfirmedMessage(),
                            )
                        ) {
                            releaseSource(request.requestId, owner)
                        }
                    },
                )
            }
            is PhotoTasksFailure.NoAccess -> finishUploadFailure(request, owner, failure.message)
            is PhotoTasksFailure.Offline -> finishUploadFailure(request, owner, failure.message)
            is PhotoTasksFailure.InvalidPayload -> finishUploadFailure(request, owner, failure.message)
            is PhotoTasksFailure.Retryable -> finishUploadFailure(request, owner, failure.message)
        }
    }

    private fun finishUploadFailure(
        request: PhotoTaskUploadRequest,
        owner: UploadSourceOwner,
        message: pl.dlaflow.mobile.core.state.DlaFlowUiMessage,
    ) {
        stateHolder.acceptUploadFailure(request, message)
        releaseSource(request.requestId, owner)
    }

    private fun executeCompletion(
        token: String,
        request: PhotoTaskCompletionRequest,
        allowUnauthorizedRetry: Boolean,
    ) {
        executor.execute {
            runCatching { gateway.complete(token, request.taskId) }
                .onSuccess { task ->
                    postToMain {
                        if (stateHolder.acceptCompletionSuccess(request, task)) onTasksChanged()
                    }
                }
                .onFailure { error ->
                    postToMain { handleCompletionFailure(token, request, error, allowUnauthorizedRetry) }
                }
        }
    }

    private fun handleCompletionFailure(
        token: String,
        request: PhotoTaskCompletionRequest,
        error: Throwable,
        allowUnauthorizedRetry: Boolean,
    ) {
        when (val failure = mapPhotoTasksFailure(error)) {
            is PhotoTasksFailure.Unauthorized -> {
                val terminal = failure.message.takeIf { !allowUnauthorizedRetry }
                if (!stateHolder.acceptCompletionUnauthorized(request, terminal)) return
                if (allowUnauthorizedRetry) {
                    onUnauthorized(
                        error,
                        true,
                        {
                            stateHolder.beginCompletionUnauthorizedRetry(request)?.let { retry ->
                                executeCompletion(token, retry, allowUnauthorizedRetry = false)
                            }
                        },
                        {
                            stateHolder.acceptCompletionSessionUnconfirmed(
                                request,
                                photoTasksSessionUnconfirmedMessage(),
                            )
                        },
                    )
                }
            }
            is PhotoTasksFailure.NoAccess -> stateHolder.acceptCompletionFailure(request, failure.message)
            is PhotoTasksFailure.Offline -> stateHolder.acceptCompletionFailure(request, failure.message)
            is PhotoTasksFailure.InvalidPayload -> stateHolder.acceptCompletionFailure(request, failure.message)
            is PhotoTasksFailure.Retryable -> stateHolder.acceptCompletionFailure(request, failure.message)
        }
    }

    private fun executeDispatch(
        token: String,
        request: PhotoTaskDispatchRequest,
        allowUnauthorizedRetry: Boolean,
    ) {
        executor.execute {
            runCatching { gateway.loadDispatch(token) }
                .onSuccess { task ->
                    postToMain {
                        stateHolder.acceptDispatchSuccess(request, task)?.let {
                            onEffect(PhotoTasksEffect.PresentDispatch(it))
                        }
                    }
                }
                .onFailure { error ->
                    postToMain { handleDispatchFailure(token, request, error, allowUnauthorizedRetry) }
                }
        }
    }

    private fun handleDispatchFailure(
        token: String,
        request: PhotoTaskDispatchRequest,
        error: Throwable,
        allowUnauthorizedRetry: Boolean,
    ) {
        when (val failure = mapPhotoTasksFailure(error)) {
            is PhotoTasksFailure.Unauthorized -> {
                val terminal = failure.message.takeIf { !allowUnauthorizedRetry }
                if (!stateHolder.acceptDispatchUnauthorized(request, terminal)) return
                if (allowUnauthorizedRetry) {
                    onUnauthorized(
                        error,
                        true,
                        {
                            stateHolder.beginDispatchUnauthorizedRetry(request)?.let { retry ->
                                executeDispatch(token, retry, allowUnauthorizedRetry = false)
                            }
                        },
                        {
                            stateHolder.acceptDispatchSessionUnconfirmed(
                                request,
                                photoTasksSessionUnconfirmedMessage(),
                            )
                        },
                    )
                }
            }
            is PhotoTasksFailure.NoAccess -> stateHolder.acceptDispatchFailure(request, failure.message)
            is PhotoTasksFailure.Offline -> stateHolder.acceptDispatchFailure(request, failure.message)
            is PhotoTasksFailure.InvalidPayload -> stateHolder.acceptDispatchFailure(request, failure.message)
            is PhotoTasksFailure.Retryable -> stateHolder.acceptDispatchFailure(request, failure.message)
        }
    }

    private fun releaseAllUploadSources() {
        val sources = synchronized(uploadSourcesLock) {
            uploadSources.values.toList().also { uploadSources.clear() }
        }
        sources.forEach { it.dispose() }
    }

    private fun releaseSource(requestId: Long, owner: UploadSourceOwner) {
        synchronized(uploadSourcesLock) { uploadSources.remove(requestId, owner) }
        owner.dispose()
    }

    private fun registerSource(requestId: Long, owner: UploadSourceOwner) {
        synchronized(uploadSourcesLock) { uploadSources[requestId] = owner }
    }

    private fun isRegisteredSource(requestId: Long, owner: UploadSourceOwner): Boolean =
        synchronized(uploadSourcesLock) { uploadSources[requestId] === owner }

    private fun transferSource(fromRequestId: Long, toRequestId: Long, owner: UploadSourceOwner): Boolean =
        synchronized(uploadSourcesLock) {
            if (uploadSources[fromRequestId] !== owner) return@synchronized false
            uploadSources.remove(fromRequestId)
            uploadSources[toRequestId] = owner
            true
        }

    internal fun activeUploadSourceCount(): Int = synchronized(uploadSourcesLock) { uploadSources.size }

    private fun PhotoUploadSource.isValid(): Boolean =
        sourceId.length in 1..128 &&
            lengthBytes in 1..PHOTO_TASK_UPLOAD_MAX_BYTES &&
            safeFileName.length in 1..128 &&
            safeFileName.matches(SAFE_FILE_NAME) &&
            ".." !in safeFileName &&
            safeMimeType in SAFE_IMAGE_MIME_TYPES

    private fun String.validTaskId(): String? = trim().takeIf { it.isNotEmpty() }

    private companion object {
        val SAFE_FILE_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")
        val SAFE_IMAGE_MIME_TYPES = setOf(
            "image/avif",
            "image/gif",
            "image/jpeg",
            "image/png",
            "image/webp",
        )
    }

    private class UploadSourceOwner(val source: PhotoUploadSource) {
        private val disposed = AtomicBoolean(false)

        fun dispose() {
            if (disposed.compareAndSet(false, true)) runCatching(source::dispose)
        }
    }
}
