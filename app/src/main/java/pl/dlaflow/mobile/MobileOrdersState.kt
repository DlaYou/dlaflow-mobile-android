package pl.dlaflow.mobile

import java.net.URLEncoder
import java.util.Locale

enum class MobileOrderFilter(val queryValue: String, val label: String) {
    ALL("", "Wszystkie"),
    NEW("new", "Nowe"),
    TO_SHIP("to-ship", "Do wysyłki"),
    PROBLEMS("problems", "Problemy"),
    MESSAGES("messages", "Wiadomości"),
}

enum class MobileOrderUiTone {
    BRAND,
    INFO,
    SUCCESS,
    WARNING,
    NEUTRAL,
}

fun buildMobileOrdersQuery(search: String, filter: MobileOrderFilter, offset: Int = 0): String {
    val params = mutableListOf(
        "limit" to "20",
        "offset" to offset.coerceAtLeast(0).toString(),
    )
    val trimmedSearch = search.trim()
    if (trimmedSearch.isNotBlank()) {
        params.add("search" to trimmedSearch)
    }
    if (filter != MobileOrderFilter.ALL) {
        params.add("filter" to filter.queryValue)
    }

    return params.joinToString("&") { (key, value) -> "$key=${encodeMobileOrderQueryValue(value)}" }
}

fun normalizeMobileOrdersNextOffset(value: String?): Int? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) {
        return null
    }

    return trimmed.toIntOrNull()?.takeIf { it >= 0 }
}

fun mobileOrderStatusLabel(status: String): String {
    val trimmedStatus = status.trim()
    val normalized = trimmedStatus.lowercase(Locale.ROOT)
    if (normalized.isBlank()) {
        return "Bez statusu"
    }

    return when (normalized) {
        "nowe", "new" -> "Nowe"
        "do wysyłki", "do wysylki", "to_ship", "to-ship" -> "Do wysyłki"
        "w realizacji", "processing" -> "W realizacji"
        "dostarczone", "delivered" -> "Dostarczone"
        "zakończone", "zakonczone", "finished", "completed" -> "Zakończone"
        else -> trimmedStatus.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }
}

fun mobileOrderUiTone(tone: String): MobileOrderUiTone {
    return when (tone.trim().lowercase(Locale.ROOT)) {
        "brand" -> MobileOrderUiTone.BRAND
        "info" -> MobileOrderUiTone.INFO
        "success" -> MobileOrderUiTone.SUCCESS
        "warning" -> MobileOrderUiTone.WARNING
        else -> MobileOrderUiTone.NEUTRAL
    }
}

private fun encodeMobileOrderQueryValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
}
