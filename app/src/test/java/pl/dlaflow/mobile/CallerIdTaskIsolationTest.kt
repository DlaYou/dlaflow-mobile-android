package pl.dlaflow.mobile

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallerIdTaskIsolationTest {
    @Test
    fun `caller id activity is isolated from the main app task`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val activityBlock = Regex("""<activity\s+android:name="\.CallerIdActivity"[\s\S]*?(?:/>|</activity>)""")
            .find(manifest)
            ?.value
            .orEmpty()

        assertTrue(activityBlock.contains("""android:excludeFromRecents="true""""))
        assertTrue(activityBlock.contains("""android:noHistory="true""""))
        assertTrue(activityBlock.contains("""android:finishOnTaskLaunch="true""""))
        assertTrue(activityBlock.contains("""android:taskAffinity="${'$'}{applicationId}.callerid""""))
    }

    @Test
    fun `caller id launch flags do not bring the main application task forward`() {
        val callerIdActivity = File("src/main/java/pl/dlaflow/mobile/CallerIdActivity.kt").readText()
        val createIntentBlock = Regex("""fun createIntent[\s\S]*?\.addFlags\(([\s\S]*?)\)\s*\n\s*}""")
            .find(callerIdActivity)
            ?.value
            .orEmpty()

        assertTrue(createIntentBlock.contains("Intent.FLAG_ACTIVITY_NEW_TASK"))
        assertTrue(createIntentBlock.contains("Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS"))
        assertTrue(createIntentBlock.contains("Intent.FLAG_ACTIVITY_NO_ANIMATION"))
        assertFalse(createIntentBlock.contains("Intent.FLAG_ACTIVITY_CLEAR_TOP"))
    }
}
