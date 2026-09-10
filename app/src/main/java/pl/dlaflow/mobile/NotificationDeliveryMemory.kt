package pl.dlaflow.mobile

internal interface PanelNotificationDeliveryMemory {
    fun readShownPanelAlertIds(): String
    fun saveShownPanelAlertIds(ids: String)
}

/** Claims a panel event before showing it so FCM and polling share one delivery gate. */
internal fun deliverPanelNotificationOnce(
    memory: PanelNotificationDeliveryMemory,
    notification: MobileAssistantNotification,
    effect: () -> Boolean,
): Boolean {
    val id = notification.id.trim()
    if (id.isBlank()) return false

    val shownIds = memory.readShownPanelAlertIds()
    if (hasShownNotificationId(shownIds, id)) return false

    memory.saveShownPanelAlertIds(rememberShownNotificationId(shownIds, id))
    val delivered = runCatching(effect).getOrDefault(false)
    if (!delivered) {
        memory.saveShownPanelAlertIds(
            forgetShownNotificationId(memory.readShownPanelAlertIds(), id),
        )
    }
    return delivered
}

internal fun forgetShownNotificationId(serialized: String, id: String): String {
    if (id.isBlank()) return serialized
    return serialized.split('|').filter { it.isNotBlank() && it != id }.joinToString("|")
}
