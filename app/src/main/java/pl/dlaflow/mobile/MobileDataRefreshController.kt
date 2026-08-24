package pl.dlaflow.mobile

import pl.dlaflow.mobile.app.navigation.MobileAssistantTab

/** Keeps foreground refreshes alive without allowing a background timer to keep running. */
internal class MobileDataRefreshController(
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
    private val refreshDashboard: () -> Unit,
    private val refreshOrders: () -> Unit,
    private val refreshNotifications: () -> Unit,
    private val refreshMessages: () -> Unit = {},
    private val selectedTab: () -> MobileAssistantTab,
    private val intervalMs: Long,
) {
    private var started = false
    private val tick = Runnable {
        if (!started) return@Runnable
        refreshVisibleData()
        scheduleNext()
    }

    fun start() {
        if (started) return
        started = true
        refreshVisibleData()
        scheduleNext()
    }

    fun stop() {
        if (!started) return
        started = false
        removeCallbacks(tick)
    }

    fun refreshAfterTabSelection(tab: MobileAssistantTab) {
        when (tab) {
            MobileAssistantTab.DASHBOARD -> refreshDashboard()
            MobileAssistantTab.ORDERS -> refreshOrders()
            MobileAssistantTab.MESSAGES -> refreshMessages()
            else -> Unit
        }
    }

    private fun refreshVisibleData() {
        refreshDashboard()
        refreshNotifications()
        when (selectedTab()) {
            MobileAssistantTab.ORDERS -> refreshOrders()
            MobileAssistantTab.MESSAGES -> refreshMessages()
            else -> Unit
        }
    }

    private fun scheduleNext() {
        postDelayed(tick, intervalMs)
    }
}
