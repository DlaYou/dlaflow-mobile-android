package pl.dlaflow.mobile

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductPhotoTaskActionTest {
    @Test
    fun `delegates dashboard ownership to the dashboard feature`() {
        val source = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()

        assertFalse(source.contains("private var assistantDashboard"))
        assertFalse(source.contains("private fun refreshAssistantDashboard("))
        assertFalse(source.contains("MobileAssistantQuickAction"))
        assertTrue(source.contains("DashboardStateHolder"))
        assertTrue(source.contains("DashboardCoordinator"))
        assertTrue(source.contains("DashboardGateway"))
        assertTrue(source.contains("onUnauthorized = { error ->"))
        assertTrue(source.contains("confirmRevokedSession("))
        assertTrue(source.contains("onSessionValid ="))
    }

    @Test
    fun `uses first active task from refreshed task list`() {
        val decision = chooseProductPhotoTaskAction(
            activeTaskIds = listOf("task-list-first", "task-list-second"),
            dashboardActiveTaskId = "task-dashboard",
        )

        assertEquals("task-list-first", decision.focusedTaskId)
        assertFalse(decision.shouldRefreshTasks)
    }

    @Test
    fun `uses dashboard task when task list has not loaded yet`() {
        val decision = chooseProductPhotoTaskAction(
            activeTaskIds = emptyList(),
            dashboardActiveTaskId = "task-dashboard",
        )

        assertEquals("task-dashboard", decision.focusedTaskId)
        assertFalse(decision.shouldRefreshTasks)
    }

    @Test
    fun `asks for refresh when there is no product photo task`() {
        val decision = chooseProductPhotoTaskAction(
            activeTaskIds = emptyList(),
            dashboardActiveTaskId = null,
        )

        assertEquals(null, decision.focusedTaskId)
        assertTrue(decision.shouldRefreshTasks)
    }
}
