package pl.dlaflow.mobile

internal interface PanelNotificationDeliveryMemory {
    fun readShownPanelAlertIds(): String
    fun saveShownPanelAlertIds(ids: String)
}

/** Describes whether an event should be shown, consumed silently, or ignored. */
internal enum class PanelNotificationDeliveryDecision {
    SHOW,
    SUPPRESS,
    IGNORE,
}

/** Claims a panel event before showing it so FCM and polling share one delivery gate. */
internal fun deliverPanelNotificationOnce(
    memory: PanelNotificationDeliveryMemory,
    notification: MobileAssistantNotification,
    effect: () -> Boolean,
): Boolean = deliverPanelNotificationOnce(
    memory = memory,
    notification = notification,
    decision = { PanelNotificationDeliveryDecision.SHOW },
    effect = effect,
)

internal fun deliverPanelNotificationOnce(
    memory: PanelNotificationDeliveryMemory,
    notification: MobileAssistantNotification,
    decision: () -> PanelNotificationDeliveryDecision,
    effect: () -> Boolean,
): Boolean {
    val id = notification.id.trim()
    if (id.isBlank()) return false

    val shownIds = memory.readShownPanelAlertIds()
    if (hasShownNotificationId(shownIds, id)) return false

    when (runCatching(decision).getOrDefault(PanelNotificationDeliveryDecision.IGNORE)) {
        PanelNotificationDeliveryDecision.IGNORE -> return false
        PanelNotificationDeliveryDecision.SUPPRESS -> {
            memory.saveShownPanelAlertIds(rememberShownNotificationId(shownIds, id))
            return false
        }
        PanelNotificationDeliveryDecision.SHOW -> Unit
    }

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
