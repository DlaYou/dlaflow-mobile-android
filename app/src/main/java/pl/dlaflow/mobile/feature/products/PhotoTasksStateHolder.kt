package pl.dlaflow.mobile.feature.products

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal class PhotoTasksStateHolder {
    var state by mutableStateOf(PhotoTasksUiState())
        private set

    private var nextRefreshRequestId = 0L
    private var nextUploadRequestId = 0L
    private var nextCompletionRequestId = 0L
    private var nextDispatchRequestId = 0L
    private var activeRefreshSessionKey: String? = null
    private var activeUploadSessionKey: String? = null
    private var activeCompletionSessionKey: String? = null
    private var activeDispatchSessionKey: String? = null
    private var pendingRefreshUnauthorizedRequestId: Long? = null
    private var pendingUploadUnauthorizedRequestId: Long? = null
    private var pendingCompletionUnauthorizedRequestId: Long? = null
    private var pendingDispatchUnauthorizedRequestId: Long? = null
    private var lastDispatchedTaskId: String? = null

    fun setFallback(task: ProductPhotoTask?) {
        state = state.copy(fallbackTask = task)
    }

    fun focus(rawTaskId: String?) {
        state = state.copy(focusedTaskId = rawTaskId?.trim()?.takeIf(String::isNotEmpty))
    }

    fun beginMediaSelection(taskId: String): Boolean {
        if (state.hasBlockingOperationInFlight) return false
        invalidateDispatchForOperation()
        state = state.copy(
            preparingMediaTaskId = taskId,
            transientMessage = null,
            transientMessageCanRefresh = false,
        )
        return true
    }

    fun restoreMediaSelection(taskId: String): Boolean {
        if (state.preparingMediaTaskId == taskId) return true
        if (state.hasBlockingOperationInFlight) return false
        invalidateDispatchForOperation()
        state = state.copy(
            preparingMediaTaskId = taskId,
            transientMessage = null,
            transientMessageCanRefresh = false,
        )
        return true
    }

    fun cancelMediaSelection() {
        if (state.preparingMediaTaskId == null) return
        state = state.copy(preparingMediaTaskId = null)
    }

    fun beginRefresh(sessionKey: String): PhotoTasksRefreshRequest? {
        if (state.hasMutationInFlight) return null
        invalidateDispatchForOperation()
        return startRefresh(sessionKey)
    }

    fun acceptRefreshSuccess(request: PhotoTasksRefreshRequest, tasks: List<ProductPhotoTask>): Boolean {
        if (!matches(request)) return false
        finishRefresh(
            tasksState = if (tasks.isEmpty()) DlaFlowUiState.Empty else DlaFlowUiState.Content(tasks.toList()),
            clearFallback = true,
        )
        return true
    }

    fun acceptRefreshOffline(request: PhotoTasksRefreshRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        finishRefresh(
            tasksState = DlaFlowUiState.Offline(state.contentOrNull()),
            transientMessage = message,
        )
        return true
    }

    fun acceptRefreshFailure(request: PhotoTasksRefreshRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        val content = state.contentOrNull()
        finishRefresh(
            tasksState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message),
            transientMessage = message.takeIf { content != null },
        )
        return true
    }

    fun acceptRefreshNoAccess(request: PhotoTasksRefreshRequest): Boolean {
        if (!matches(request)) return false
        finishRefresh(DlaFlowUiState.NoAccess, clearFallback = true)
        return true
    }

    fun acceptRefreshUnauthorized(
        request: PhotoTasksRefreshRequest,
        terminalMessage: DlaFlowUiMessage? = null,
    ): Boolean {
        if (!matches(request)) return false
        activeRefreshSessionKey = null
        pendingRefreshUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        val content = state.contentOrNull()
        state = state.copy(
            tasksState = when {
                content != null -> DlaFlowUiState.Content(content)
                terminalMessage != null -> DlaFlowUiState.Error(terminalMessage)
                else -> state.tasksState
            },
            isRefreshing = false,
            activeRefreshRequestId = null,
            transientMessage = terminalMessage.takeIf { content != null },
            transientMessageCanRefresh = content != null && terminalMessage?.retryable == true,
        )
        return true
    }

    fun beginRefreshUnauthorizedRetry(request: PhotoTasksRefreshRequest): PhotoTasksRefreshRequest? {
        if (pendingRefreshUnauthorizedRequestId != request.requestId) return null
        pendingRefreshUnauthorizedRequestId = null
        if (state.hasMutationInFlight) return null
        invalidateDispatchForOperation()
        return startRefresh(request.sessionKey)
    }

    fun acceptRefreshSessionUnconfirmed(
        request: PhotoTasksRefreshRequest,
        message: DlaFlowUiMessage,
    ): Boolean {
        if (pendingRefreshUnauthorizedRequestId != request.requestId) return false
        pendingRefreshUnauthorizedRequestId = null
        val content = state.contentOrNull()
        state = state.copy(
            tasksState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Error(message),
            transientMessage = message.takeIf { content != null },
            transientMessageCanRefresh = content != null && message.retryable,
        )
        return true
    }

    fun beginUpload(sessionKey: String, taskId: String, sourceId: String): PhotoTaskUploadRequest? {
        if (state.hasMutationInFlight) return null
        invalidateRefreshForMutation()
        invalidateDispatchForOperation()
        return startUpload(sessionKey, taskId, sourceId)
    }

    fun beginPreparedUpload(sessionKey: String, taskId: String, sourceId: String): PhotoTaskUploadRequest? {
        if (state.preparingMediaTaskId != taskId || state.uploadingTaskId != null || state.completingTaskIds.isNotEmpty()) {
            return null
        }
        invalidateRefreshForMutation()
        invalidateDispatchForOperation()
        state = state.copy(preparingMediaTaskId = null)
        return startUpload(sessionKey, taskId, sourceId)
    }

    private fun startUpload(sessionKey: String, taskId: String, sourceId: String): PhotoTaskUploadRequest {
        val request = PhotoTaskUploadRequest(
            requestId = ++nextUploadRequestId,
            sessionKey = sessionKey,
            taskId = taskId,
            sourceId = sourceId,
        )
        activeUploadSessionKey = sessionKey
        pendingUploadUnauthorizedRequestId = null
        state = state.copy(
            uploadingTaskId = taskId,
            activeUploadRequestId = request.requestId,
            transientMessage = null,
            transientMessageCanRefresh = false,
        )
        return request
    }

    fun acceptUploadSuccess(request: PhotoTaskUploadRequest, task: ProductPhotoTask): Boolean {
        if (!matches(request) || task.id != request.taskId) return false
        state = state.withUpsertedTask(task)
        finishUpload()
        return true
    }

    fun acceptUploadFailure(request: PhotoTaskUploadRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        finishUpload(message)
        return true
    }

    fun acceptUploadUnauthorized(
        request: PhotoTaskUploadRequest,
        terminalMessage: DlaFlowUiMessage? = null,
    ): Boolean {
        if (!matches(request)) return false
        activeUploadSessionKey = null
        pendingUploadUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        state = state.copy(
            uploadingTaskId = request.taskId.takeIf { terminalMessage == null },
            activeUploadRequestId = null,
            transientMessage = terminalMessage,
            transientMessageCanRefresh = false,
        )
        return true
    }

    fun beginUploadUnauthorizedRetry(request: PhotoTaskUploadRequest): PhotoTaskUploadRequest? {
        if (pendingUploadUnauthorizedRequestId != request.requestId) return null
        pendingUploadUnauthorizedRequestId = null
        if (state.uploadingTaskId != request.taskId || state.completingTaskIds.isNotEmpty()) return null
        return startUpload(request.sessionKey, request.taskId, request.sourceId)
    }

    fun acceptUploadSessionUnconfirmed(
        request: PhotoTaskUploadRequest,
        message: DlaFlowUiMessage,
    ): Boolean {
        if (pendingUploadUnauthorizedRequestId != request.requestId) return false
        pendingUploadUnauthorizedRequestId = null
        finishUpload(message)
        return true
    }

    fun rejectUpload(taskId: String?, message: DlaFlowUiMessage) {
        state = state.copy(
            preparingMediaTaskId = null,
            transientMessage = message,
            transientMessageCanRefresh = false,
        )
    }

    fun beginCompletion(sessionKey: String, taskId: String): PhotoTaskCompletionRequest? {
        if (state.hasMutationInFlight) return null
        invalidateRefreshForMutation()
        invalidateDispatchForOperation()
        return startCompletion(sessionKey, taskId)
    }

    private fun startCompletion(sessionKey: String, taskId: String): PhotoTaskCompletionRequest {
        val request = PhotoTaskCompletionRequest(
            requestId = ++nextCompletionRequestId,
            sessionKey = sessionKey,
            taskId = taskId,
        )
        activeCompletionSessionKey = sessionKey
        pendingCompletionUnauthorizedRequestId = null
        state = state.copy(
            completingTaskIds = setOf(taskId),
            activeCompletionRequestId = request.requestId,
            transientMessage = null,
            transientMessageCanRefresh = false,
        )
        return request
    }

    fun acceptCompletionSuccess(request: PhotoTaskCompletionRequest, task: ProductPhotoTask): Boolean {
        if (!matches(request) || task.id != request.taskId) return false
        state = state.withoutTask(request.taskId)
        finishCompletion()
        return true
    }

    fun acceptCompletionFailure(request: PhotoTaskCompletionRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        finishCompletion(message)
        return true
    }

    fun acceptCompletionUnauthorized(
        request: PhotoTaskCompletionRequest,
        terminalMessage: DlaFlowUiMessage? = null,
    ): Boolean {
        if (!matches(request)) return false
        activeCompletionSessionKey = null
        pendingCompletionUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        state = state.copy(
            completingTaskIds = if (terminalMessage == null) setOf(request.taskId) else emptySet(),
            activeCompletionRequestId = null,
            transientMessage = terminalMessage,
            transientMessageCanRefresh = false,
        )
        return true
    }

    fun beginCompletionUnauthorizedRetry(request: PhotoTaskCompletionRequest): PhotoTaskCompletionRequest? {
        if (pendingCompletionUnauthorizedRequestId != request.requestId) return null
        pendingCompletionUnauthorizedRequestId = null
        if (state.completingTaskIds != setOf(request.taskId) || state.uploadingTaskId != null) return null
        return startCompletion(request.sessionKey, request.taskId)
    }

    fun acceptCompletionSessionUnconfirmed(
        request: PhotoTaskCompletionRequest,
        message: DlaFlowUiMessage,
    ): Boolean {
        if (pendingCompletionUnauthorizedRequestId != request.requestId) return false
        pendingCompletionUnauthorizedRequestId = null
        finishCompletion(message)
        return true
    }

    fun beginDispatch(sessionKey: String): PhotoTaskDispatchRequest? {
        if (state.activeDispatchRequestId != null || state.hasBlockingOperationInFlight) return null
        val request = PhotoTaskDispatchRequest(
            requestId = ++nextDispatchRequestId,
            sessionKey = sessionKey,
        )
        activeDispatchSessionKey = sessionKey
        pendingDispatchUnauthorizedRequestId = null
        state = state.copy(activeDispatchRequestId = request.requestId)
        return request
    }

    fun acceptDispatchSuccess(
        request: PhotoTaskDispatchRequest,
        task: ProductPhotoTask?,
    ): ProductPhotoTask? {
        if (!matches(request)) return null
        finishDispatch()
        if (task == null || task.id.isBlank() || task.id == lastDispatchedTaskId) return null
        lastDispatchedTaskId = task.id
        state = state.withUpsertedTask(task).copy(
            fallbackTask = task,
            focusedTaskId = task.id,
        )
        return task
    }

    fun acceptDispatchFailure(request: PhotoTaskDispatchRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        finishDispatch(message)
        return true
    }

    fun acceptDispatchUnauthorized(
        request: PhotoTaskDispatchRequest,
        terminalMessage: DlaFlowUiMessage? = null,
    ): Boolean {
        if (!matches(request)) return false
        activeDispatchSessionKey = null
        pendingDispatchUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        state = state.copy(
            activeDispatchRequestId = null,
            transientMessage = terminalMessage,
            transientMessageCanRefresh = false,
        )
        return true
    }

    fun beginDispatchUnauthorizedRetry(request: PhotoTaskDispatchRequest): PhotoTaskDispatchRequest? {
        if (pendingDispatchUnauthorizedRequestId != request.requestId) return null
        pendingDispatchUnauthorizedRequestId = null
        return beginDispatch(request.sessionKey)
    }

    fun acceptDispatchSessionUnconfirmed(
        request: PhotoTaskDispatchRequest,
        message: DlaFlowUiMessage,
    ): Boolean {
        if (pendingDispatchUnauthorizedRequestId != request.requestId) return false
        pendingDispatchUnauthorizedRequestId = null
        state = state.copy(transientMessage = message, transientMessageCanRefresh = false)
        return true
    }

    fun reset() {
        activeRefreshSessionKey = null
        activeUploadSessionKey = null
        activeCompletionSessionKey = null
        activeDispatchSessionKey = null
        pendingRefreshUnauthorizedRequestId = null
        pendingUploadUnauthorizedRequestId = null
        pendingCompletionUnauthorizedRequestId = null
        pendingDispatchUnauthorizedRequestId = null
        lastDispatchedTaskId = null
        state = PhotoTasksUiState()
    }

    private fun startRefresh(sessionKey: String): PhotoTasksRefreshRequest {
        val request = PhotoTasksRefreshRequest(++nextRefreshRequestId, sessionKey)
        val content = state.contentOrNull()
        activeRefreshSessionKey = sessionKey
        pendingRefreshUnauthorizedRequestId = null
        state = state.copy(
            tasksState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Loading,
            isRefreshing = content != null,
            activeRefreshRequestId = request.requestId,
            transientMessage = null,
            transientMessageCanRefresh = false,
        )
        return request
    }

    private fun invalidateRefreshForMutation() {
        if (state.activeRefreshRequestId == null && pendingRefreshUnauthorizedRequestId == null) return
        activeRefreshSessionKey = null
        pendingRefreshUnauthorizedRequestId = null
        state = state.copy(
            isRefreshing = false,
            activeRefreshRequestId = null,
        )
    }

    private fun invalidateDispatchForOperation() {
        if (state.activeDispatchRequestId == null && pendingDispatchUnauthorizedRequestId == null) return
        activeDispatchSessionKey = null
        pendingDispatchUnauthorizedRequestId = null
        state = state.copy(activeDispatchRequestId = null)
    }

    private fun matches(request: PhotoTasksRefreshRequest): Boolean =
        state.activeRefreshRequestId == request.requestId && activeRefreshSessionKey == request.sessionKey

    private fun matches(request: PhotoTaskUploadRequest): Boolean =
        state.activeUploadRequestId == request.requestId &&
            activeUploadSessionKey == request.sessionKey &&
            state.uploadingTaskId == request.taskId

    private fun matches(request: PhotoTaskCompletionRequest): Boolean =
        state.activeCompletionRequestId == request.requestId &&
            activeCompletionSessionKey == request.sessionKey &&
            request.taskId in state.completingTaskIds

    private fun matches(request: PhotoTaskDispatchRequest): Boolean =
        state.activeDispatchRequestId == request.requestId && activeDispatchSessionKey == request.sessionKey

    private fun finishRefresh(
        tasksState: DlaFlowUiState<List<ProductPhotoTask>>,
        transientMessage: DlaFlowUiMessage? = null,
        clearFallback: Boolean = false,
    ) {
        activeRefreshSessionKey = null
        pendingRefreshUnauthorizedRequestId = null
        state = state.copy(
            tasksState = tasksState,
            fallbackTask = if (clearFallback) null else state.fallbackTask,
            isRefreshing = false,
            activeRefreshRequestId = null,
            transientMessage = transientMessage,
            transientMessageCanRefresh = tasksState is DlaFlowUiState.Content && transientMessage?.retryable == true,
        )
    }

    private fun finishUpload(message: DlaFlowUiMessage? = null) {
        activeUploadSessionKey = null
        pendingUploadUnauthorizedRequestId = null
        state = state.copy(
            uploadingTaskId = null,
            activeUploadRequestId = null,
            transientMessage = message,
            transientMessageCanRefresh = false,
        )
    }

    private fun finishCompletion(message: DlaFlowUiMessage? = null) {
        activeCompletionSessionKey = null
        pendingCompletionUnauthorizedRequestId = null
        state = state.copy(
            completingTaskIds = emptySet(),
            activeCompletionRequestId = null,
            transientMessage = message,
            transientMessageCanRefresh = false,
        )
    }

    private fun finishDispatch(message: DlaFlowUiMessage? = null) {
        activeDispatchSessionKey = null
        pendingDispatchUnauthorizedRequestId = null
        state = state.copy(
            activeDispatchRequestId = null,
            transientMessage = message,
            transientMessageCanRefresh = false,
        )
    }

    private fun PhotoTasksUiState.withUpsertedTask(task: ProductPhotoTask): PhotoTasksUiState {
        val current = contentOrNull()
        if (current == null) return copy(fallbackTask = task)
        val next = current.toMutableList()
        val index = next.indexOfFirst { it.id == task.id }
        if (index >= 0) next[index] = task else next.add(task)
        return copy(tasksState = DlaFlowUiState.Content(next.toList()))
    }

    private fun PhotoTasksUiState.withoutTask(taskId: String): PhotoTasksUiState {
        val current = contentOrNull()
        val nextState = if (current == null) {
            tasksState
        } else {
            val next = current.filterNot { it.id == taskId }
            if (next.isEmpty()) DlaFlowUiState.Empty else DlaFlowUiState.Content(next)
        }
        return copy(
            tasksState = nextState,
            fallbackTask = fallbackTask?.takeUnless { it.id == taskId },
            focusedTaskId = focusedTaskId?.takeUnless { it == taskId },
        )
    }
}
