package pl.dlaflow.mobile.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsIntentLaunchPolicyTest {
    @Test
    fun `skips unresolved candidate and launches fallback`() {
        val launched = mutableListOf<String>()

        val result = launchFirstResolvedSettingsTarget(
            candidates = listOf("primary", "fallback"),
            canResolve = { it == "fallback" },
            launch = { launched += it },
        )

        assertTrue(result.launched)
        assertEquals(1, result.candidateIndex)
        assertEquals(listOf("fallback"), launched)
    }

    @Test
    fun `recoverable launch failure continues to fallback`() {
        val attempted = mutableListOf<String>()

        val result = launchFirstResolvedSettingsTarget(
            candidates = listOf("primary", "fallback"),
            canResolve = { true },
            launch = {
                attempted += it
                if (it == "primary") throw IllegalStateException("activity disappeared")
            },
        )

        assertTrue(result.launched)
        assertEquals(1, result.candidateIndex)
        assertEquals(listOf("primary", "fallback"), attempted)
    }

    @Test
    fun `successful primary stops chain`() {
        val attempted = mutableListOf<String>()

        val result = launchFirstResolvedSettingsTarget(
            candidates = listOf("primary", "fallback"),
            canResolve = { true },
            launch = { attempted += it },
        )

        assertTrue(result.launched)
        assertEquals(0, result.candidateIndex)
        assertEquals(listOf("primary"), attempted)
    }

    @Test
    fun `returns false when no candidate can be launched`() {
        val result = launchFirstResolvedSettingsTarget(
            candidates = listOf("primary", "fallback"),
            canResolve = { false },
            launch = { error("must not launch") },
        )

        assertFalse(result.launched)
        assertEquals(null, result.candidateIndex)
    }
}
