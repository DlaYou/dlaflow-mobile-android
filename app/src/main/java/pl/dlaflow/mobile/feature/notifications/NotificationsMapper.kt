package pl.dlaflow.mobile.feature.notifications

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
    destination = mobileAction.type.toNotificationDestination(),
)

private fun String.toNotificationTone(): NotificationTone = when (trim().lowercase()) {
    "info" -> NotificationTone.Info
    "success" -> NotificationTone.Success
    "warning", "error", "attention" -> NotificationTone.Attention
    "neutral" -> NotificationTone.Neutral
    else -> NotificationTone.Neutral
}

private fun String.toNotificationDestination(): NotificationDestination = when (trim()) {
    "OPEN_ORDERS" -> NotificationDestination.Orders
    "OPEN_PRODUCTS" -> NotificationDestination.Products
    "OPEN_MESSAGES" -> NotificationDestination.Messages
    "OPEN_PHOTO_TASKS" -> NotificationDestination.PhotoTasks
    "OPEN_LOGS_SUMMARY" -> NotificationDestination.LogsSummary
    "CONTACT_ADMIN", "OPEN_CONTACT_ADMIN" -> NotificationDestination.ContactAdmin
    else -> NotificationDestination.Unsupported
}
