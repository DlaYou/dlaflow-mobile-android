package pl.dlaflow.mobile

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileBackgroundSyncTest {
    @Test
    fun `background sync restarts only for boot and app update events`() {
        assertTrue(shouldScheduleBackgroundSyncForAction(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(shouldScheduleBackgroundSyncForAction(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertFalse(shouldScheduleBackgroundSyncForAction(Intent.ACTION_AIRPLANE_MODE_CHANGED))
        assertFalse(shouldScheduleBackgroundSyncForAction(null))
    }

    @Test
    fun `caller id notification text stays business focused`() {
        val order = MobileCallerIdOrder(
            amount = 129.0,
            currency = "zł",
            delivery = "InPost Paczkomat",
            lastEventAt = "Dziś 14:20",
            orderNumber = "#00000124",
            paymentStatus = "Opłacone",
            productSummary = "Bluza Classic",
            sourceCreatedAt = "2026-07-01T12:00:00Z",
            status = "Do wysyłki",
        )

        assertEquals("#00000124 · Do wysyłki · 129.0 zł", callerIdNotificationText(order))
        assertEquals("Dotknij, aby zobaczyć kartę DlaFlow.", callerIdNotificationText(null))
    }
}
