package pl.dlaflow.mobile.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class DlaFlowDesignTokensTest {
    @Test
    fun `dark colors preserve current mobile rendering`() {
        val colors = dlaFlowColors(dark = true)

        assertEquals(Color(0xFF0F131D), colors.appBg)
        assertEquals(Color(0xFF171C27), colors.surface)
        assertEquals(Color(0xFF9B83FF), colors.primary)
        assertEquals(Color(0xFFF8FAFC), colors.textStrong)
        assertEquals(Color(0xFFF1EEFF), colors.pairingPreviewBackground)
        assertEquals(Color(0xFF151A2E), colors.pairingPreviewDark)
        assertEquals(Color(0xFF3CF2B1), colors.heroPositive)
        assertEquals(Color(0xFFFFB4B4), colors.heroNegative)
    }

    @Test
    fun `light colors preserve current compose background difference`() {
        val colors = dlaFlowColors(dark = false)

        assertEquals(Color.White, colors.appBg)
        assertEquals(Color(0xFFF8F9FC), colors.material.background)
        assertEquals(Color.White, colors.surface)
        assertEquals(Color(0xFF7B5CF6), colors.primary)
        assertEquals(Color(0xFF0F172A), colors.textStrong)
        assertEquals(Color.White, colors.pairingPreviewBackground)
    }

    @Test
    fun `shared dimensions freeze current component geometry`() {
        assertEquals(48.dp, DlaFlowDimensions.minimumTouchTarget)
        assertEquals(8.dp, DlaFlowDimensions.controlRadius)
        assertEquals(0.dp, DlaFlowDimensions.flatElevation)
    }
}
