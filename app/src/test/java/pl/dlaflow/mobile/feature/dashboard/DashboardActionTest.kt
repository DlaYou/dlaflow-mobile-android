package pl.dlaflow.mobile.feature.dashboard

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardActionTest {
    @Test
    fun `dashboard quick actions preserve current destinations`() {
        assertEquals(DashboardAction.ScanPackage, dashboardQuickAction(0))
        assertEquals(DashboardAction.OpenProductWork, dashboardQuickAction(1))
        assertEquals(DashboardAction.OpenStatistics, dashboardQuickAction(2))
        assertEquals(DashboardAction.OpenProducts, dashboardQuickAction(3))
    }

    @Test
    fun `dashboard visible fallback and relative time copy comes from resources`() {
        val source = File("src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardScreen.kt").readText()

        listOf(
            "dashboard_name_fallback_display",
            "dashboard_name_fallback_first",
            "dashboard_time_now",
            "dashboard_time_minutes_ago",
            "dashboard_time_hours_ago",
            "dashboard_time_days_ago",
        ).forEach { resourceName ->
            assertTrue(source.contains("R.string.$resourceName"))
        }

        assertFalse(source.contains("\"DlaFlow\""))
        assertFalse(source.contains("\"Maciek\""))
        assertFalse(source.contains("\"teraz\""))
        assertFalse(source.contains("min temu"))
        assertFalse(source.contains("minutes / 60}h"))
        assertFalse(source.contains("minutes / (24 * 60)}d"))
    }

}
