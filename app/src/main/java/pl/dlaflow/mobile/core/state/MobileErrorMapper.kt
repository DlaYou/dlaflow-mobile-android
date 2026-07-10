package pl.dlaflow.mobile.core.state

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.network.MobileApiException

internal fun mobileErrorToUiMessage(error: Throwable): DlaFlowUiMessage {
    return when (error) {
        is UnknownHostException,
        is ConnectException,
        -> DlaFlowUiMessage(
            titleRes = R.string.mobile_error_offline_title,
            descriptionRes = R.string.mobile_error_offline_description,
            retryable = true,
        )

        is SocketTimeoutException -> DlaFlowUiMessage(
            titleRes = R.string.mobile_error_timeout_title,
            descriptionRes = R.string.mobile_error_timeout_description,
            retryable = true,
        )

        is MobileApiException -> when {
            error.statusCode == 401 -> DlaFlowUiMessage(
                titleRes = R.string.mobile_error_session_title,
                descriptionRes = R.string.mobile_error_session_description,
                retryable = false,
            )

            error.statusCode == 403 -> DlaFlowUiMessage(
                titleRes = R.string.mobile_error_no_access_title,
                descriptionRes = R.string.mobile_error_no_access_description,
                retryable = false,
            )

            error.statusCode == 400 || error.statusCode == 422 -> DlaFlowUiMessage(
                titleRes = R.string.mobile_error_validation_title,
                descriptionRes = R.string.mobile_error_validation_description,
                retryable = false,
            )

            error.statusCode == 429 -> DlaFlowUiMessage(
                titleRes = R.string.mobile_error_rate_limit_title,
                descriptionRes = R.string.mobile_error_rate_limit_description,
                retryable = true,
            )

            error.statusCode >= 500 -> DlaFlowUiMessage(
                titleRes = R.string.mobile_error_server_title,
                descriptionRes = R.string.mobile_error_server_description,
                retryable = true,
            )

            else -> unknownMobileErrorMessage()
        }

        else -> unknownMobileErrorMessage()
    }
}

private fun unknownMobileErrorMessage() = DlaFlowUiMessage(
    titleRes = R.string.mobile_error_unknown_title,
    descriptionRes = R.string.mobile_error_unknown_description,
    retryable = true,
)
