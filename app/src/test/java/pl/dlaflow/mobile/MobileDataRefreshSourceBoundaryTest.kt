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
    fun `tab changes keep cached data and leave refresh to explicit actions`() {
        val source = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()

        assertTrue(!source.contains("dataRefreshController.refreshAfterTabSelection(it)"))
        assertTrue(source.contains("dataRefreshIntervalMs"))
        assertTrue(source.contains("ensureOrdersLoaded(showFeedback = false, refreshExisting = false)"))
    }

    @Test
    fun `pull to refresh is owned by the shared scroll container`() {
        val screen = File("src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt").readText()
        val activity = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()

        assertTrue(screen.contains("PullToRefreshBox("))
        assertTrue(screen.contains("onRefresh = onRefreshCurrentTab"))
        assertTrue(screen.contains("val contentScrollState = rememberScrollState()"))
        assertTrue(screen.contains(".verticalScroll(contentScrollState)"))
        assertTrue(screen.contains("snapshotFlow { contentScrollState.value to contentScrollState.isScrollInProgress }"))
        assertTrue(screen.contains("val latestMessagesState = rememberUpdatedState(messagesState)"))
        assertTrue(screen.contains("latestMessagesState.value"))
        assertTrue(screen.contains("contentScrollState.isScrollInProgress"))
        assertTrue(activity.contains("onRefreshCurrentTab = ::refreshCurrentTab"))
        assertTrue(activity.contains("private fun refreshCurrentTab()"))
    }

    @Test
    fun `refresh hides retained lists behind shared skeleton surfaces`() {
        val screen = File("src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt")
            .readText()
            .replace("\r\n", "\n")

        assertTrue(screen.contains("mobileProductsLoading -> ProductListSkeleton(colors)"))
        assertTrue(screen.contains("messagesState.isRefreshing"))
        assertTrue(screen.contains("if (loading) {\n            NotificationPreviewSkeleton(colors)"))
        assertTrue(screen.contains("NotificationPreviewSkeleton(colors)"))
    }
}
