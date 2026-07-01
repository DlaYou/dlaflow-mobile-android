package pl.dlaflow.mobile

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionTransitionOverlayTest {
    @Test
    fun `session transition background uses one solid color`() {
        assertArrayEquals(
            intArrayOf(0xFF0F131D.toInt(), 0xFF0F131D.toInt()),
            sessionTransitionBackgroundColors(dark = true),
        )
        assertArrayEquals(
            intArrayOf(0xFFF8F9FC.toInt(), 0xFFF8F9FC.toInt()),
            sessionTransitionBackgroundColors(dark = false),
        )
    }

    @Test
    fun `session transition overlay is opaque`() {
        assertEquals(0xFF0F131D.toInt(), sessionTransitionOverlayColor(dark = true))
        assertEquals(0xFFF8F9FC.toInt(), sessionTransitionOverlayColor(dark = false))
    }
}
