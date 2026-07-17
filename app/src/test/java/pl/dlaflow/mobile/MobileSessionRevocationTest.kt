package pl.dlaflow.mobile

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException

class MobileSessionRevocationTest {
    @Test
    fun `unauthorized action does not clear session when mobile session is still valid`() {
        val shouldClear = shouldClearMobileSessionAfterUnauthorized(
            error = MobileApiException(401, "AUTH_REQUIRED", "Authentication is required."),
            verifyCurrentSession = { Unit },
        )

        assertFalse(shouldClear)
    }

    @Test
    fun `unauthorized action clears session when mobile session verification is also unauthorized`() {
        val shouldClear = shouldClearMobileSessionAfterUnauthorized(
            error = MobileApiException(401, "AUTH_REQUIRED", "Authentication is required."),
            verifyCurrentSession = { throw MobileApiException(401, "AUTH_REQUIRED", "Authentication is required.") },
        )

        assertTrue(shouldClear)
    }

    @Test
    fun `temporary verification failure keeps session to avoid false logout`() {
        val shouldClear = shouldClearMobileSessionAfterUnauthorized(
            error = MobileApiException(401, "AUTH_REQUIRED", "Authentication is required."),
            verifyCurrentSession = { throw MobileApiException(500, "SERVER_ERROR", "Temporary problem.") },
        )

        assertFalse(shouldClear)
    }

    @Test
    fun `session valid callback runs only after successful mobile session verification`() {
        var validCallbackCount = 0

        val validShouldClear = shouldClearMobileSessionAfterUnauthorized(
            error = MobileApiException(401, "AUTH_REQUIRED", "Authentication is required."),
            verifyCurrentSession = { Unit },
            onSessionValid = { validCallbackCount++ },
        )
        val unconfirmedShouldClear = shouldClearMobileSessionAfterUnauthorized(
            error = MobileApiException(401, "AUTH_REQUIRED", "Authentication is required."),
            verifyCurrentSession = { throw MobileApiException(500, "SERVER_ERROR", "Temporary problem.") },
            onSessionValid = { validCallbackCount++ },
        )

        assertFalse(validShouldClear)
        assertFalse(unconfirmedShouldClear)
        assertTrue(validCallbackCount == 1)
    }

    @Test
    fun `session clear requires same token used by failing request`() {
        assertTrue(isSameMobileSessionToken(currentToken = "token-new", requestToken = "token-new"))
        assertFalse(isSameMobileSessionToken(currentToken = "token-new", requestToken = "token-old"))
        assertFalse(isSameMobileSessionToken(currentToken = "", requestToken = "token-old"))
    }

    @Test
    fun `background services guard session clear by current token`() {
        listOf(
            "DlaFlowBackgroundSyncService.kt",
            "DlaFlowDispatchJobService.kt",
            "DlaFlowCallScreeningService.kt",
        ).forEach { fileName ->
            val source = File("src/main/java/pl/dlaflow/mobile/$fileName").readText()

            assertTrue("$fileName must not clear a newer session after an old request fails", source.contains("isSameMobileSessionToken("))
        }
    }

    @Test
    fun `main activity does not have local only session clear`() {
        val source = File("src/main/java/pl/dlaflow/mobile/MainActivity.kt").readText()

        assertFalse(source.contains("sessionStore.clear()"))
    }
}
