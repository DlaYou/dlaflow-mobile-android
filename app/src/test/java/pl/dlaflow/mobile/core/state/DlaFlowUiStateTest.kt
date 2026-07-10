package pl.dlaflow.mobile.core.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DlaFlowUiStateTest {
    @Test
    fun `loading is covariant for every screen data type`() {
        val state: DlaFlowUiState<String> = DlaFlowUiState.Loading

        assertEquals(DlaFlowUiState.Loading, state)
    }

    @Test
    fun `content keeps immutable screen data`() {
        val state = DlaFlowUiState.Content(listOf("A", "B"))

        assertEquals(listOf("A", "B"), state.data)
    }

    @Test
    fun `offline can exist without pretending cached data is current`() {
        val state = DlaFlowUiState.Offline<String>()

        assertNull(state.lastContent)
    }

    @Test
    fun `error carries only controlled resource references`() {
        val message = DlaFlowUiMessage(titleRes = 1, descriptionRes = 2, retryable = true)
        val state = DlaFlowUiState.Error(message)

        assertEquals(message, state.message)
    }
}
