package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class MobileSessionUiStateTest {
    @Test
    fun `saved session verification states stay on recovery screen`() {
        assertEquals(MobileSessionRoute.RECOVERY, mobileSessionRoute(MobileSessionUiState.CHECKING))
        assertEquals(MobileSessionRoute.RECOVERY, mobileSessionRoute(MobileSessionUiState.OFFLINE))
    }

    @Test
    fun `only a missing or revoked session opens pairing`() {
        assertEquals(MobileSessionRoute.PAIRING, mobileSessionRoute(MobileSessionUiState.PAIRING))
        assertEquals(MobileSessionRoute.ASSISTANT, mobileSessionRoute(MobileSessionUiState.CONNECTED))
    }

    @Test
    fun `verification retry delay grows and is capped`() {
        assertEquals(2_000L, mobileSessionRetryDelayMs(0))
        assertEquals(4_000L, mobileSessionRetryDelayMs(1))
        assertEquals(30_000L, mobileSessionRetryDelayMs(4))
        assertEquals(30_000L, mobileSessionRetryDelayMs(99))
    }
}
