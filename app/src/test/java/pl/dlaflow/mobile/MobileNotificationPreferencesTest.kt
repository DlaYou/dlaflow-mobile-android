package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileNotificationPreferencesTest {
    @Test
    fun `new installs receive every notification category`() {
        val preferences = MobileNotificationPreferences.defaults()

        MobileNotificationCategory.entries.forEach { category ->
            assertTrue(preferences.isEnabled(category))
        }
    }

    @Test
    fun `preferences round trip through compact local encoding`() {
        val preferences = MobileNotificationPreferences.defaults()
            .withEnabled(MobileNotificationCategory.CUSTOMER_MESSAGES, false)
            .withEnabled(MobileNotificationCategory.SHIPMENT_STATUS, false)

        assertEquals(
            preferences,
            parseMobileNotificationPreferences(serializeMobileNotificationPreferences(preferences)),
        )
    }

    @Test
    fun `notification classifier maps panel events to operator categories`() {
        assertEquals(MobileNotificationCategory.NEW_ORDERS, classifyMobileNotification(testNotification("Nowe zamówienie", "OPEN_ORDERS")))
        assertEquals(MobileNotificationCategory.CUSTOMER_MESSAGES, classifyMobileNotification(testNotification("Wiadomość od klienta", "OPEN_MESSAGES")))
        assertEquals(MobileNotificationCategory.ORDER_STATUS, classifyMobileNotification(testNotification("Zmiana statusu zamówienia", "OPEN_ORDERS")))
        assertEquals(MobileNotificationCategory.SHIPMENT_STATUS, classifyMobileNotification(testNotification("Zmiana statusu przesyłki", "OPEN_ORDERS")))
        assertEquals(MobileNotificationCategory.PHOTO_TASKS, classifyMobileNotification(testNotification("Zadanie zdjęciowe", "OPEN_PHOTO_TASKS")))
        assertEquals(MobileNotificationCategory.IMPORTANT_PANEL, classifyMobileNotification(testNotification("Problem integracji", "OPEN_LOGS_SUMMARY")))
    }

    @Test
    fun `disabled category suppresses native notification without affecting others`() {
        val preferences = MobileNotificationPreferences.defaults()
            .withEnabled(MobileNotificationCategory.NEW_ORDERS, false)

        assertFalse(shouldShowNativePanelNotification(testNotification("Nowe zamówienie", "OPEN_ORDERS"), preferences))
        assertTrue(shouldShowNativePanelNotification(testNotification("Wiadomość od klienta", "OPEN_MESSAGES"), preferences))

        val importantDisabled = MobileNotificationPreferences.defaults()
            .withEnabled(MobileNotificationCategory.IMPORTANT_PANEL, false)
        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Informacja z panelu", "OPEN_LOGS_SUMMARY", tone = "info"),
                importantDisabled,
            ),
        )
    }

    @Test
    fun `enabled important panel category accepts every tone`() {
        val preferences = MobileNotificationPreferences.defaults()

        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Informacja z panelu", "OPEN_LOGS_SUMMARY", tone = "info"),
                preferences,
            ),
        )
        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Zakończono synchronizację", "OPEN_LOGS_SUMMARY", tone = "success"),
                preferences,
            ),
        )
    }

    @Test
    fun `technical message synchronization notifications never reach Android`() {
        val preferences = MobileNotificationPreferences.defaults()

        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Wiadomości Gmail: zakończono", "OPEN_MESSAGES", tone = "success"),
                preferences,
            ),
        )
        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Wiadomości Gmail: zakończono", "OPEN_MESSAGES", tone = "info"),
                preferences,
            ),
        )
    }

    @Test
    fun `real customer message and reply notifications reach Android`() {
        val preferences = MobileNotificationPreferences.defaults()

        assertTrue(shouldShowNativePanelNotification(testNotification("Nowa wiadomość od klienta", "OPEN_MESSAGES"), preferences))
        assertTrue(shouldShowNativePanelNotification(testNotification("Klient odpowiedział na wiadomość", "OPEN_MESSAGES"), preferences))
    }

    @Test
    fun `disabled photo tasks suppress direct task alerts`() {
        val preferences = MobileNotificationPreferences.defaults()
            .withEnabled(MobileNotificationCategory.PHOTO_TASKS, false)

        assertFalse(shouldShowNativePhotoTaskNotification(preferences))
        assertTrue(shouldShowNativePhotoTaskNotification(MobileNotificationPreferences.defaults()))
    }

    @Test
    fun `settings summary uses business wording`() {
        assertEquals("Wszystkie typy włączone", mobileNotificationPreferenceSummary(MobileNotificationPreferences.defaults()))
        assertEquals(
            "5 z 6 typów włączonych",
            mobileNotificationPreferenceSummary(
                MobileNotificationPreferences.defaults().withEnabled(MobileNotificationCategory.NEW_ORDERS, false),
            ),
        )
        assertEquals("Powiadomienia wyłączone", mobileNotificationPreferenceSummary(MobileNotificationPreferences(emptySet())))
    }

    private fun testNotification(title: String, actionType: String, tone: String = "info") = MobileAssistantNotification(
        id = title,
        title = title,
        description = "Opis",
        tone = tone,
        source = "DlaFlow",
        account = "Panel",
        occurredAt = "2026-08-19T08:00:00Z",
        readAt = null,
        mobileAction = MobileNotificationAction(actionType, "Otwórz"),
    )
}
