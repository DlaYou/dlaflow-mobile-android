package pl.dlaflow.mobile.feature.messages

import java.net.ConnectException
import java.net.UnknownHostException
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.mobileErrorToUiMessage

internal sealed interface MessagesFailure {
    data class Unauthorized(val message: DlaFlowUiMessage) : MessagesFailure
    data object NoAccess : MessagesFailure
    data class Offline(val message: DlaFlowUiMessage) : MessagesFailure
    data class Retryable(val message: DlaFlowUiMessage) : MessagesFailure
}

internal fun mapMessagesFailure(error: Throwable): MessagesFailure = when {
    error is MobileApiException && error.statusCode == 401 ->
        MessagesFailure.Unauthorized(mobileErrorToUiMessage(error))
    error is MobileApiException && error.statusCode == 403 -> MessagesFailure.NoAccess
    error is UnknownHostException || error is ConnectException ->
        MessagesFailure.Offline(mobileErrorToUiMessage(error))
    else -> MessagesFailure.Retryable(mobileErrorToUiMessage(error))
}
