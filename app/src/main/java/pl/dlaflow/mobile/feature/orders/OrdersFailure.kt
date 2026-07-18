package pl.dlaflow.mobile.feature.orders

import java.net.ConnectException
import java.net.UnknownHostException
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.mobileErrorToUiMessage

internal sealed interface OrdersFailure {
    data class Unauthorized(val message: DlaFlowUiMessage) : OrdersFailure
    data object NoAccess : OrdersFailure
    data class NotFound(val message: DlaFlowUiMessage) : OrdersFailure
    data class Offline(val message: DlaFlowUiMessage) : OrdersFailure
    data class Retryable(val message: DlaFlowUiMessage) : OrdersFailure
}

internal fun mapOrdersFailure(error: Throwable): OrdersFailure = when {
    error is MobileApiException && error.statusCode == 401 ->
        OrdersFailure.Unauthorized(mobileErrorToUiMessage(error))
    error is MobileApiException && error.statusCode == 403 -> OrdersFailure.NoAccess
    error is MobileApiException && error.statusCode == 404 ->
        OrdersFailure.NotFound(
            DlaFlowUiMessage(
                titleRes = R.string.orders_not_found_title,
                descriptionRes = R.string.orders_not_found_description,
                retryable = false,
            ),
        )

    error is UnknownHostException || error is ConnectException ->
        OrdersFailure.Offline(mobileErrorToUiMessage(error))

    else -> OrdersFailure.Retryable(mobileErrorToUiMessage(error))
}

internal fun ordersSessionUnconfirmedMessage() = DlaFlowUiMessage(
    titleRes = R.string.orders_session_check_failed_title,
    descriptionRes = R.string.orders_session_check_failed_description,
    retryable = true,
)
