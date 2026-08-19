package pl.dlaflow.mobile.feature.scanner

import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal enum class ScannerMatchKind {
    NO_MATCH,
    MATCH,
    AMBIGUOUS,
}

internal data class ScannerOrder(
    val orderNumber: String,
    val customer: String,
    val status: String,
)

internal data class ScannerShipment(
    val carrier: String,
    val status: String,
)

internal data class ScannerLookupResult(
    val kind: ScannerMatchKind,
    val order: ScannerOrder?,
    val shipment: ScannerShipment?,
)

internal data class ScannerUiState(
    val lookupState: DlaFlowUiState<ScannerLookupResult> = DlaFlowUiState.Empty,
    val activeRequestId: Long? = null,
    val waitingForSession: Boolean = false,
    val transientMessage: DlaFlowUiMessage? = null,
)

internal fun ScannerUiState.contentOrNull(): ScannerLookupResult? = when (val current = lookupState) {
    is DlaFlowUiState.Content -> current.data
    is DlaFlowUiState.Offline -> current.lastContent
    else -> null
}

internal data class ScannerLookupRequest(
    val requestId: Long,
    internal val sessionKey: String,
    internal val code: String,
)

internal sealed interface ScannerAction {
    data object RequestCapture : ScannerAction
    data class OpenOrder(val orderNumber: String) : ScannerAction
    data object Reset : ScannerAction
}
