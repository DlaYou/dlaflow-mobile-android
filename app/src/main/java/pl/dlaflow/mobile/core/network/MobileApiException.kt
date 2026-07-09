package pl.dlaflow.mobile.core.network

class MobileApiException(
    val statusCode: Int,
    val code: String,
    message: String,
) : IllegalStateException(message)
