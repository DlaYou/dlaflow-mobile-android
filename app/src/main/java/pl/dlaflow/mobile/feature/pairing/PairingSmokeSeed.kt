package pl.dlaflow.mobile.feature.pairing

internal data class PairingSmokeSeed(
    val baseUrl: String,
    val pairingCode: String,
    val deviceName: String?,
) {
    val shouldAutoSubmit: Boolean
        get() = deviceName != null
}

internal fun pairingSmokeSeed(
    apiUrl: String?,
    pairingCode: String?,
    deviceName: String?,
): PairingSmokeSeed? {
    val baseUrl = apiUrl?.trim().orEmpty()
    val code = canonicalPairingCodeOrNull(pairingCode.orEmpty()) ?: return null
    if (baseUrl.isBlank()) return null
    val normalizedName = deviceName
        ?.let(::normalizePairingDeviceName)
        ?.takeIf { pairingDeviceNameError(it) == null }
    return PairingSmokeSeed(baseUrl, code, normalizedName)
}
