package pl.dlaflow.mobile.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.dlaflow.mobile.MobileAssistantCallerIdStatus
import pl.dlaflow.mobile.MobileAssistantDashboard
import pl.dlaflow.mobile.MobileAssistantKpis
import pl.dlaflow.mobile.MobileAssistantNotification
import pl.dlaflow.mobile.MobileAssistantPhotoTask
import pl.dlaflow.mobile.MobileAssistantTrendPoint
import pl.dlaflow.mobile.MobileNotificationAction
import pl.dlaflow.mobile.MobileNotificationSummary

class DashboardMapperTest {
    @Test
    fun `transport dashboard maps to presentation snapshot without secrets`() {
        val content = dashboardDto().toDashboardContent()

        assertEquals("Maciej", content.userName)
        assertEquals("DlaFlow", content.tenantName)
        assertEquals(350.0, content.todayRevenue, 0.0)
        assertEquals(12.5, content.revenueChangePercent, 0.0)
        assertEquals(DashboardKpis(2, 3, 1, 4), content.kpis)
        assertEquals(DashboardNotificationSummary(5, 1), content.notificationSummary)
        assertEquals(
            DashboardNotification(
                id = "notification-1",
                title = "Nowe zamówienie",
                description = "Zamówienie czeka na obsługę",
                tone = "info",
                source = "Allegro",
                account = "Konto główne",
                occurredAt = "2026-07-10T08:00:00Z",
                readAt = null,
                actionType = "OPEN_ORDER",
                actionLabel = "Otwórz",
            ),
            content.notifications.single(),
        )
        assertEquals(
            DashboardPhotoTask(
                id = "task-1",
                productName = "Produkt testowy",
                productSku = "SKU-1",
                productImage = "https://example.test/product.jpg",
                status = "OPEN",
                mediaCount = 2,
                maxPhotos = 6,
                expiresAt = "2026-07-11T08:00:00Z",
            ),
            content.activePhotoTask,
        )
        assertEquals(DashboardCallerIdStatus(true, "Aktywny"), content.callerIdStatus)
        assertEquals(
            listOf(
                DashboardTrendPoint("2026-07-09", 1, 100.0),
                DashboardTrendPoint("2026-07-10", 2, 350.0),
            ),
            content.trend,
        )
        assertEquals("2026-07-10T09:00:00Z", content.generatedAt)
        assertEquals("notification-1", content.notifications.single().id)
        assertEquals("task-1", content.activePhotoTask?.id)
        assertEquals(listOf(100.0, 350.0), content.trend.map { it.revenue })
    }

    @Test
    fun `mapper copies transport lists and nullable photo task`() {
        val notifications = mutableListOf(dashboardNotificationDto())
        val trend = mutableListOf(dashboardTrendPointDto())

        val content = dashboardDto(
            notifications = notifications,
            activePhotoTask = null,
            trend = trend,
        ).toDashboardContent()
        notifications.clear()
        trend.clear()

        assertEquals(1, content.notifications.size)
        assertEquals(1, content.trend.size)
        assertNull(content.activePhotoTask)
    }
}

internal fun dashboardDto(
    todayRevenue: Double = 350.0,
    notifications: List<MobileAssistantNotification> = listOf(dashboardNotificationDto()),
    activePhotoTask: MobileAssistantPhotoTask? = MobileAssistantPhotoTask(
        id = "task-1",
        productName = "Produkt testowy",
        productSku = "SKU-1",
        productImage = "https://example.test/product.jpg",
        status = "OPEN",
        mediaCount = 2,
        maxPhotos = 6,
        expiresAt = "2026-07-11T08:00:00Z",
    ),
    trend: List<MobileAssistantTrendPoint> = listOf(
        MobileAssistantTrendPoint("2026-07-09", 1, 100.0),
        MobileAssistantTrendPoint("2026-07-10", 2, 350.0),
    ),
) = MobileAssistantDashboard(
    userName = "Maciej",
    tenantName = "DlaFlow",
    todayRevenue = todayRevenue,
    revenueChangePercent = 12.5,
    kpis = MobileAssistantKpis(
        newOrders = 2,
        toShip = 3,
        overdueOrProblems = 1,
        messages = 4,
    ),
    notificationSummary = MobileNotificationSummary(
        unreadCount = 5,
        unreadAttentionCount = 1,
    ),
    notifications = notifications,
    activePhotoTask = activePhotoTask,
    callerIdStatus = MobileAssistantCallerIdStatus(
        enabled = true,
        label = "Aktywny",
    ),
    trend = trend,
    generatedAt = "2026-07-10T09:00:00Z",
)

private fun dashboardNotificationDto() = MobileAssistantNotification(
    id = "notification-1",
    title = "Nowe zamówienie",
    description = "Zamówienie czeka na obsługę",
    tone = "info",
    source = "Allegro",
    account = "Konto główne",
    occurredAt = "2026-07-10T08:00:00Z",
    readAt = null,
    mobileAction = MobileNotificationAction(
        type = "OPEN_ORDER",
        label = "Otwórz",
    ),
)

private fun dashboardTrendPointDto() = MobileAssistantTrendPoint(
    date = "2026-07-10",
    orders = 2,
    revenue = 350.0,
)
