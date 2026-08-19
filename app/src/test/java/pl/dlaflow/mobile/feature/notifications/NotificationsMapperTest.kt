package pl.dlaflow.mobile.feature.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.MobileAssistantNotification
import pl.dlaflow.mobile.MobileNotificationAction
import pl.dlaflow.mobile.MobileNotificationsPage

class NotificationsMapperTest {
    @Test
    fun `maps transport page into immutable feature content`() {
        val transport = mutableListOf(notification(id = " notification-1 ", action = "OPEN_ORDERS"))

        val content = MobileNotificationsPage(
            attentionCount = 3,
            unreadAttentionCount = 2,
            unreadCount = 4,
            notifications = transport,
        ).toNotificationsContent()
        transport.clear()

        assertEquals(1, content.items.size)
        assertEquals("notification-1", content.items.single().id)
        assertEquals(NotificationDestination.Orders, content.items.single().destination)
        assertEquals(NotificationTone.Info, content.items.single().tone)
        assertEquals(3, content.attentionCount)
        assertEquals(2, content.unreadAttentionCount)
        assertEquals(4, content.unreadCount)
    }

    @Test
    fun `maps only closed action and tone allowlists`() {
        val content = MobileNotificationsPage(
            attentionCount = -1,
            unreadAttentionCount = -1,
            unreadCount = -1,
            notifications = listOf(
                notification(id = "warning", tone = "warning", action = "OPEN_PHOTO_TASKS"),
                notification(id = "unknown", tone = "brand-purple", action = "https://unsafe.example"),
            ),
        ).toNotificationsContent()

        assertEquals(NotificationTone.Attention, content.items[0].tone)
        assertEquals(NotificationDestination.PhotoTasks, content.items[0].destination)
        assertEquals(NotificationTone.Neutral, content.items[1].tone)
        assertEquals(NotificationDestination.Unsupported, content.items[1].destination)
        assertEquals(0, content.attentionCount)
        assertEquals(0, content.unreadAttentionCount)
        assertEquals(0, content.unreadCount)
    }

    @Test
    fun `blank read timestamp remains unread`() {
        val item = notification(id = "unread", readAt = "  ").toNotificationItem()

        assertTrue(item.isUnread)
    }

    private fun notification(
        id: String,
        tone: String = "info",
        action: String = "OPEN_LOGS_SUMMARY",
        readAt: String? = null,
    ) = MobileAssistantNotification(
        id = id,
        title = "Nowe zdarzenie",
        description = "Sprawdź szczegóły w panelu.",
        tone = tone,
        source = "Panel",
        account = "Sklep",
        occurredAt = "2026-07-18T10:00:00.000Z",
        readAt = readAt,
        mobileAction = MobileNotificationAction(type = action, label = "Otwórz"),
    )
}
