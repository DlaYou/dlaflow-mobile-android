package pl.dlaflow.mobile.feature.dashboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardLayoutPolicyTest {
    @Test
    fun `standard width and font preserve dashboard parity dimensions`() {
        val policy = dashboardLayoutPolicy(widthDp = 412, fontScale = 1.0f)

        assertEquals(104, policy.revenueCardHeightDp)
        assertEquals(98, policy.kpiTileHeightDp)
        assertEquals(84, policy.quickActionHeightDp)
        assertEquals(1, policy.quickActionLabelMaxLines)
        assertEquals(1, policy.quickActionSubtitleMaxLines)
        assertFalse(policy.stackRevenueComparison)
        assertEquals(4, policy.kpiColumns)
    }

    @Test
    fun `narrow dashboard with larger font grows owned cards without shrinking text`() {
        val policy = dashboardLayoutPolicy(widthDp = 360, fontScale = 1.3f)

        assertTrue(policy.revenueCardHeightDp > 104)
        assertTrue(policy.kpiTileHeightDp > 98)
        assertEquals(116, policy.quickActionHeightDp)
        assertEquals(2, policy.quickActionLabelMaxLines)
        assertEquals(2, policy.quickActionSubtitleMaxLines)
        assertTrue(policy.stackRevenueComparison)
        assertEquals(2, policy.kpiColumns)
    }
}
