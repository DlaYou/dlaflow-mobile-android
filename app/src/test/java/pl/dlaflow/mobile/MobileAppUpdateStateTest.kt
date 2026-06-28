package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileAppUpdateStateTest {
    @Test
    fun `optional update can be deferred three times`() {
        val update = mobileUpdate(required = false, latestVersionCode = 5)

        assertFalse(mobileAppUpdateIsBlocking(update, MobileAppUpdateDismissalState()))
        assertEquals(3, mobileAppUpdateDismissalsRemaining(update, MobileAppUpdateDismissalState()))
        assertEquals(2, mobileAppUpdateDismissalsRemaining(update, MobileAppUpdateDismissalState(versionCode = 5, count = 1)))
        assertEquals(1, mobileAppUpdateDismissalsRemaining(update, MobileAppUpdateDismissalState(versionCode = 5, count = 2)))
        assertEquals(0, mobileAppUpdateDismissalsRemaining(update, MobileAppUpdateDismissalState(versionCode = 5, count = 3)))
        assertTrue(mobileAppUpdateIsBlocking(update, MobileAppUpdateDismissalState(versionCode = 5, count = 3)))
    }

    @Test
    fun `required update blocks immediately`() {
        val update = mobileUpdate(required = true, latestVersionCode = 5)

        assertTrue(mobileAppUpdateIsBlocking(update, MobileAppUpdateDismissalState()))
        assertEquals(0, mobileAppUpdateDismissalsRemaining(update, MobileAppUpdateDismissalState()))
    }

    @Test
    fun `new update version resets optional dismissals`() {
        val update = mobileUpdate(required = false, latestVersionCode = 6)
        val previousDismissals = MobileAppUpdateDismissalState(versionCode = 5, count = 3)

        assertFalse(mobileAppUpdateIsBlocking(update, previousDismissals))
        assertEquals(3, mobileAppUpdateDismissalsRemaining(update, previousDismissals))
    }

    @Test
    fun `next dismissal is counted per latest version and capped`() {
        val update = mobileUpdate(required = false, latestVersionCode = 5)

        assertEquals(
            MobileAppUpdateDismissalState(versionCode = 5, count = 1),
            nextMobileAppUpdateDismissalState(update, MobileAppUpdateDismissalState()),
        )
        assertEquals(
            MobileAppUpdateDismissalState(versionCode = 5, count = 3),
            nextMobileAppUpdateDismissalState(update, MobileAppUpdateDismissalState(versionCode = 5, count = 3)),
        )
    }

    private fun mobileUpdate(required: Boolean, latestVersionCode: Int): MobileAppUpdate {
        return MobileAppUpdate(
            currentVersionCode = 3,
            currentVersionName = "0.2.0",
            downloadUrl = "https://panel.example.test/api/m/a/m2_token",
            expiresAt = "2026-06-27T12:00:00.000Z",
            latestVersionCode = latestVersionCode,
            latestVersionName = "0.3.0",
            minSupportedVersionCode = 1,
            releaseNotes = listOf("Nowy pulpit", "Szybsze produkty"),
            releaseTitle = "Nowa wersja DlaFlow",
            required = required,
            sha256 = "a".repeat(64),
            sizeBytes = 4_000_000,
            status = if (required) MobileAppUpdateStatus.REQUIRED_UPDATE else MobileAppUpdateStatus.OPTIONAL_UPDATE,
            updatePriority = "normal",
        )
    }
}
