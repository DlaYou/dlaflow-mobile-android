package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class MobileMoreSettingsMenuTest {
    @Test
    fun `more tab settings menu keeps account center order`() {
        val items = buildMobileMoreSettingsItems(
            appVersionName = "0.3.3",
            callerIdLabel = "Włączone",
            canAutoOpenTasks = true,
            updateAvailable = false,
        )

        assertEquals(
            listOf(
                "Dane konta",
                "Bezpieczeństwo",
                "Powiadomienia",
                "Preferencje",
                "Integracje",
                "Zespół",
                "Aplikacja",
                "Caller ID",
            ),
            items.map { it.title },
        )
        assertEquals("Wersja 0.3.3", items.first { it.kind == MobileMoreSettingsKind.APP }.subtitle)
        assertEquals("Włączone", items.first { it.kind == MobileMoreSettingsKind.CALLER_ID }.subtitle)
    }

    @Test
    fun `more tab app row highlights available update`() {
        val items = buildMobileMoreSettingsItems(
            appVersionName = "0.3.3",
            callerIdLabel = "Do włączenia",
            canAutoOpenTasks = false,
            updateAvailable = true,
        )

        assertEquals("Aktualizacja dostępna", items.first { it.kind == MobileMoreSettingsKind.APP }.subtitle)
        assertEquals("Przez powiadomienie", items.first { it.kind == MobileMoreSettingsKind.PREFERENCES }.subtitle)
    }

    @Test
    fun `each more tab settings item opens a business detail screen`() {
        MobileMoreSettingsKind.entries.forEach { kind ->
            val detail = buildMobileMoreSettingsDetail(
                kind = kind,
                userName = "Maciek",
                userEmail = "maciek@example.test",
                tenantName = "DlaFlow",
                deviceName = "Telefon DlaFlow",
                appVersionName = "0.3.3",
                callerIdLabel = "Włączone",
                notificationAllowed = true,
                canAutoOpenTasks = true,
                updateAvailable = false,
            )

            assertEquals(kind, detail.kind)
            assert(detail.title.isNotBlank())
            assert(detail.description.isNotBlank())
            assert(detail.rows.isNotEmpty() || detail.primaryActionLabel != null || detail.secondaryActionLabel != null || detail.dangerActionLabel != null)
        }
    }

    @Test
    fun `app settings detail keeps one compact action model`() {
        val detail = buildMobileMoreSettingsDetail(
            kind = MobileMoreSettingsKind.APP,
            userName = "Maciek",
            userEmail = "maciek@example.test",
            tenantName = "DlaFlow",
            deviceName = "Telefon DlaFlow",
            appVersionName = "0.3.3",
            callerIdLabel = "Włączone",
            notificationAllowed = true,
            canAutoOpenTasks = true,
            updateAvailable = false,
        )

        assertEquals(
            listOf(
                "Wersja" to "0.3.3",
                "Kanał" to "Production APK z panelu",
                "Status" to "Masz aktualną wersję",
            ),
            detail.rows,
        )
        assertEquals("Sprawdź aktualizację", detail.primaryActionLabel)
        assertEquals("Ustawienia systemowe", detail.secondaryActionLabel)
    }

    @Test
    fun `caller id permission message explains saved contacts requirement`() {
        assertEquals(
            "Caller ID wymaga zgody na kontakty, żeby działać dla numerów zapisanych w telefonie.",
            callerIdMissingPermissionMessage(needsPhoneState = false, needsContacts = true),
        )
        assertEquals(
            "Caller ID wymaga zgody na telefon i kontakty, żeby pokazywać kartę także dla zapisanych klientów.",
            callerIdMissingPermissionMessage(needsPhoneState = true, needsContacts = true),
        )
    }
}
