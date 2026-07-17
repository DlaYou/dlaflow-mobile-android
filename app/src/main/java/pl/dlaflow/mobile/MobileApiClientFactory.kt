package pl.dlaflow.mobile

internal fun mobileApiClientForSession(sessionStore: MobileSessionStore): MobileApiClient {
    return MobileApiClient(
        baseUrl = sessionStore.readBaseUrl(),
        requestSigner = AndroidKeystoreMobileRequestSigner(),
        deviceIdProvider = { sessionStore.readDeviceId() },
    )
}

internal fun mobileApiClientForBaseUrl(baseUrl: String, sessionStore: MobileSessionStore? = null): MobileApiClient {
    return MobileApiClient(
        baseUrl = baseUrl,
        requestSigner = AndroidKeystoreMobileRequestSigner(),
        deviceIdProvider = { sessionStore?.readDeviceId().orEmpty() },
    )
}

internal fun mobileApiClientForDevice(baseUrl: String, deviceId: String): MobileApiClient {
    return MobileApiClient(
        baseUrl = baseUrl,
        requestSigner = AndroidKeystoreMobileRequestSigner(),
        deviceIdProvider = { deviceId },
    )
}
