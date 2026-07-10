package pl.dlaflow.mobile.feature.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal class DashboardStateHolder {
    var state by mutableStateOf(DashboardUiState())
        private set

    private var nextRequestId = 0L
    private var activeSessionKey: String? = null

    fun beginLoad(sessionKey: String): DashboardLoadRequest {
        val request = DashboardLoadRequest(
            requestId = ++nextRequestId,
            sessionKey = sessionKey,
        )
        val content = state.contentOrNull()
        activeSessionKey = sessionKey
        state = DashboardUiState(
            contentState = content?.let { DlaFlowUiState.Content(it) } ?: DlaFlowUiState.Loading,
            isRefreshing = content != null,
            activeRequestId = request.requestId,
        )
        return request
    }

    fun acceptSuccess(request: DashboardLoadRequest, content: DashboardContent): Boolean {
        if (!matches(request)) return false
        finish(DlaFlowUiState.Content(content))
        return true
    }

    fun acceptOffline(request: DashboardLoadRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        finish(
            contentState = DlaFlowUiState.Offline(state.contentOrNull()),
            transientMessage = message,
        )
        return true
    }

    fun acceptFailure(request: DashboardLoadRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        val content = state.contentOrNull()
        if (content == null) {
            finish(DlaFlowUiState.Error(message))
        } else {
            finish(
                contentState = DlaFlowUiState.Content(content),
                transientMessage = message,
            )
        }
        return true
    }

    fun acceptNoAccess(request: DashboardLoadRequest): Boolean {
        if (!matches(request)) return false
        finish(DlaFlowUiState.NoAccess)
        return true
    }

    fun acceptUnauthorized(request: DashboardLoadRequest): Boolean {
        if (!matches(request)) return false
        reset()
        return true
    }

    fun reset() {
        activeSessionKey = null
        state = DashboardUiState()
    }

    private fun matches(request: DashboardLoadRequest): Boolean =
        state.activeRequestId == request.requestId && activeSessionKey == request.sessionKey

    private fun finish(
        contentState: DlaFlowUiState<DashboardContent>,
        transientMessage: DlaFlowUiMessage? = null,
    ) {
        activeSessionKey = null
        state = DashboardUiState(
            contentState = contentState,
            transientMessage = transientMessage,
        )
    }
}
