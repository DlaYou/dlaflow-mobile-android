package pl.dlaflow.mobile.feature.pairing

import pl.dlaflow.mobile.core.state.DlaFlowUiMessage

internal enum class PairingStep { CODE, NAME, HELP }

internal enum class PairingFeedback {
    ENTER_CODE,
    INVALID_CODE,
    INVALID_QR,
    DEVICE_NAME_REQUIRED,
    DEVICE_NAME_TOO_SHORT,
    DEVICE_NAME_TOO_LONG,
    DEVICE_NAME_INVALID,
    CODE_NOT_FOUND,
    CODE_USED,
    CODE_EXPIRED,
}

internal data class PairingUiState(
    val step: PairingStep = PairingStep.CODE,
    val codeInput: String = "",
    val deviceNameInput: String = "",
    val isSubmitting: Boolean = false,
    val activeRequestId: Long? = null,
    val localFeedback: PairingFeedback? = null,
    val sharedMessage: DlaFlowUiMessage? = null,
)

internal data class PairingSubmission(
    val requestId: Long,
    val code: String,
    val deviceName: String,
)
