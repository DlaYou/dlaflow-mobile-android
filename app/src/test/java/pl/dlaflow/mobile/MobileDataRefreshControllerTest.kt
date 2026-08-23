package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.app.navigation.MobileAssistantTab

class MobileDataRefreshControllerTest {
    @Test
    fun `foreground refresh runs immediately and then every interval for visible data`() {
        val scheduler = RefreshSchedulerHarness(selectedTab = MobileAssistantTab.ORDERS)

        scheduler.controller.start()

        assertEquals(1, scheduler.dashboardRefreshes)
        assertEquals(1, scheduler.ordersRefreshes)
        assertEquals(1, scheduler.notificationRefreshes)
        assertEquals(60_000L, scheduler.scheduledDelayMs)

        scheduler.runScheduledRefresh()

        assertEquals(2, scheduler.dashboardRefreshes)
        assertEquals(2, scheduler.ordersRefreshes)
        assertEquals(2, scheduler.notificationRefreshes)
        assertEquals(60_000L, scheduler.scheduledDelayMs)
    }

    @Test
    fun `tab selection refreshes only the selected operational area`() {
        val scheduler = RefreshSchedulerHarness(selectedTab = MobileAssistantTab.PRODUCTS)

        scheduler.controller.refreshAfterTabSelection(MobileAssistantTab.DASHBOARD)
        scheduler.controller.refreshAfterTabSelection(MobileAssistantTab.ORDERS)
        scheduler.controller.refreshAfterTabSelection(MobileAssistantTab.PRODUCTS)

        assertEquals(1, scheduler.dashboardRefreshes)
        assertEquals(1, scheduler.ordersRefreshes)
        assertEquals(0, scheduler.notificationRefreshes)
    }

    @Test
    fun `stopping in background cancels polling and stale tick cannot refresh`() {
        val scheduler = RefreshSchedulerHarness(selectedTab = MobileAssistantTab.ORDERS)
        scheduler.controller.start()
        val staleTick = scheduler.scheduledRunnable

        scheduler.controller.stop()

        assertSame(staleTick, scheduler.removedRunnable)
        assertFalse(scheduler.hasScheduledRefresh)
        staleTick!!.run()
        assertEquals(1, scheduler.dashboardRefreshes)
        assertEquals(1, scheduler.ordersRefreshes)
        assertEquals(1, scheduler.notificationRefreshes)
        assertFalse(scheduler.hasScheduledRefresh)
    }

    @Test
    fun `repeated start does not duplicate immediate refresh or timer`() {
        val scheduler = RefreshSchedulerHarness(selectedTab = MobileAssistantTab.DASHBOARD)

        scheduler.controller.start()
        val firstTick = scheduler.scheduledRunnable
        scheduler.controller.start()

        assertEquals(1, scheduler.dashboardRefreshes)
        assertEquals(0, scheduler.ordersRefreshes)
        assertEquals(1, scheduler.notificationRefreshes)
        assertSame(firstTick, scheduler.scheduledRunnable)
        assertTrue(scheduler.hasScheduledRefresh)
    }
}

private class RefreshSchedulerHarness(
    var selectedTab: MobileAssistantTab,
) {
    var dashboardRefreshes = 0
    var ordersRefreshes = 0
    var notificationRefreshes = 0
    var scheduledRunnable: Runnable? = null
    var scheduledDelayMs: Long? = null
    var removedRunnable: Runnable? = null

    val hasScheduledRefresh: Boolean
        get() = scheduledRunnable != null

    val controller = MobileDataRefreshController(
        postDelayed = { runnable, delayMs ->
            scheduledRunnable = runnable
            scheduledDelayMs = delayMs
        },
        removeCallbacks = { runnable ->
            removedRunnable = runnable
            if (scheduledRunnable === runnable) scheduledRunnable = null
        },
        refreshDashboard = { dashboardRefreshes += 1 },
        refreshOrders = { ordersRefreshes += 1 },
        refreshNotifications = { notificationRefreshes += 1 },
        selectedTab = { selectedTab },
        intervalMs = 60_000L,
    )

    fun runScheduledRefresh() {
        val runnable = requireNotNull(scheduledRunnable)
        scheduledRunnable = null
        scheduledDelayMs = null
        runnable.run()
    }
}
