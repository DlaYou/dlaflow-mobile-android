package pl.dlaflow.mobile.feature.dashboard

import java.io.File
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
    }

    @Test
    fun `dashboard applies responsive policy to every owned fixed height area`() {
        val source = File("src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardScreen.kt").readText()

        assertTrue(source.contains("dashboardLayoutPolicy("))
        assertTrue(source.contains("layoutPolicy.revenueCardHeightDp.dp"))
        assertTrue(source.contains("layoutPolicy.kpiTileHeightDp.dp"))
        assertTrue(source.contains("layoutPolicy.quickActionHeightDp.dp"))
        assertTrue(source.contains("maxLines = layoutPolicy.quickActionLabelMaxLines"))
        assertTrue(source.contains("maxLines = layoutPolicy.quickActionSubtitleMaxLines"))
    }
}
