package pl.dlaflow.mobile.feature.pairing

import java.util.Locale

private const val pairingCodePrefix = "dlaflow-pair:v1:"
private const val pairingDeviceNameMinLength = 2
internal const val pairingDeviceNameMaxLength = 80
private val pairingCodePattern = Regex("^[A-Z0-9]{3}-?[A-Z0-9]{3}$")

internal enum class PairingDeviceNameError {
    REQUIRED,
    TOO_SHORT,
    TOO_LONG,
    CONTROL_CHARACTER,
}

internal fun formatPairingCodeInput(value: String): String {
    val compact = value.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }.take(6)
    return if (compact.length > 3) compact.take(3) + "-" + compact.drop(3) else compact
}

internal fun canonicalPairingCodeOrNull(value: String): String? {
    val candidate = value.trim().uppercase(Locale.ROOT).replace(" ", "")
    if (!pairingCodePattern.matches(candidate)) return null
    val compact = candidate.replace("-", "")
    return compact.take(3) + "-" + compact.drop(3)
}

internal fun pairingCodeFromQrOrNull(rawValue: String?): String? {
    val raw = rawValue?.trim().orEmpty()
    val candidate = if (raw.startsWith(pairingCodePrefix, ignoreCase = true)) {
        raw.substring(pairingCodePrefix.length)
    } else {
        raw
    }
    return canonicalPairingCodeOrNull(candidate)
}

internal fun normalizePairingDeviceName(value: String): String = value.trim()

internal fun pairingDeviceNameError(value: String): PairingDeviceNameError? {
    val normalized = normalizePairingDeviceName(value)
    return when {
        normalized.isEmpty() -> PairingDeviceNameError.REQUIRED
        normalized.length < pairingDeviceNameMinLength -> PairingDeviceNameError.TOO_SHORT
        normalized.length > pairingDeviceNameMaxLength -> PairingDeviceNameError.TOO_LONG
        normalized.any(Char::isISOControl) -> PairingDeviceNameError.CONTROL_CHARACTER
        else -> null
    }
}
