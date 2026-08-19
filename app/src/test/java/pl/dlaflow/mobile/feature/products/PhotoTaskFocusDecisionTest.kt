package pl.dlaflow.mobile.feature.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoTaskFocusDecisionTest {
    @Test
    fun `uses first active task from refreshed task list`() {
        val decision = choosePhotoTaskFocus(
            activeTaskIds = listOf("task-list-first", "task-list-second"),
            dashboardActiveTaskId = "task-dashboard",
        )

        assertEquals("task-list-first", decision.focusedTaskId)
        assertFalse(decision.shouldRefreshTasks)
    }

    @Test
    fun `uses dashboard task when task list has not loaded yet`() {
        val decision = choosePhotoTaskFocus(
            activeTaskIds = emptyList(),
            dashboardActiveTaskId = "task-dashboard",
        )

        assertEquals("task-dashboard", decision.focusedTaskId)
        assertFalse(decision.shouldRefreshTasks)
    }

    @Test
    fun `asks for refresh when there is no product photo task`() {
        val decision = choosePhotoTaskFocus(
            activeTaskIds = emptyList(),
            dashboardActiveTaskId = null,
        )

        assertEquals(null, decision.focusedTaskId)
        assertTrue(decision.shouldRefreshTasks)
    }
}
