package pl.dlaflow.mobile.feature.settings

import pl.dlaflow.mobile.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMapperTest {
    @Test
    fun `content keeps canonical menu order and dynamic summaries`() {
        val content = buildSettingsContent(
            input().copy(
                canAutoOpenTasks = false,
                update = SettingsUpdateInfo("Wydanie", "0.5.0", 12_345L),
            ),
        )

        assertEquals(SettingsKind.entries, content.items.map(SettingsMenuItem::kind))
        assertEquals("Aktualizacja dostępna", content.items.first { it.kind == SettingsKind.APP }.subtitle)
        assertEquals("Przez powiadomienie", content.items.first { it.kind == SettingsKind.PREFERENCES }.subtitle)
        assertEquals("Włączone", content.items.first { it.kind == SettingsKind.CALLER_ID }.subtitle)
    }

    @Test
    fun `blank profile values use safe existing fallbacks`() {
        val content = buildSettingsContent(
            input().copy(displayName = "", userEmail = "operator@example.test", tenantName = "", deviceName = ""),
        )
        val account = content.detail(SettingsKind.ACCOUNT)

        assertEquals(
            listOf(
                SettingsDetailRow("Operator", "operator"),
                SettingsDetailRow("E-mail", "operator@example.test"),
                SettingsDetailRow("Firma", "DlaFlow"),
                SettingsDetailRow("Telefon", "Telefon DlaFlow"),
            ),
            account.rows,
        )
    }

    @Test
    fun `all settings kinds produce business detail without platform objects`() {
        val content = buildSettingsContent(input())

        SettingsKind.entries.forEach { kind ->
            val detail = content.detail(kind)
            assertEquals(kind, detail.kind)
            assertTrue(detail.title.isNotBlank())
            assertTrue(detail.description.isNotBlank())
            assertTrue(
                detail.rows.isNotEmpty() ||
                    detail.primaryActionLabel != null ||
                    detail.secondaryActionLabel != null ||
                    detail.dangerActionLabel != null,
            )
        }
    }

    @Test
    fun `permission and update states remain explicit and read only`() {
        val content = buildSettingsContent(
            input().copy(
                notificationAllowed = false,
                canAutoOpenTasks = false,
                update = SettingsUpdateInfo("Nowa wersja", "0.5.0", 5_242_880L),
                updateChecking = true,
                updateDownloading = true,
                updateDownloadProgress = 47,
                updateError = "Bezpieczny błąd",
            ),
        )

        assertEquals("Wymagają zgody Androida", content.detail(SettingsKind.NOTIFICATIONS).rows.first().value)
        assertEquals("Włącz auto-otwieranie", content.detail(SettingsKind.PREFERENCES).primaryActionLabel)
        assertTrue(content.updateChecking)
        assertTrue(content.updateDownloading)
        assertEquals(47, content.updateDownloadProgress)
        assertEquals("Bezpieczny błąd", content.updateError)
        assertFalse(content.notificationAllowed)
    }

    @Test
    fun `notification presentation remains a narrow copy of host preferences`() {
        val preferences = listOf(
            SettingsNotificationPreference(
                key = "new_orders",
                label = "Nowe zamowienia",
                description = "Gdy wpada nowe zamowienie do obslugi.",
                enabled = false,
            ),
        )

        val content = buildSettingsContent(
            input().copy(
                notificationPreferenceSummary = "5 z 6 typow wlaczonych",
                notificationPreferences = preferences,
            ),
        )

        assertEquals("5 z 6 typow wlaczonych", content.notificationPreferenceSummary)
        assertEquals(preferences, content.notificationPreferences)
    }

    private fun input() = SettingsInput(
        displayName = "Maciek",
        userEmail = "maciek@example.test",
        tenantName = "DlaFlow",
        deviceName = "Telefon DlaFlow",
        phoneStatusMessage = "Telefon działa normalnie.",
        callerIdLabel = "Włączone",
        callerIdPreview = SettingsCallerIdPreview(
            displayName = "Klient testowy",
            phone = "+48123123123",
            primaryOrder = SettingsCallerIdOrder("1001", "Nowe"),
        ),
        callerIdAvailable = true,
        callerIdOperational = true,
        canAutoOpenTasks = true,
        notificationAllowed = true,
        appVersionName = "0.4.4",
        update = null,
        updateChecking = false,
        updateDownloading = false,
        updateDownloadProgress = 0,
        updateError = "",
        textResolver = testSettingsTextResolver,
    )
}

internal val testSettingsTextResolver = SettingsTextResolver { resourceId, arguments ->
    when (resourceId) {
        R.string.settings_operator_fallback -> "Operator"
        R.string.settings_tenant_fallback -> "DlaFlow"
        R.string.settings_device_fallback -> "Telefon DlaFlow"
        R.string.settings_caller_id_fallback -> "Do sprawdzenia"
        R.string.settings_phone_normal -> "Telefon działa normalnie."
        R.string.settings_label_operator -> "Operator"
        R.string.settings_label_email -> "E-mail"
        R.string.settings_label_company -> "Firma"
        R.string.settings_label_phone -> "Telefon"
        R.string.settings_value_update_available -> "Aktualizacja dostępna"
        R.string.settings_value_via_notification -> "Przez powiadomienie"
        R.string.settings_value_android_permission -> "Wymagają zgody Androida"
        R.string.settings_overlay_enable -> "Włącz auto-otwieranie"
        else -> if (arguments.isEmpty()) "tekst-$resourceId" else "tekst-$resourceId-${arguments.joinToString()}"
    }
}
