package pl.dlaflow.mobile.feature.notifications

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException

class NotificationsFailureTest {
    @Test
    fun `maps 401 403 offline and retryable failures into closed categories`() {
        assertTrue(mapNotificationsFailure(apiError(401)) is NotificationsFailure.Unauthorized)
        assertTrue(mapNotificationsFailure(apiError(403)) is NotificationsFailure.NoAccess)
        assertTrue(mapNotificationsFailure(UnknownHostException()) is NotificationsFailure.Offline)
        assertTrue(mapNotificationsFailure(ConnectException()) is NotificationsFailure.Offline)
        assertTrue(mapNotificationsFailure(SocketTimeoutException()) is NotificationsFailure.Retryable)
        assertTrue(mapNotificationsFailure(IllegalStateException("raw secret")) is NotificationsFailure.Retryable)
    }

    private fun apiError(status: Int) = MobileApiException(
        statusCode = status,
        code = "TEST",
        message = "raw transport detail",
    )
}
