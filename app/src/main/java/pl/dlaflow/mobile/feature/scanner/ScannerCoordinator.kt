package pl.dlaflow.mobile.feature.scanner

import java.util.concurrent.Executor

internal enum class ScannerFeedback {
    CAPTURE_EMPTY,
    WAITING_FOR_SESSION,
    LOOKUP_LOADING,
    MATCHED,
    AMBIGUOUS,
    NO_MATCH,
    LOAD_FAILED,
}

internal class ScannerCoordinator(
    private val stateHolder: ScannerStateHolder,
    private val gateway: ScannerGateway,
    private val executor: Executor,
    private val postToMain: (() -> Unit) -> Unit,
    private val onFeedback: (ScannerFeedback) -> Unit,
    private val onRequestCapture: () -> Unit,
    private val onOpenOrder: (String) -> Unit,
    private val onUnauthorized: (Throwable, Boolean, () -> Unit, () -> Unit) -> Unit,
) {
    fun handleAction(action: ScannerAction) {
        when (action) {
            ScannerAction.RequestCapture -> onRequestCapture()
            is ScannerAction.OpenOrder -> action.orderNumber.trim().takeIf { it.isNotBlank() }?.let(onOpenOrder)
            ScannerAction.Reset -> reset()
        }
    }

    fun acceptCapture(
        rawCode: String?,
        token: String?,
        hasSavedSession: Boolean = false,
    ): Boolean {
        val code = rawCode?.trim().orEmpty()
        if (code.isBlank()) {
            stateHolder.captureCancelled()
            onFeedback(ScannerFeedback.CAPTURE_EMPTY)
            return false
        }
        if (token.isNullOrBlank()) {
            if (hasSavedSession) {
                val accepted = stateHolder.waitForSession(code)
                if (accepted) onFeedback(ScannerFeedback.WAITING_FOR_SESSION)
                return accepted
            }
            stateHolder.failWithoutSession(scannerMissingSessionMessage())
            onFeedback(ScannerFeedback.LOAD_FAILED)
            return false
        }
        return startLookup(token, code)
    }

    fun acceptLaunch(
        rawCode: String?,
        activeToken: String?,
        hasSavedSession: Boolean,
    ): Boolean {
        val code = rawCode?.trim().orEmpty()
        if (code.isBlank()) return false
        if (!activeToken.isNullOrBlank()) return startLookup(activeToken, code)
        if (hasSavedSession) {
            val accepted = stateHolder.waitForSession(code)
            if (accepted) onFeedback(ScannerFeedback.WAITING_FOR_SESSION)
            return accepted
        }
        stateHolder.failWithoutSession(scannerMissingSessionMessage())
        onFeedback(ScannerFeedback.LOAD_FAILED)
        return false
    }

    fun resumePendingLaunch(token: String?): Boolean {
        val code = stateHolder.consumePendingLaunch() ?: return false
        if (token.isNullOrBlank()) {
            stateHolder.failWithoutSession(scannerMissingSessionMessage())
            onFeedback(ScannerFeedback.LOAD_FAILED)
            return false
        }
        return startLookup(token, code)
    }

    fun failPendingLaunch(): Boolean {
        val accepted = stateHolder.failPendingLaunch(scannerSessionUnconfirmedMessage())
        if (accepted) onFeedback(ScannerFeedback.LOAD_FAILED)
        return accepted
    }

    fun reset() = stateHolder.reset()

    private fun startLookup(token: String, code: String): Boolean {
        val request = stateHolder.beginLookup(token, code) ?: return false
        onFeedback(ScannerFeedback.LOOKUP_LOADING)
        scheduleLookup(token, request, allowUnauthorizedRetry = true)
        return true
    }

    private fun scheduleLookup(
        token: String,
        request: ScannerLookupRequest,
        allowUnauthorizedRetry: Boolean,
    ) {
        executor.execute {
            runCatching {
                gateway.lookup(token, request.code)
            }.onSuccess { result ->
                postToMain {
                    if (stateHolder.acceptSuccess(request, result)) {
                        onFeedback(result.toFeedback())
                    }
                }
            }.onFailure { error ->
                postToMain {
                    handleFailure(request, token, error, allowUnauthorizedRetry)
                }
            }
        }
    }

    private fun handleFailure(
        request: ScannerLookupRequest,
        token: String,
        error: Throwable,
        allowUnauthorizedRetry: Boolean,
    ) {
        when (val failure = mapScannerFailure(error)) {
            is ScannerFailure.Unauthorized -> handleUnauthorized(
                request = request,
                token = token,
                error = error,
                message = failure.message,
                allowRetry = allowUnauthorizedRetry,
            )

            ScannerFailure.NoAccess -> {
                if (stateHolder.acceptNoAccess(request)) onFeedback(ScannerFeedback.LOAD_FAILED)
            }

            is ScannerFailure.Offline -> {
                if (stateHolder.acceptOffline(request, failure.message)) onFeedback(ScannerFeedback.LOAD_FAILED)
            }

            is ScannerFailure.InvalidResult -> {
                if (stateHolder.acceptFailure(request, failure.message)) onFeedback(ScannerFeedback.LOAD_FAILED)
            }

            is ScannerFailure.Retryable -> {
                if (stateHolder.acceptFailure(request, failure.message)) onFeedback(ScannerFeedback.LOAD_FAILED)
            }
        }
    }

    private fun handleUnauthorized(
        request: ScannerLookupRequest,
        token: String,
        error: Throwable,
        message: pl.dlaflow.mobile.core.state.DlaFlowUiMessage,
        allowRetry: Boolean,
    ) {
        if (!allowRetry) {
            if (stateHolder.acceptUnauthorized(request, terminalMessage = message)) {
                onFeedback(ScannerFeedback.LOAD_FAILED)
            }
            return
        }
        if (!stateHolder.acceptUnauthorized(request)) return
        onUnauthorized(
            error,
            true,
            {
                val retry = stateHolder.beginUnauthorizedRetry(request) ?: return@onUnauthorized
                onFeedback(ScannerFeedback.LOOKUP_LOADING)
                scheduleLookup(token, retry, allowUnauthorizedRetry = false)
            },
            {
                if (stateHolder.acceptSessionUnconfirmed(request, scannerSessionUnconfirmedMessage())) {
                    onFeedback(ScannerFeedback.LOAD_FAILED)
                }
            },
        )
    }

    private fun ScannerLookupResult.toFeedback(): ScannerFeedback = when (kind) {
        ScannerMatchKind.NO_MATCH -> ScannerFeedback.NO_MATCH
        ScannerMatchKind.MATCH -> ScannerFeedback.MATCHED
        ScannerMatchKind.AMBIGUOUS -> ScannerFeedback.AMBIGUOUS
    }
}
