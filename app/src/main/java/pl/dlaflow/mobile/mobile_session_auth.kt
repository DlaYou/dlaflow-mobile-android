package pl.dlaflow.mobile

import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import pl.dlaflow.mobile.core.network.MobileApiException

internal fun isRetryableSavedSessionFailure(error: Throwable): Boolean {
    var current: Throwable? = error
    while (current != null) {
        if (current is MobileApiException) {
            return current.statusCode == 408 ||
                current.statusCode == 425 ||
                current.statusCode == 429 ||
                current.statusCode >= 500
        }
        if (current is UnknownHostException ||
            current is ConnectException ||
            current is SocketException ||
            current is SocketTimeoutException ||
            current is IOException
        ) {
            return true
        }
        current = current.cause
    }
    return false
}

internal fun isConfirmedMobileSessionUnauthorized(error: Throwable): Boolean {
    return error is MobileApiException && error.statusCode == 401
}

internal fun shouldClearMobileSessionAfterUnauthorized(
    error: Throwable,
    onSessionValid: () -> Unit = {},
    onSessionUnconfirmed: () -> Unit = {},
    onSessionUnconfirmedWithError: (Throwable) -> Unit = {},
    verifyCurrentSession: () -> Any?,
): Boolean {
    if (error !is MobileApiException || error.statusCode != 401) {
        return false
    }

    return runCatching {
        verifyCurrentSession()
    }.fold(
        onSuccess = {
            onSessionValid()
            false
        },
        onFailure = { verificationError ->
            val revoked = verificationError is MobileApiException && verificationError.statusCode == 401
            if (!revoked) {
                onSessionUnconfirmed()
                onSessionUnconfirmedWithError(verificationError)
            }
            revoked
        },
    )
}

internal fun isSameMobileSessionToken(currentToken: String, requestToken: String): Boolean {
    return currentToken.isNotBlank() && currentToken == requestToken
}
