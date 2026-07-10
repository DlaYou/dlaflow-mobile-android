package pl.dlaflow.mobile.feature.pairing

import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.mobileErrorToUiMessage

internal sealed interface PairingFailure {
    data class CodeRejected(val feedback: PairingFeedback) : PairingFailure
    data class Retryable(val message: DlaFlowUiMessage) : PairingFailure
}

internal fun mapPairingFailure(error: Throwable): PairingFailure {
    val code = (error as? MobileApiException)?.code
    return when (code) {
        "MOBILE_PAIRING_CODE_NOT_FOUND",
        "MOBILE_PAIRING_CODE_INVALID",
        -> PairingFailure.CodeRejected(PairingFeedback.CODE_NOT_FOUND)
        "MOBILE_PAIRING_CODE_USED" -> PairingFailure.CodeRejected(PairingFeedback.CODE_USED)
        "MOBILE_PAIRING_CODE_EXPIRED" -> PairingFailure.CodeRejected(PairingFeedback.CODE_EXPIRED)
        else -> PairingFailure.Retryable(mobileErrorToUiMessage(error))
    }
}
