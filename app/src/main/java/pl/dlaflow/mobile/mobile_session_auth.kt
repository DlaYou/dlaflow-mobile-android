package pl.dlaflow.mobile

internal fun shouldClearMobileSessionAfterUnauthorized(
    error: Throwable,
    verifyCurrentSession: () -> Any?,
): Boolean {
    if (error !is MobileApiException || error.statusCode != 401) {
        return false
    }

    return runCatching {
        verifyCurrentSession()
    }.fold(
        onSuccess = { false },
        onFailure = { verificationError ->
            verificationError is MobileApiException && verificationError.statusCode == 401
        },
    )
}

internal fun isSameMobileSessionToken(currentToken: String, requestToken: String): Boolean {
    return currentToken.isNotBlank() && currentToken == requestToken
}
