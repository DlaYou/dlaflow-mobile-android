package pl.dlaflow.mobile

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

    @Test
    fun `system splash has a bounded fallback release`() {
        val source = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()
        val onCreateSource = source.substringAfter("override fun onCreate").substringBefore("private fun startInitialContent")
        val onResumeSource = source.substringAfter("override fun onResume").substringBefore("override fun onActivityResult")

        assertTrue(source.contains("systemSplashFallbackDelayMs"))
        assertTrue(source.contains("dispatchHandler.postDelayed(::releaseSystemSplash, systemSplashFallbackDelayMs)"))
        assertTrue(source.contains("private fun releaseSystemSplash()"))
        assertTrue(onCreateSource.contains("showSessionTransitionShell()"))
        assertTrue(!onCreateSource.contains("sessionStore = MobileSessionStore(this)"))
        assertTrue(!onCreateSource.contains("startupHasSavedSession = sessionStore.readToken().isNotBlank()"))
        assertTrue(onCreateSource.indexOf("showSessionTransitionShell()") < onCreateSource.indexOf("screenView.doOnPreDraw"))
        assertTrue(onCreateSource.contains("screenView.doOnPreDraw"))
        assertTrue(onCreateSource.contains("dispatchHandler.post(::startInitialContent)"))
        assertTrue(source.contains("private fun startInitialContent()"))
        assertTrue(source.contains("startupHasSavedSession = sessionStore.readToken().isNotBlank()"))
        assertTrue(onResumeSource.contains("if (!::sessionStore.isInitialized)"))
        assertTrue(source.contains("screenView.addView(composeView, 0"))
    }
}
