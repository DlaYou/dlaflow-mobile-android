package pl.dlaflow.mobile.feature.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsHostIdentityTest {
    @Test
    fun `caller id completion requires request session and normalized phone identity`() {
        val request = SettingsCallerIdLookupRequest(3L, 9L, "+48 123")

        assertTrue(request.accepts(3L, 9L, " +48 123 "))
        assertFalse(request.accepts(4L, 9L, "+48 123"))
        assertFalse(request.accepts(3L, 10L, "+48 123"))
        assertFalse(request.accepts(3L, 9L, "+48 999"))
    }

    @Test
    fun `update completion requires operation lifecycle and session identity`() {
        val operation = SettingsUpdateOperation(5L, 12L, 21L)

        assertTrue(operation.accepts(5L, 12L, 21L))
        assertFalse(operation.accepts(6L, 12L, 21L))
        assertFalse(operation.accepts(5L, 13L, 21L))
        assertFalse(operation.accepts(5L, 12L, 22L))
    }

    @Test
    fun `apk signer validation fails closed for empty or disjoint signer sets`() {
        assertFalse(settingsSignerSetsMatch(emptySet(), setOf("archive")))
        assertFalse(settingsSignerSetsMatch(setOf("current"), emptySet()))
        assertFalse(settingsSignerSetsMatch(setOf("current"), setOf("archive")))
        assertTrue(settingsSignerSetsMatch(setOf("shared", "old"), setOf("shared")))
    }
}
