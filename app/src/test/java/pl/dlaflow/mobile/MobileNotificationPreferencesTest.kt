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
    fun `push event names map to explicit business categories`() {
        assertEquals(MobileNotificationCategory.NEW_ORDERS, mobileNotificationCategoryForPushEvent("order.created"))
        assertEquals(MobileNotificationCategory.CUSTOMER_MESSAGES, mobileNotificationCategoryForPushEvent(" message.created "))
        assertEquals(null, mobileNotificationCategoryForPushEvent("order.updated"))
    }

    @Test
    fun `disabled category suppresses native notification without affecting others`() {
        val preferences = MobileNotificationPreferences.defaults()
            .withEnabled(MobileNotificationCategory.NEW_ORDERS, false)

        assertFalse(shouldShowNativePanelNotification(testNotification("Nowe zamówienie", "OPEN_ORDERS", source = "push"), preferences))
        assertTrue(shouldShowNativePanelNotification(testNotification("Wiadomość od klienta", "OPEN_MESSAGES", source = "push"), preferences))

        val importantDisabled = MobileNotificationPreferences.defaults()
            .withEnabled(MobileNotificationCategory.IMPORTANT_PANEL, false)
        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Problem integracji", "OPEN_LOGS_SUMMARY", tone = "error"),
                importantDisabled,
            ),
        )
    }

    @Test
    fun `only new orders customer messages and red panel alerts reach Android`() {
        val preferences = MobileNotificationPreferences.defaults()

        assertTrue(
            shouldShowNativePanelNotification(
                testNotification("Nowe zamówienie", "OPEN_ORDERS", source = "push"),
                preferences,
            ),
        )
        assertTrue(
            shouldShowNativePanelNotification(
                testNotification("Nowa wiadomość od klienta", "OPEN_MESSAGES", source = "push"),
                preferences,
            ),
        )
        assertTrue(
            shouldShowNativePanelNotification(
                testNotification(
                    "Problem integracji",
                    "OPEN_LOGS_SUMMARY",
                    tone = "error",
                    source = "DlaFlow",
                ),
                preferences,
            ),
        )
        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Wymaga decyzji", "OPEN_LOGS_SUMMARY", tone = "warning"),
                preferences,
            ),
        )
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
        assertTrue(
            shouldShowNativePanelNotification(
                testNotification("Zmiana statusu zamówienia", "OPEN_ORDERS", tone = "error", source = "DlaFlow"),
                preferences,
            ),
        )
        assertTrue(
            shouldShowNativePanelNotification(
                testNotification("Zmiana statusu przesyłki", "OPEN_ORDERS", tone = "error", source = "DlaFlow"),
                preferences,
            ),
        )
    }

    @Test
    fun `stored panel messages and orders never become native alerts`() {
        val preferences = MobileNotificationPreferences.defaults()

        assertFalse(
            shouldShowNativePanelNotification(
                testNotification(
                    title = "Nowa wiadomość od klienta",
                    actionType = "OPEN_MESSAGES",
                    tone = "info",
                    description = "Odebrano nową wiadomość.",
                    source = "Gmail",
                ),
                preferences,
            ),
        )
        assertFalse(
            shouldShowNativePanelNotification(
                testNotification(
                    title = "Nowe zamówienie",
                    actionType = "OPEN_ORDERS",
                    tone = "info",
                    description = "Zamówienie czeka na obsługę.",
                    source = "Allegro",
                ),
                preferences,
            ),
        )
    }

    @Test
    fun `critical red panel alert uses important panel preference before text category`() {
        val disabled = MobileNotificationPreferences.defaults()
            .withEnabled(MobileNotificationCategory.IMPORTANT_PANEL, false)

        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Zmiana statusu zamówienia", "OPEN_ORDERS", tone = "error", source = "DlaFlow"),
                disabled,
            ),
        )
    }

    @Test
    fun `technical message synchronization notifications never reach Android`() {
        val preferences = MobileNotificationPreferences.defaults()

        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Wiadomości Gmail: zakończono", "OPEN_MESSAGES", tone = "success", source = "Gmail"),
                preferences,
            ),
        )
        assertFalse(
            shouldShowNativePanelNotification(
                testNotification("Wiadomości Gmail: zakończono", "OPEN_MESSAGES", tone = "info", source = "Gmail"),
                preferences,
            ),
        )
    }

    @Test
    fun `real customer message and reply notifications reach Android`() {
        val preferences = MobileNotificationPreferences.defaults()

        assertTrue(shouldShowNativePanelNotification(testNotification("Nowa wiadomość od klienta", "OPEN_MESSAGES", source = "push"), preferences))
        assertTrue(shouldShowNativePanelNotification(testNotification("Klient odpowiedział na wiadomość", "OPEN_MESSAGES", source = "push"), preferences))
    }

    @Test
    fun `disabled photo tasks suppress direct task alerts`() {
        val preferences = MobileNotificationPreferences.defaults()
            .withEnabled(MobileNotificationCategory.PHOTO_TASKS, false)

        assertFalse(shouldShowNativePhotoTaskNotification(preferences))
        assertTrue(shouldShowNativePhotoTaskNotification(MobileNotificationPreferences.defaults()))
    }

    @Test
    fun `every category switch controls its explicit push event`() {
        val defaults = MobileNotificationPreferences.defaults()

        MobileNotificationCategory.entries.forEach { category ->
            val notification = notificationForCategory(category, source = "push")
            assertTrue("${category.name} should be enabled by default", shouldShowNativePanelNotification(notification, defaults))
            assertFalse(
                "${category.name} should be suppressed when disabled",
                shouldShowNativePanelNotification(
                    notification,
                    defaults.withEnabled(category, false),
                ),
            )
        }
    }

    @Test
    fun `panel history is never replayed except for a critical important alert`() {
        val defaults = MobileNotificationPreferences.defaults()

        MobileNotificationCategory.entries
            .filterNot { it == MobileNotificationCategory.IMPORTANT_PANEL }
            .forEach { category ->
                assertFalse(
                    "${category.name} history must stay in the in-app center",
                    shouldShowNativePanelNotification(notificationForCategory(category), defaults),
                )
            }

        assertTrue(
            shouldShowNativePanelNotification(
                testNotification(
                    title = "Awaria integracji",
                    actionType = "OPEN_LOGS_SUMMARY",
                    tone = "error",
                    source = "DlaFlow",
                ),
                defaults,
            ),
        )
        assertFalse(
            shouldShowNativePanelNotification(
                testNotification(
                    title = "Awaria integracji",
                    actionType = "OPEN_LOGS_SUMMARY",
                    tone = "error",
                    source = "DlaFlow",
                ),
                defaults.withEnabled(MobileNotificationCategory.IMPORTANT_PANEL, false),
            ),
        )
    }

    @Test
    fun `disabled explicit events are consumed while ordinary history is ignored`() {
        val disabledMessages = MobileNotificationPreferences.defaults()
            .withEnabled(MobileNotificationCategory.CUSTOMER_MESSAGES, false)
        val message = notificationForCategory(MobileNotificationCategory.CUSTOMER_MESSAGES, source = "push")

        assertEquals(
            PanelNotificationDeliveryDecision.SUPPRESS,
            mobileNotificationDeliveryDecision(
                message,
                disabledMessages,
                MobileNotificationDeliveryOrigin.FCM,
            ),
        )
        assertEquals(
            PanelNotificationDeliveryDecision.IGNORE,
            mobileNotificationDeliveryDecision(
                message.copy(source = "Gmail"),
                MobileNotificationPreferences.defaults(),
                MobileNotificationDeliveryOrigin.PANEL_HISTORY,
            ),
        )
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

    private fun testNotification(
        title: String,
        actionType: String,
        tone: String = "info",
        description: String = "Opis",
        source: String = "DlaFlow",
    ) = MobileAssistantNotification(
        id = title,
        title = title,
        description = description,
        tone = tone,
        source = source,
        account = "Panel",
        occurredAt = "2026-08-19T08:00:00Z",
        readAt = null,
        mobileAction = MobileNotificationAction(actionType, "Otwórz"),
    )

    private fun notificationForCategory(
        category: MobileNotificationCategory,
        source: String = "DlaFlow",
    ): MobileAssistantNotification = when (category) {
        MobileNotificationCategory.NEW_ORDERS -> testNotification("Nowe zamówienie", "OPEN_ORDERS", source = source)
        MobileNotificationCategory.CUSTOMER_MESSAGES -> testNotification("Nowa wiadomość od klienta", "OPEN_MESSAGES", source = source)
        MobileNotificationCategory.ORDER_STATUS -> testNotification("Zmiana statusu zamówienia", "OPEN_ORDERS", tone = "attention", source = source)
        MobileNotificationCategory.SHIPMENT_STATUS -> testNotification("Zmiana statusu przesyłki", "OPEN_ORDERS", tone = "attention", source = source)
        MobileNotificationCategory.PHOTO_TASKS -> testNotification("Zadanie zdjęciowe", "OPEN_PHOTO_TASKS", source = source)
        MobileNotificationCategory.IMPORTANT_PANEL -> testNotification("Ważna sprawa", "OPEN_LOGS_SUMMARY", tone = "error", source = source)
    }
}
