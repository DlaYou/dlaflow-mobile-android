package pl.dlaflow.mobile.feature.products

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

internal class PhotoTasksCoordinator(
    private val stateHolder: PhotoTasksStateHolder,
    private val gateway: PhotoTasksGateway,
    private val executor: Executor,
    private val postToMain: (() -> Unit) -> Unit,
    private val pollScheduler: ProductsSearchScheduler,
    private val onEffect: (PhotoTasksEffect) -> Unit,
    private val onUnauthorized: (Throwable, Boolean, () -> Unit, () -> Unit) -> Unit,
    private val onTasksChanged: () -> Unit = {},
) {
    private val uploadSources = mutableMapOf<Long, UploadSourceOwner>()
    private val uploadSourcesLock = Any()
    private var pendingUploadPoll: ProductsScheduledTask? = null
    private var uploadPollAttempt = 0
    private var pendingUploadPollUnauthorizedRequestId: Long? = null

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
        waitForProcessing: Boolean = true,
    ): Boolean {
        val owner = UploadSourceOwner(source)
        val normalizedTaskId = taskId.validTaskId()
        if (normalizedTaskId == null || !source.isValid()) {
            stateHolder.rejectUpload(normalizedTaskId, invalidPhotoUploadMessage())
            owner.dispose()
            return false
        }

        val request = if (stateHolder.state.preparingMediaTaskId != null) {
            stateHolder.beginPreparedUpload(token, normalizedTaskId, source.sourceId, waitForProcessing)
        } else {
            stateHolder.beginUpload(token, normalizedTaskId, source.sourceId, waitForProcessing)
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
        cancelUploadPolling()
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
                    postToMain {
                        if (stateHolder.acceptRefreshSuccess(request, tasks)) onTasksChanged()
                    }
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
                        releaseSource(request.requestId, owner)
                        handleUploadResult(token, request, task, allowUnauthorizedRetry)
                    }
                }
                .onFailure { error ->
                    postToMain { handleUploadFailure(token, request, owner, error, allowUnauthorizedRetry) }
                }
        }
    }

    private fun handleUploadResult(
        token: String,
        request: PhotoTaskUploadRequest,
        task: ProductPhotoTask,
        allowUnauthorizedRetry: Boolean,
    ) {
        when {
            task.hasFailedMedia(request) -> finishUploadWithFailure(request)
            !request.waitForProcessing -> {
                if (stateHolder.acceptUploadSuccess(request, task)) {
                    onTasksChanged()
                    onEffect(PhotoTasksEffect.UploadSucceeded)
                }
            }
            task.isUploadReady(request) -> {
                if (stateHolder.acceptUploadSuccess(request, task)) {
                    cancelUploadPolling()
                    onTasksChanged()
                    onEffect(PhotoTasksEffect.UploadSucceeded)
                }
            }
            else -> {
                if (stateHolder.acceptUploadProgress(request, task)) {
                    uploadPollAttempt = 0
                    scheduleUploadPoll(token, request, allowUnauthorizedRetry)
                }
            }
        }
    }

    private fun scheduleUploadPoll(
        token: String,
        request: PhotoTaskUploadRequest,
        allowUnauthorizedRetry: Boolean,
    ) {
        if (uploadPollAttempt >= MAX_UPLOAD_POLL_ATTEMPTS) {
            finishUploadWithFailure(request)
            return
        }
        pendingUploadPoll?.cancel()
        pendingUploadPoll = pollScheduler.schedule(UPLOAD_POLL_INTERVAL_MS) {
            uploadPollAttempt += 1
            executor.execute {
                runCatching { gateway.load(token, request.taskId) }
                    .onSuccess { task ->
                        postToMain { handleUploadPollResult(token, request, task, allowUnauthorizedRetry) }
                    }
                    .onFailure { error ->
                        postToMain {
                            val failure = mapPhotoTasksFailure(error)
                            when (failure) {
                                is PhotoTasksFailure.Offline,
                                is PhotoTasksFailure.Retryable -> {
                                    if (stateHolder.isUploadActive(request)) {
                                        scheduleUploadPoll(token, request, allowUnauthorizedRetry)
                                    }
                                }
                                is PhotoTasksFailure.Unauthorized -> handleUploadPollUnauthorized(
                                    token,
                                    request,
                                    error,
                                    failure,
                                    allowUnauthorizedRetry,
                                )
                                is PhotoTasksFailure.NoAccess,
                                is PhotoTasksFailure.InvalidPayload -> finishUploadWithFailure(request, failure.message)
                            }
                        }
                    }
            }
        }
    }

    private fun handleUploadPollResult(
        token: String,
        request: PhotoTaskUploadRequest,
        task: ProductPhotoTask,
        allowUnauthorizedRetry: Boolean,
    ) {
        when {
            task.hasFailedMedia(request) -> finishUploadWithFailure(request)
            task.isUploadReady(request) -> {
                if (stateHolder.acceptUploadSuccess(request, task)) {
                    cancelUploadPolling()
                    onTasksChanged()
                    onEffect(PhotoTasksEffect.UploadSucceeded)
                }
            }
            else -> {
                if (stateHolder.acceptUploadProgress(request, task)) {
                    scheduleUploadPoll(token, request, allowUnauthorizedRetry)
                }
            }
        }
    }

    private fun handleUploadPollUnauthorized(
        token: String,
        request: PhotoTaskUploadRequest,
        error: Throwable,
        failure: PhotoTasksFailure.Unauthorized,
        allowUnauthorizedRetry: Boolean,
    ) {
        if (!stateHolder.isUploadActive(request)) return
        pendingUploadPoll = null
        if (!allowUnauthorizedRetry || pendingUploadPollUnauthorizedRequestId == request.requestId) {
            finishUploadWithFailure(request, failure.message)
            return
        }
        pendingUploadPollUnauthorizedRequestId = request.requestId
        onUnauthorized(
            error,
            true,
            {
                if (pendingUploadPollUnauthorizedRequestId != request.requestId) return@onUnauthorized
                pendingUploadPollUnauthorizedRequestId = null
                if (stateHolder.isUploadActive(request)) {
                    scheduleUploadPoll(token, request, allowUnauthorizedRetry = false)
                }
            },
            {
                if (pendingUploadPollUnauthorizedRequestId != request.requestId) return@onUnauthorized
                pendingUploadPollUnauthorizedRequestId = null
                finishUploadWithFailure(request, photoTasksSessionUnconfirmedMessage())
            },
        )
    }

    private fun finishUploadWithFailure(
        request: PhotoTaskUploadRequest,
        message: pl.dlaflow.mobile.core.state.DlaFlowUiMessage = photoTaskProcessingFailedMessage(),
    ) {
        if (stateHolder.acceptUploadFailure(request, message)) {
            cancelUploadPolling()
            onEffect(PhotoTasksEffect.OperationFailed)
        }
    }

    private fun cancelUploadPolling() {
        pendingUploadPoll?.cancel()
        pendingUploadPoll = null
        uploadPollAttempt = 0
        pendingUploadPollUnauthorizedRequestId = null
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
                    onEffect(PhotoTasksEffect.UploadFailed(retryable = false))
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
                            onEffect(PhotoTasksEffect.UploadFailed(retryable = true))
                            releaseSource(request.requestId, owner)
                        }
                    },
                )
            }
            is PhotoTasksFailure.NoAccess -> finishUploadFailure(request, owner, failure.message, retryable = false)
            is PhotoTasksFailure.Offline -> finishUploadFailure(request, owner, failure.message, retryable = true)
            is PhotoTasksFailure.InvalidPayload -> finishUploadFailure(request, owner, failure.message, retryable = false)
            is PhotoTasksFailure.Retryable -> finishUploadFailure(request, owner, failure.message, retryable = failure.message.retryable)
        }
    }

    private fun finishUploadFailure(
        request: PhotoTaskUploadRequest,
        owner: UploadSourceOwner,
        message: pl.dlaflow.mobile.core.state.DlaFlowUiMessage,
        retryable: Boolean,
    ) {
        val accepted = stateHolder.acceptUploadFailure(request, message)
        releaseSource(request.requestId, owner)
        if (accepted) onEffect(PhotoTasksEffect.UploadFailed(retryable))
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
                        if (stateHolder.acceptCompletionSuccess(request, task)) {
                            onTasksChanged()
                            onEffect(PhotoTasksEffect.CompletionSucceeded)
                        }
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
            is PhotoTasksFailure.NoAccess -> if (stateHolder.acceptCompletionFailure(request, failure.message)) onEffect(PhotoTasksEffect.OperationFailed)
            is PhotoTasksFailure.Offline -> if (stateHolder.acceptCompletionFailure(request, failure.message)) onEffect(PhotoTasksEffect.OperationFailed)
            is PhotoTasksFailure.InvalidPayload -> if (stateHolder.acceptCompletionFailure(request, failure.message)) onEffect(PhotoTasksEffect.OperationFailed)
            is PhotoTasksFailure.Retryable -> if (stateHolder.acceptCompletionFailure(request, failure.message)) onEffect(PhotoTasksEffect.OperationFailed)
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
            is PhotoTasksFailure.NoAccess -> if (stateHolder.acceptDispatchFailure(request, failure.message)) onEffect(PhotoTasksEffect.OperationFailed)
            is PhotoTasksFailure.Offline -> if (stateHolder.acceptDispatchFailure(request, failure.message)) onEffect(PhotoTasksEffect.OperationFailed)
            is PhotoTasksFailure.InvalidPayload -> if (stateHolder.acceptDispatchFailure(request, failure.message)) onEffect(PhotoTasksEffect.OperationFailed)
            is PhotoTasksFailure.Retryable -> if (stateHolder.acceptDispatchFailure(request, failure.message)) onEffect(PhotoTasksEffect.OperationFailed)
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

    private fun ProductPhotoTask.hasFailedMedia(request: PhotoTaskUploadRequest): Boolean =
        media.any { it.id !in request.existingMediaIds && it.status == PhotoTaskMediaStatus.FAILED }

    private fun ProductPhotoTask.isUploadReady(request: PhotoTaskUploadRequest): Boolean {
        val uploadedMedia = media.filterNot { it.id in request.existingMediaIds }
        return if (uploadedMedia.isNotEmpty()) {
            uploadedMedia.all { it.status == PhotoTaskMediaStatus.READY }
        } else {
            media.isNotEmpty() && media.all { it.status == PhotoTaskMediaStatus.READY }
        }
    }

    private companion object {
        const val UPLOAD_POLL_INTERVAL_MS = 2_000L
        const val MAX_UPLOAD_POLL_ATTEMPTS = 15
        val SAFE_FILE_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")
        val SAFE_IMAGE_MIME_TYPES = setOf(
            "image/avif",
            "image/gif",
            "image/heic",
            "image/heif",
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
