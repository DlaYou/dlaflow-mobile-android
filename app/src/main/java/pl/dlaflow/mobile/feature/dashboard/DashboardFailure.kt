package pl.dlaflow.mobile.feature.dashboard

import java.net.ConnectException
import java.net.UnknownHostException
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.mobileErrorToUiMessage

internal sealed interface DashboardFailure {
    data object Unauthorized : DashboardFailure
    data object NoAccess : DashboardFailure
    data class Offline(val message: DlaFlowUiMessage) : DashboardFailure
    data class Retryable(val message: DlaFlowUiMessage) : DashboardFailure
}

internal fun mapDashboardFailure(error: Throwable): DashboardFailure = when {
    error is MobileApiException && error.statusCode == 401 -> DashboardFailure.Unauthorized
    error is MobileApiException && error.statusCode == 403 -> DashboardFailure.NoAccess
    error is UnknownHostException || error is ConnectException ->
        DashboardFailure.Offline(mobileErrorToUiMessage(error))
    else -> DashboardFailure.Retryable(mobileErrorToUiMessage(error))
}
