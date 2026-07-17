package pl.dlaflow.mobile.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardActionTest {
    @Test
    fun `dashboard quick actions preserve current destinations`() {
        assertEquals(DashboardAction.ScanPackage, dashboardQuickAction(0))
        assertEquals(DashboardAction.OpenProductWork, dashboardQuickAction(1))
        assertEquals(DashboardAction.OpenStatistics, dashboardQuickAction(2))
        assertEquals(DashboardAction.OpenProducts, dashboardQuickAction(3))
    }
}
