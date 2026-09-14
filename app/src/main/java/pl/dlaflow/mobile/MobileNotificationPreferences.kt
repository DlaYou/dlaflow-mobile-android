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
        action == "OPEN_MESSAGES" && (
            "nowa wiadomość" in text ||
                "nowa wiadomosc" in text ||
                "od klienta" in text ||
                "klient napisał" in text ||
                "klient napisal" in text ||
                "klient odpowiedział" in text ||
                "klient odpowiedzial" in text
            ) -> MobileNotificationCategory.CUSTOMER_MESSAGES
        "nowe zamówienie" in text || "nowe zamowienie" in text -> MobileNotificationCategory.NEW_ORDERS
        "statusu przesyłki" in text || "statusu przesylki" in text || "w drodze" in text || "w trasie" in text || "śledzenia" in text || "sledzenia" in text -> MobileNotificationCategory.SHIPMENT_STATUS
        "statusu zamówienia" in text || "statusu zamowienia" in text -> MobileNotificationCategory.ORDER_STATUS
        else -> MobileNotificationCategory.IMPORTANT_PANEL
    }
}

/** Keeps the FCM contract independent from localized notification copy. */
internal fun mobileNotificationCategoryForPushEvent(event: String): MobileNotificationCategory? = when (event.trim()) {
    "order.created" -> MobileNotificationCategory.NEW_ORDERS
    "message.created" -> MobileNotificationCategory.CUSTOMER_MESSAGES
    else -> null
}

/** Identifies the transport that is allowed to create a native alert. */
internal enum class MobileNotificationDeliveryOrigin {
    FCM,
    PANEL_HISTORY,
    PHOTO_TASK,
}

fun shouldShowNativePanelNotification(
    notification: MobileAssistantNotification,
    preferences: MobileNotificationPreferences,
): Boolean {
    val origin = if (notification.source.trim().equals("push", ignoreCase = true)) {
        MobileNotificationDeliveryOrigin.FCM
    } else {
        MobileNotificationDeliveryOrigin.PANEL_HISTORY
    }
    return shouldShowNativePanelNotification(notification, preferences, origin)
}

internal fun shouldShowNativePanelNotification(
    notification: MobileAssistantNotification,
    preferences: MobileNotificationPreferences,
    origin: MobileNotificationDeliveryOrigin,
): Boolean {
    return mobileNotificationDeliveryDecision(notification, preferences, origin) ==
        PanelNotificationDeliveryDecision.SHOW
}

internal fun mobileNotificationDeliveryDecision(
    notification: MobileAssistantNotification,
    preferences: MobileNotificationPreferences,
    origin: MobileNotificationDeliveryOrigin,
): PanelNotificationDeliveryDecision {
    return mobileNotificationDeliveryDecision(
        category = classifyMobileNotification(notification),
        tone = notification.tone.trim().lowercase(Locale.ROOT),
        preferences = preferences,
        origin = origin,
    )
}

internal fun mobileNotificationDeliveryDecision(
    category: MobileNotificationCategory,
    tone: String,
    preferences: MobileNotificationPreferences,
    origin: MobileNotificationDeliveryOrigin,
): PanelNotificationDeliveryDecision {
    val normalizedTone = tone.trim().lowercase(Locale.ROOT)
    // A red alert is the one allowed exception for a historical panel row.
    // It is always governed by the dedicated important-panel switch, even if
    // its action text happens to look like an order or shipment update.
    if (normalizedTone == "error") {
        return when (origin) {
            MobileNotificationDeliveryOrigin.PHOTO_TASK -> PanelNotificationDeliveryDecision.IGNORE
            MobileNotificationDeliveryOrigin.FCM,
            MobileNotificationDeliveryOrigin.PANEL_HISTORY,
            -> if (preferences.isEnabled(MobileNotificationCategory.IMPORTANT_PANEL)) {
                PanelNotificationDeliveryDecision.SHOW
            } else {
                PanelNotificationDeliveryDecision.SUPPRESS
            }
        }
    }

    if (!preferences.isEnabled(category)) {
        return when (origin) {
            // Explicit events are consumed while disabled so enabling a switch
            // later cannot replay an old FCM event.
            MobileNotificationDeliveryOrigin.FCM,
            MobileNotificationDeliveryOrigin.PHOTO_TASK,
            -> PanelNotificationDeliveryDecision.SUPPRESS
            // Ordinary history is not eligible for a native alert at all, so
            // it should not consume the bounded delivery memory.
            MobileNotificationDeliveryOrigin.PANEL_HISTORY -> PanelNotificationDeliveryDecision.IGNORE
        }
    }

    return when (origin) {
        // FCM is an explicit event contract. Every category is eligible here;
        // the category switch remains the final gate for future event types.
        MobileNotificationDeliveryOrigin.FCM -> PanelNotificationDeliveryDecision.SHOW
        // The notifications endpoint is a history snapshot. It must never
        // replay ordinary messages, orders, statuses or successes as a native
        // alert. Critical errors are handled by the branch above.
        MobileNotificationDeliveryOrigin.PANEL_HISTORY -> PanelNotificationDeliveryDecision.IGNORE
        MobileNotificationDeliveryOrigin.PHOTO_TASK -> if (category == MobileNotificationCategory.PHOTO_TASKS) {
            PanelNotificationDeliveryDecision.SHOW
        } else {
            PanelNotificationDeliveryDecision.IGNORE
        }
    }
}

fun shouldShowNativePhotoTaskNotification(preferences: MobileNotificationPreferences): Boolean =
    mobileNotificationDeliveryDecision(
        category = MobileNotificationCategory.PHOTO_TASKS,
        tone = "",
        preferences = preferences,
        origin = MobileNotificationDeliveryOrigin.PHOTO_TASK,
    ) == PanelNotificationDeliveryDecision.SHOW
