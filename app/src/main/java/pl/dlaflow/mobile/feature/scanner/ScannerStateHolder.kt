package pl.dlaflow.mobile.feature.scanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal class ScannerStateHolder {
    var state by mutableStateOf(ScannerUiState())
        private set

    private var nextRequestId = 0L
    private var activeSessionKey: String? = null
    private var pendingLaunchCode: String? = null
    private var pendingUnauthorizedRequestId: Long? = null

    fun beginLookup(sessionKey: String, rawCode: String?): ScannerLookupRequest? {
        val code = rawCode?.trim().orEmpty()
        if (code.isBlank()) return null
        pendingLaunchCode = null
        pendingUnauthorizedRequestId = null
        return startRequest(sessionKey, code)
    }

    fun waitForSession(rawCode: String?): Boolean {
        val code = rawCode?.trim().orEmpty()
        if (code.isBlank()) return false
        activeSessionKey = null
        pendingUnauthorizedRequestId = null
        pendingLaunchCode = code
        state = ScannerUiState(
            lookupState = DlaFlowUiState.Loading,
            waitingForSession = true,
        )
        return true
    }

    fun consumePendingLaunch(): String? {
        val code = pendingLaunchCode ?: return null
        pendingLaunchCode = null
        state = state.copy(waitingForSession = false)
        return code
    }

    fun failPendingLaunch(message: DlaFlowUiMessage): Boolean {
        if (pendingLaunchCode == null) return false
        pendingLaunchCode = null
        state = ScannerUiState(lookupState = DlaFlowUiState.Error(message))
        return true
    }

    fun failWithoutSession(message: DlaFlowUiMessage) {
        activeSessionKey = null
        pendingLaunchCode = null
        pendingUnauthorizedRequestId = null
        state = ScannerUiState(lookupState = DlaFlowUiState.Error(message))
    }

    fun captureCancelled() = Unit

    fun acceptSuccess(request: ScannerLookupRequest, result: ScannerLookupResult): Boolean {
        if (!matches(request)) return false
        finish(DlaFlowUiState.Content(result))
        return true
    }

    fun acceptOffline(request: ScannerLookupRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        finish(
            lookupState = DlaFlowUiState.Offline(),
            transientMessage = message,
        )
        return true
    }

    fun acceptFailure(request: ScannerLookupRequest, message: DlaFlowUiMessage): Boolean {
        if (!matches(request)) return false
        finish(DlaFlowUiState.Error(message))
        return true
    }

    fun acceptNoAccess(request: ScannerLookupRequest): Boolean {
        if (!matches(request)) return false
        finish(DlaFlowUiState.NoAccess)
        return true
    }

    fun acceptUnauthorized(
        request: ScannerLookupRequest,
        terminalMessage: DlaFlowUiMessage? = null,
    ): Boolean {
        if (!matches(request)) return false
        activeSessionKey = null
        pendingUnauthorizedRequestId = request.requestId.takeIf { terminalMessage == null }
        state = ScannerUiState(
            lookupState = terminalMessage?.let { DlaFlowUiState.Error(it) } ?: DlaFlowUiState.Loading,
        )
        return true
    }

    fun beginUnauthorizedRetry(request: ScannerLookupRequest): ScannerLookupRequest? {
        if (pendingUnauthorizedRequestId != request.requestId) return null
        pendingUnauthorizedRequestId = null
        return startRequest(request.sessionKey, request.code)
    }

    fun acceptSessionUnconfirmed(
        request: ScannerLookupRequest,
        message: DlaFlowUiMessage,
    ): Boolean {
        if (pendingUnauthorizedRequestId != request.requestId) return false
        pendingUnauthorizedRequestId = null
        state = ScannerUiState(lookupState = DlaFlowUiState.Error(message))
        return true
    }

    fun reset() {
        activeSessionKey = null
        pendingLaunchCode = null
        pendingUnauthorizedRequestId = null
        state = ScannerUiState()
    }

    private fun startRequest(sessionKey: String, code: String): ScannerLookupRequest {
        val request = ScannerLookupRequest(
            requestId = ++nextRequestId,
            sessionKey = sessionKey,
            code = code,
        )
        activeSessionKey = sessionKey
        state = ScannerUiState(
            lookupState = DlaFlowUiState.Loading,
            activeRequestId = request.requestId,
        )
        return request
    }

    private fun matches(request: ScannerLookupRequest): Boolean =
        state.activeRequestId == request.requestId && activeSessionKey == request.sessionKey

    private fun finish(
        lookupState: DlaFlowUiState<ScannerLookupResult>,
        transientMessage: DlaFlowUiMessage? = null,
    ) {
        activeSessionKey = null
        pendingLaunchCode = null
        pendingUnauthorizedRequestId = null
        state = ScannerUiState(
            lookupState = lookupState,
            transientMessage = transientMessage,
        )
    }
}
