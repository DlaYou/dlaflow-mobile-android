package pl.dlaflow.mobile.feature.notifications

import java.net.ConnectException
import java.net.UnknownHostException
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.mobileErrorToUiMessage

internal sealed interface NotificationsFailure {
    data class Unauthorized(val message: DlaFlowUiMessage) : NotificationsFailure
    data class NoAccess(val message: DlaFlowUiMessage) : NotificationsFailure
    data class Offline(val message: DlaFlowUiMessage) : NotificationsFailure
    data class Retryable(val message: DlaFlowUiMessage) : NotificationsFailure
}

internal fun mapNotificationsFailure(error: Throwable): NotificationsFailure = when {
    error is MobileApiException && error.statusCode == 401 ->
        NotificationsFailure.Unauthorized(mobileErrorToUiMessage(error))

    error is MobileApiException && error.statusCode == 403 ->
        NotificationsFailure.NoAccess(mobileErrorToUiMessage(error))

    error is UnknownHostException || error is ConnectException ->
        NotificationsFailure.Offline(mobileErrorToUiMessage(error))

    else -> NotificationsFailure.Retryable(mobileErrorToUiMessage(error))
}

internal fun notificationsSessionUnconfirmedMessage() = DlaFlowUiMessage(
    titleRes = R.string.mobile_error_offline_title,
    descriptionRes = R.string.mobile_error_offline_description,
    retryable = true,
)
