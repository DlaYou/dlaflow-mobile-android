package pl.dlaflow.mobile.feature.notifications

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationsSourceBoundaryTest {
    @Test
    fun `notification center forwards row taps to the notification action`() {
        val screen = File("src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt").readText()
        val dashboard = File("src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardScreen.kt").readText()
        val host = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()

        assertTrue(screen.contains("onOpenNotification: (DashboardNotification) -> Unit"))
        assertTrue(screen.contains("onOpenNotification(notification)"))
        assertTrue(screen.contains("onClick = onClick"))
        assertTrue(dashboard.contains("onOpenNotification: (DashboardNotification) -> Unit"))
        assertTrue(dashboard.contains("onOpenNotification = onOpenNotification"))
        assertTrue(dashboard.contains("onClick = { onOpenNotification(notification) }"))
        assertTrue(host.contains("onOpenNotification = ::handleNotificationTap"))
    }

    @Test
    fun `notification tap is resolved by the shared destination resolver`() {
        val host = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()
        val resolverIndex = host.indexOf("notificationEffectFor(notification.actionType, notification.title, notification.description)")

        assertTrue(resolverIndex >= 0)
        assertTrue(host.contains("private fun handleNotificationTap"))
    }

    @Test
    fun `notification launch refreshes the selected destination after session startup`() {
        val host = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()

        assertTrue(host.contains("private fun ensureSelectedTabLoaded()"))
        assertTrue(host.contains("selectedTab == MobileAssistantTab.PRODUCTS -> ensureProductsLoaded()"))
        assertTrue(host.contains("selectedTab == MobileAssistantTab.MESSAGES -> ensureMessagesLoaded()"))
        assertTrue(host.contains("ensureSelectedTabLoaded()"))
    }
}
