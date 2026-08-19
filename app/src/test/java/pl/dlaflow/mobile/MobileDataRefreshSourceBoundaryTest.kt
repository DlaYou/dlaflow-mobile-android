package pl.dlaflow.mobile

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileDataRefreshSourceBoundaryTest {
    @Test
    fun `main activity owns foreground refresh lifecycle`() {
        val source = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()
        val onResume = source.substringAfter("override fun onResume()").substringBefore("override fun onActivityResult")
        val onPause = source.substringAfter("override fun onPause()").substringBefore("override fun onActivityResult")

        assertTrue(onResume.contains("dataRefreshController.start()"))
        assertTrue(onPause.contains("dataRefreshController.stop()"))
    }

    @Test
    fun `tab changes request a refresh through the shared controller`() {
        val source = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()

        assertTrue(source.contains("dataRefreshController.refreshAfterTabSelection(it)"))
        assertTrue(source.contains("dataRefreshIntervalMs"))
        assertTrue(source.contains("ensureOrdersLoaded(showFeedback = false)"))
    }
}
