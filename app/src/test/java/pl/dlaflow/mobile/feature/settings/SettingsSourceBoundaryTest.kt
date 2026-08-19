package pl.dlaflow.mobile.feature.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSourceBoundaryTest {
    @Test
    fun `settings feature owns settings ui and host monolith has no legacy more settings system`() {
        val hostScreen = File("src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt").readText()
        val featureScreen = File("src/main/java/pl/dlaflow/mobile/feature/settings/SettingsScreen.kt").readText()

        assertFalse(hostScreen.contains("MobileMoreSettings"))
        assertFalse(hostScreen.contains("MoreTab("))
        assertTrue(hostScreen.contains("SettingsFeatureScreen("))
        assertTrue(featureScreen.contains("testTag(\"settings_feature_root\")"))
    }

    @Test
    fun `clearing mobile session preserves device update dismissal counters`() {
        val store = File("src/main/java/pl/dlaflow/mobile/session_store.kt").readText()

        assertTrue(store.contains("preserveUpdateDismissalState(updateDismissalState)"))
        assertTrue(store.contains("app_update_dismissed_version_code"))
        assertTrue(store.contains("app_update_dismiss_count"))
    }

    @Test
    fun `capabilities refresh on resume and host uses tested settings launch policy`() {
        val host = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()
        val onResume = host.substringAfter("override fun onResume()").substringBefore("override fun onActivityResult")

        assertTrue(onResume.contains("render()"))
        assertTrue(host.contains("launchFirstResolvedSettingsTarget("))
    }

    @Test
    fun `settings host never renders throwable message`() {
        val host = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()
        val screen = File("src/main/java/pl/dlaflow/mobile/feature/settings/SettingsScreen.kt").readText()

        assertFalse(host.contains("error.message"))
        assertFalse(screen.contains("Throwable"))
    }

    @Test
    fun `notification preference changes do not rebuild the host compose root`() {
        val host = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()
        val preferenceBranch = host
            .substringAfter("is SettingsEffect.NotificationPreferenceChanged")
            .substringBefore("is SettingsEffect.Disconnect")

        assertFalse(preferenceBranch.contains("render()"))
    }

    @Test
    fun `host keeps one compose root after the first render`() {
        val host = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()
        val renderBody = host.substringAfter("private fun render() {").substringBefore("private fun showSessionTransitionShell()")

        assertTrue(renderBody.contains("if (::contentView.isInitialized)"))
        assertTrue(renderBody.contains("hostRenderVersion += 1"))
        assertFalse(renderBody.contains("screenView.removeView(contentView)"))
    }

    @Test
    fun `settings business copy is resource backed instead of hardcoded in mapper`() {
        val mapper = File("src/main/java/pl/dlaflow/mobile/feature/settings/SettingsMapper.kt").readText()
        val resources = File("src/main/res/values/strings.xml").readText()

        assertFalse(mapper.contains("\"Dane konta\""))
        assertFalse(mapper.contains("\"Powiadomienia informują"))
        assertTrue(resources.contains("name=\"settings_account_title\""))
        assertTrue(resources.contains("name=\"settings_notifications_description\""))
    }

    @Test
    fun `settings host operator copy is resource backed`() {
        val host = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()
        val resources = File("src/main/res/values/strings.xml").readText()
        val hostCopy = listOf(
            "Nie udało się otworzyć karty połączenia.",
            "Nie udało się pobrać aktualizacji. Spróbuj ponownie.",
            "Nie udało się otworzyć ustawień instalacji aplikacji.",
            "Otwórz ustawienia aplikacji i zezwól na instalowanie aktualizacji.",
            "Na tym telefonie nie ma aplikacji, która może otworzyć instalator.",
            "Nie udało się otworzyć ustawień powiadomień.",
            "Nie udało się otworzyć ustawień wyświetlania nad aplikacjami.",
            "Nie udało się otworzyć ustawień aplikacji.",
            "Otworzyłem ustawienia aplikacji. Wybierz odpowiednią zgodę.",
        )

        hostCopy.forEach { copy -> assertFalse("Hardcoded Settings copy: $copy", host.contains(copy)) }
        assertTrue(resources.contains("name=\"settings_host_caller_card_open_failed\""))
        assertTrue(resources.contains("name=\"settings_host_system_settings_fallback_opened\""))
    }
}
