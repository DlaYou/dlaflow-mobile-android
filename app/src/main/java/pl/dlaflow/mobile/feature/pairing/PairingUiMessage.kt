package pl.dlaflow.mobile.feature.pairing

import androidx.annotation.StringRes
import pl.dlaflow.mobile.R

@StringRes
internal fun PairingFeedback.messageRes(): Int = when (this) {
    PairingFeedback.ENTER_CODE,
    PairingFeedback.INVALID_CODE,
    -> R.string.pairing_error_code_invalid
    PairingFeedback.INVALID_QR -> R.string.pairing_error_qr_invalid
    PairingFeedback.DEVICE_NAME_REQUIRED -> R.string.pairing_error_name_required
    PairingFeedback.DEVICE_NAME_TOO_SHORT -> R.string.pairing_error_name_short
    PairingFeedback.DEVICE_NAME_TOO_LONG -> R.string.pairing_error_name_long
    PairingFeedback.DEVICE_NAME_INVALID -> R.string.pairing_error_name_invalid
    PairingFeedback.CODE_NOT_FOUND -> R.string.pairing_error_code_not_found
    PairingFeedback.CODE_USED -> R.string.pairing_error_code_used
    PairingFeedback.CODE_EXPIRED -> R.string.pairing_error_code_expired
}

internal fun pairingStatusMessage(
    localMessage: String?,
    requestMessage: String?,
    sessionMessage: String,
): String = localMessage ?: requestMessage ?: sessionMessage
