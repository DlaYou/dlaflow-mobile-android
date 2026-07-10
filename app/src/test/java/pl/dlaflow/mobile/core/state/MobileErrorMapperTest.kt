package pl.dlaflow.mobile.core.state

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.network.MobileApiException

class MobileErrorMapperTest {
    @Test
    fun `offline error maps to retryable business copy`() {
        val message = mobileErrorToUiMessage(UnknownHostException("private host"))

        assertEquals(R.string.mobile_error_offline_title, message.titleRes)
        assertEquals(R.string.mobile_error_offline_description, message.descriptionRes)
        assertTrue(message.retryable)
    }

    @Test
    fun `timeout maps to retryable timeout copy`() {
        val message = mobileErrorToUiMessage(SocketTimeoutException("raw timeout"))

        assertEquals(R.string.mobile_error_timeout_title, message.titleRes)
        assertTrue(message.retryable)
    }

    @Test
    fun `unauthorized response does not expose raw api message`() {
        val message = mobileErrorToUiMessage(
            MobileApiException(401, "AUTH_REQUIRED", "raw secret token"),
        )

        assertEquals(R.string.mobile_error_session_title, message.titleRes)
        assertEquals(R.string.mobile_error_session_description, message.descriptionRes)
        assertFalse(message.retryable)
    }

    @Test
    fun `forbidden response maps to no access`() {
        val message = mobileErrorToUiMessage(MobileApiException(403, "FORBIDDEN", "raw"))

        assertEquals(R.string.mobile_error_no_access_title, message.titleRes)
        assertFalse(message.retryable)
    }

    @Test
    fun `validation response maps to controlled non retryable copy`() {
        val message = mobileErrorToUiMessage(MobileApiException(422, "VALIDATION_ERROR", "raw field payload"))

        assertEquals(R.string.mobile_error_validation_title, message.titleRes)
        assertEquals(R.string.mobile_error_validation_description, message.descriptionRes)
        assertFalse(message.retryable)
    }

    @Test
    fun `rate limit remains retryable`() {
        val message = mobileErrorToUiMessage(MobileApiException(429, "RATE_LIMITED", "raw"))

        assertEquals(R.string.mobile_error_rate_limit_title, message.titleRes)
        assertTrue(message.retryable)
    }

    @Test
    fun `server failure maps to controlled availability copy`() {
        val message = mobileErrorToUiMessage(MobileApiException(503, "UNAVAILABLE", "raw payload"))

        assertEquals(R.string.mobile_error_server_title, message.titleRes)
        assertTrue(message.retryable)
    }
}
