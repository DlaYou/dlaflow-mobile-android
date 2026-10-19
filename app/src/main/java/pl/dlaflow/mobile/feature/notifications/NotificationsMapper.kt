package pl.dlaflow.mobile.feature.notifications

import java.util.Locale
import pl.dlaflow.mobile.MobileAssistantNotification
import pl.dlaflow.mobile.MobileNotificationsPage

internal fun MobileNotificationsPage.toNotificationsContent() = NotificationsContent(
    items = notifications.map(MobileAssistantNotification::toNotificationItem),
    attentionCount = attentionCount.coerceAtLeast(0),
    unreadAttentionCount = unreadAttentionCount.coerceAtLeast(0),
    unreadCount = unreadCount.coerceAtLeast(0),
)

internal fun MobileAssistantNotification.toNotificationItem() = NotificationItem(
    id = id.trim(),
    title = title,
    description = description,
    tone = tone.toNotificationTone(),
    source = source,
    account = account,
    occurredAt = occurredAt,
    readAt = readAt?.trim()?.takeIf(String::isNotEmpty),
    actionLabel = mobileAction.label.trim().takeIf(String::isNotEmpty),
    destination = notificationDestinationFor(
        actionType = mobileAction.type,
        title = title,
        description = description,
        orderNumber = mobileAction.orderNumber,
        filter = mobileAction.filter,
    ),
)

internal fun notificationEffectFor(
    actionType: String,
    title: String,
    description: String,
    orderNumber: String? = null,
    filter: String? = null,
): NotificationsEffect = notificationEffectFor(
    notificationDestinationFor(actionType, title, description, orderNumber, filter),
)

internal fun notificationEffectFor(destination: NotificationDestination): NotificationsEffect = when (destination) {
    NotificationDestination.Orders -> NotificationsEffect.OpenOrders
    is NotificationDestination.Order -> NotificationsEffect.OpenOrder(destination.orderNumber)
    NotificationDestination.OverdueOrders -> NotificationsEffect.OpenOverdueOrders
    NotificationDestination.Products -> NotificationsEffect.OpenProducts
    NotificationDestination.Messages -> NotificationsEffect.OpenMessages
    NotificationDestination.PhotoTasks -> NotificationsEffect.OpenPhotoTasks
    NotificationDestination.LogsSummary -> NotificationsEffect.OpenDashboard()
    NotificationDestination.ContactAdmin -> NotificationsEffect.OpenTeamSettings
    NotificationDestination.Unsupported -> NotificationsEffect.OpenDashboard(explainFallback = true)
}

internal fun notificationDestinationFor(
    actionType: String,
    title: String = "",
    description: String = "",
    orderNumber: String? = null,
    filter: String? = null,
): NotificationDestination {
    val normalizedAction = actionType.trim().uppercase(Locale.ROOT)
    val isOrdersAction = normalizedAction in setOf("OPEN_ORDERS", "OPEN_ORDER", "ORDERS")
    if (isOrdersAction) {
        orderNumber.normalizedNotificationOrderNumber()?.let { return NotificationDestination.Order(it) }
        if (filter?.trim()?.uppercase(Locale.ROOT) == "OVERDUE") {
            return NotificationDestination.OverdueOrders
        }
    }
    if (normalizedAction.isEmpty() || normalizedAction == "OPEN_LOGS_SUMMARY") {
        if ("wiadomo" in "$title $description".lowercase(Locale.ROOT)) {
            return NotificationDestination.Messages
        }
    }

    return normalizedAction.toNotificationDestination()
}

private fun String?.normalizedNotificationOrderNumber(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }?.take(200)

private fun String.toNotificationTone(): NotificationTone = when (trim().lowercase()) {
    "info" -> NotificationTone.Info
    "success" -> NotificationTone.Success
    "warning", "error", "attention" -> NotificationTone.Attention
    "neutral" -> NotificationTone.Neutral
    else -> NotificationTone.Neutral
}

private fun String.toNotificationDestination(): NotificationDestination = when (trim().uppercase(Locale.ROOT)) {
    "OPEN_ORDERS", "OPEN_ORDER", "ORDERS" -> NotificationDestination.Orders
    "OPEN_PRODUCTS" -> NotificationDestination.Products
    "OPEN_MESSAGES", "MESSAGES" -> NotificationDestination.Messages
    "OPEN_PHOTO_TASKS" -> NotificationDestination.PhotoTasks
    "OPEN_LOGS_SUMMARY" -> NotificationDestination.LogsSummary
    "CONTACT_ADMIN", "OPEN_CONTACT_ADMIN" -> NotificationDestination.ContactAdmin
    else -> NotificationDestination.Unsupported
}
