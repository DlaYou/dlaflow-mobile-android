package pl.dlaflow.mobile.feature.dashboard

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardSourceBoundaryTest {
    @Test
    fun `shared kpi tile supports an accessible click action`() {
        val source = File("src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowComponents.kt").readText()

        assertTrue(source.contains("onClick: (() -> Unit)? = null"))
        assertTrue(source.contains("clickable(role = Role.Button, onClick = onClick)"))
    }

    @Test
    fun `dashboard and orders kpi grids forward all destinations`() {
        val dashboard = File("src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardScreen.kt").readText()
        val orders = File("src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt").readText()

        listOf("NEW_ORDERS", "TO_SHIP", "OVERDUE", "MESSAGES").forEach { destination ->
            assertTrue(dashboard.contains("MobileKpiDestination.$destination"))
            assertTrue(orders.contains("MobileKpiDestination.$destination"))
        }
    }
}
