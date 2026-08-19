package pl.dlaflow.mobile.feature.notifications

import pl.dlaflow.mobile.MobileApiClient

internal interface NotificationsGateway {
    fun load(token: String): NotificationsContent
    fun markRead(token: String, notificationIds: List<String>)
}

internal class MobileApiNotificationsGateway(
    private val clientProvider: () -> MobileApiClient,
) : NotificationsGateway {
    override fun load(token: String): NotificationsContent =
        clientProvider().listNotifications(token, NOTIFICATIONS_PAGE_LIMIT).toNotificationsContent()

    override fun markRead(token: String, notificationIds: List<String>) {
        require(notificationIds.isNotEmpty() && notificationIds.size <= NOTIFICATIONS_PAGE_LIMIT)
        clientProvider().markNotificationsRead(token, notificationIds.toList())
    }
}
