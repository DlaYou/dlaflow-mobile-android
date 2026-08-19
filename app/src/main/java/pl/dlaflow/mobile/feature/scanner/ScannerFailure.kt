package pl.dlaflow.mobile.feature.scanner

import java.net.ConnectException
import java.net.UnknownHostException
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.mobileErrorToUiMessage

internal sealed interface ScannerFailure {
    data class Unauthorized(val message: DlaFlowUiMessage) : ScannerFailure
    data object NoAccess : ScannerFailure
    data class Offline(val message: DlaFlowUiMessage) : ScannerFailure
    data class InvalidResult(val message: DlaFlowUiMessage) : ScannerFailure
    data class Retryable(val message: DlaFlowUiMessage) : ScannerFailure
}

internal fun mapScannerFailure(error: Throwable): ScannerFailure = when {
    error is MobileApiException && error.statusCode == 401 ->
        ScannerFailure.Unauthorized(mobileErrorToUiMessage(error))

    error is MobileApiException && error.statusCode == 403 -> ScannerFailure.NoAccess
    error is UnknownHostException || error is ConnectException ->
        ScannerFailure.Offline(mobileErrorToUiMessage(error))

    error is InvalidScannerResultException -> ScannerFailure.InvalidResult(scannerInvalidResultMessage())
    else -> ScannerFailure.Retryable(mobileErrorToUiMessage(error))
}

internal fun scannerMissingSessionMessage() = DlaFlowUiMessage(
    titleRes = R.string.mobile_error_session_title,
    descriptionRes = R.string.mobile_error_session_description,
    retryable = false,
)

internal fun scannerSessionUnconfirmedMessage() = DlaFlowUiMessage(
    titleRes = R.string.mobile_error_offline_title,
    descriptionRes = R.string.mobile_error_offline_description,
    retryable = true,
)

private fun scannerInvalidResultMessage() = DlaFlowUiMessage(
    titleRes = R.string.mobile_error_unknown_title,
    descriptionRes = R.string.mobile_error_unknown_description,
    retryable = false,
)
