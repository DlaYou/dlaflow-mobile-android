package pl.dlaflow.mobile.feature.scanner

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException

class ScannerFailureTest {
    @Test
    fun `only 401 is an unauthorized confirmation failure`() {
        assertTrue(mapScannerFailure(MobileApiException(401, "AUTH_REQUIRED", "private")) is ScannerFailure.Unauthorized)
        assertTrue(mapScannerFailure(MobileApiException(500, "AUTH_REQUIRED", "private")) is ScannerFailure.Retryable)
    }

    @Test
    fun `403 is no access`() {
        assertTrue(mapScannerFailure(MobileApiException(403, "FORBIDDEN", "private")) is ScannerFailure.NoAccess)
    }

    @Test
    fun `connection failures are offline and other failures remain sanitized retryable messages`() {
        assertTrue(mapScannerFailure(UnknownHostException("private host")) is ScannerFailure.Offline)
        assertTrue(mapScannerFailure(ConnectException("private host")) is ScannerFailure.Offline)
        assertTrue(mapScannerFailure(SocketTimeoutException("private timeout")) is ScannerFailure.Retryable)

        val failure = mapScannerFailure(IllegalStateException("raw private failure")) as ScannerFailure.Retryable
        assertTrue(failure.message.retryable)
        assertFalse(failure.message.descriptionRes == 0)
    }

    @Test
    fun `invalid matched payload is a controlled non retryable feature error`() {
        val failure = mapScannerFailure(InvalidScannerResultException()) as ScannerFailure.InvalidResult

        assertFalse(failure.message.retryable)
        assertFalse(failure.message.descriptionRes == 0)
    }
}
