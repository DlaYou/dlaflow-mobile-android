package pl.dlaflow.mobile.feature.products

import java.net.ConnectException
import java.net.UnknownHostException
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.mobileErrorToUiMessage

internal sealed interface ProductsFailure {
    data class Unauthorized(val message: DlaFlowUiMessage) : ProductsFailure
    data class NoAccess(val message: DlaFlowUiMessage) : ProductsFailure
    data class Offline(val message: DlaFlowUiMessage) : ProductsFailure
    data class InvalidResult(val message: DlaFlowUiMessage) : ProductsFailure
    data class Retryable(val message: DlaFlowUiMessage) : ProductsFailure
}

internal fun mapProductsFailure(error: Throwable): ProductsFailure = when {
    error is MobileApiException && error.statusCode == 401 -> ProductsFailure.Unauthorized(mobileErrorToUiMessage(error))
    error is MobileApiException && error.statusCode == 403 -> ProductsFailure.NoAccess(mobileErrorToUiMessage(error))
    error is UnknownHostException || error is ConnectException -> ProductsFailure.Offline(mobileErrorToUiMessage(error))
    error is InvalidProductsResultException -> ProductsFailure.InvalidResult(productsInvalidResultMessage())
    else -> ProductsFailure.Retryable(mobileErrorToUiMessage(error))
}

internal fun productsSessionUnconfirmedMessage() = DlaFlowUiMessage(
    titleRes = R.string.mobile_error_offline_title,
    descriptionRes = R.string.mobile_error_offline_description,
    retryable = true,
)

internal fun productsQuickEditValidationMessage(error: ProductsQuickEditValidationError) = DlaFlowUiMessage(
    titleRes = R.string.mobile_products_quick_edit_title,
    descriptionRes = when (error) {
        ProductsQuickEditValidationError.INVALID_NUMBER -> R.string.mobile_products_quick_edit_invalid_number
        ProductsQuickEditValidationError.NEGATIVE -> R.string.mobile_products_quick_edit_negative
        ProductsQuickEditValidationError.STOCK_NOT_INTEGER -> R.string.mobile_products_quick_edit_stock_integer
        ProductsQuickEditValidationError.TOO_HIGH -> R.string.mobile_products_quick_edit_too_high
    },
    retryable = false,
)

private fun productsInvalidResultMessage() = DlaFlowUiMessage(
    titleRes = R.string.mobile_error_unknown_title,
    descriptionRes = R.string.mobile_error_unknown_description,
    retryable = false,
)
