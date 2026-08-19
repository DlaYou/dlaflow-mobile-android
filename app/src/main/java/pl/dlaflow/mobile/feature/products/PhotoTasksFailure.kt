package pl.dlaflow.mobile.feature.products

import java.net.ConnectException
import java.net.UnknownHostException
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.mobileErrorToUiMessage

internal sealed interface PhotoTasksFailure {
    data class Unauthorized(val message: DlaFlowUiMessage) : PhotoTasksFailure
    data class NoAccess(val message: DlaFlowUiMessage) : PhotoTasksFailure
    data class Offline(val message: DlaFlowUiMessage) : PhotoTasksFailure
    data class InvalidPayload(val message: DlaFlowUiMessage) : PhotoTasksFailure
    data class Retryable(val message: DlaFlowUiMessage) : PhotoTasksFailure
}

internal fun mapPhotoTasksFailure(error: Throwable): PhotoTasksFailure = when {
    error is MobileApiException && error.statusCode == 401 ->
        PhotoTasksFailure.Unauthorized(mobileErrorToUiMessage(error))
    error is MobileApiException && error.statusCode == 403 ->
        PhotoTasksFailure.NoAccess(mobileErrorToUiMessage(error))
    error is UnknownHostException || error is ConnectException ->
        PhotoTasksFailure.Offline(mobileErrorToUiMessage(error))
    error is InvalidPhotoTaskPayloadException ->
        PhotoTasksFailure.InvalidPayload(invalidPhotoTaskPayloadMessage())
    else -> PhotoTasksFailure.Retryable(mobileErrorToUiMessage(error))
}

internal fun photoTasksSessionUnconfirmedMessage() = DlaFlowUiMessage(
    titleRes = R.string.mobile_error_offline_title,
    descriptionRes = R.string.mobile_error_offline_description,
    retryable = true,
)

internal fun invalidPhotoUploadMessage() = DlaFlowUiMessage(
    titleRes = R.string.mobile_error_validation_title,
    descriptionRes = R.string.mobile_error_validation_description,
    retryable = false,
)

internal fun invalidPhotoTaskPayloadMessage() = DlaFlowUiMessage(
    titleRes = R.string.mobile_error_unknown_title,
    descriptionRes = R.string.mobile_error_unknown_description,
    retryable = false,
)
