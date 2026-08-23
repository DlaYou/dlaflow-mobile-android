package pl.dlaflow.mobile

import java.util.Locale

enum class MobileNotificationCategory(
    val key: String,
    val label: String,
    val description: String,
) {
    NEW_ORDERS("new_orders", "Nowe zamówienia", "Gdy wpada nowe zamówienie do obsługi."),
    CUSTOMER_MESSAGES("customer_messages", "Wiadomości od klientów", "Gdy klient napisze w sprawie zamówienia."),
    ORDER_STATUS("order_status", "Zmiany statusu zamówień", "Gdy zmieni się status realizacji zamówienia."),
    SHIPMENT_STATUS("shipment_status", "Zmiany statusu przesyłek", "Gdy zmieni się etap dostawy lub śledzenia."),
    PHOTO_TASKS("photo_tasks", "Zadania zdjęciowe", "Gdy panel wyśle nowe zadanie wykonania zdjęć."),
    IMPORTANT_PANEL("important_panel", "Ważne sprawy z panelu", "Problemy i działania wymagające uwagi."),
}

data class MobileNotificationPreferences(
    val enabledKeys: Set<String> = MobileNotificationCategory.entries.map { it.key }.toSet(),
) {
    fun isEnabled(category: MobileNotificationCategory): Boolean = category.key in enabledKeys

    fun withEnabled(category: MobileNotificationCategory, enabled: Boolean): MobileNotificationPreferences {
        return copy(
            enabledKeys = if (enabled) enabledKeys + category.key else enabledKeys - category.key,
        )
    }

    fun enabledCount(): Int = MobileNotificationCategory.entries.count(::isEnabled)

    companion object {
        fun defaults(): MobileNotificationPreferences = MobileNotificationPreferences()
    }
}

fun mobileNotificationPreferenceSummary(preferences: MobileNotificationPreferences): String {
    val enabled = preferences.enabledCount()
    return when {
        enabled == 0 -> "Powiadomienia wyłączone"
        enabled == MobileNotificationCategory.entries.size -> "Wszystkie typy włączone"
        else -> "$enabled z ${MobileNotificationCategory.entries.size} typów włączonych"
    }
}

private const val noNotificationCategoriesKey = "__none__"

internal fun serializeMobileNotificationPreferences(preferences: MobileNotificationPreferences): String {
    val enabled = MobileNotificationCategory.entries
        .filter(preferences::isEnabled)
        .joinToString("|") { it.key }
    return enabled.ifBlank { noNotificationCategoriesKey }
}

internal fun parseMobileNotificationPreferences(serialized: String): MobileNotificationPreferences {
    val value = serialized.trim()
    if (value.isBlank()) return MobileNotificationPreferences.defaults()
    if (value == noNotificationCategoriesKey) return MobileNotificationPreferences(emptySet())

    val knownKeys = MobileNotificationCategory.entries.map { it.key }.toSet()
    return MobileNotificationPreferences(
        enabledKeys = value.split("|").filter { it in knownKeys }.toSet(),
    )
}

fun classifyMobileNotification(notification: MobileAssistantNotification): MobileNotificationCategory {
    val title = notification.title.lowercase(Locale.ROOT)
    val description = notification.description.lowercase(Locale.ROOT)
    val text = "$title $description"
    val action = notification.mobileAction.type.uppercase(Locale.ROOT)

    return when {
        action == "OPEN_PHOTO_TASKS" -> MobileNotificationCategory.PHOTO_TASKS
        action == "OPEN_MESSAGES" || "wiadomość" in text || "wiadomosc" in text || "klient" in text -> MobileNotificationCategory.CUSTOMER_MESSAGES
        "nowe zamówienie" in text || "nowe zamowienie" in text -> MobileNotificationCategory.NEW_ORDERS
        "statusu przesyłki" in text || "statusu przesylki" in text || "w drodze" in text || "w trasie" in text || "śledzenia" in text || "sledzenia" in text -> MobileNotificationCategory.SHIPMENT_STATUS
        "statusu zamówienia" in text || "statusu zamowienia" in text -> MobileNotificationCategory.ORDER_STATUS
        else -> MobileNotificationCategory.IMPORTANT_PANEL
    }
}

fun shouldShowNativePanelNotification(
    notification: MobileAssistantNotification,
    preferences: MobileNotificationPreferences,
): Boolean {
    val category = classifyMobileNotification(notification)
    return preferences.isEnabled(category)
}

fun shouldShowNativePhotoTaskNotification(preferences: MobileNotificationPreferences): Boolean =
    preferences.isEnabled(MobileNotificationCategory.PHOTO_TASKS)
