package pl.dlaflow.mobile

internal fun forgetShownNotificationId(serialized: String, id: String): String {
    if (id.isBlank()) return serialized
    return serialized.split('|').filter { it.isNotBlank() && it != id }.joinToString("|")
}
