package pl.dlaflow.mobile.feature.products

import java.net.UnknownHostException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException

class PhotoTasksFailureTest {
    @Test
    fun `auth access offline and generic errors stay typed and sanitized`() {
        val unauthorized = mapPhotoTasksFailure(MobileApiException(401, "AUTH_REQUIRED", "private-auth"))
        val noAccess = mapPhotoTasksFailure(MobileApiException(403, "FORBIDDEN", "private-access"))
        val offline = mapPhotoTasksFailure(UnknownHostException("private-host"))
        val retryable = mapPhotoTasksFailure(IllegalStateException("private-payload"))
        val invalid = mapPhotoTasksFailure(InvalidPhotoTaskPayloadException())

        assertTrue(unauthorized is PhotoTasksFailure.Unauthorized)
        assertFalse((unauthorized as PhotoTasksFailure.Unauthorized).message.retryable)
        assertTrue(noAccess is PhotoTasksFailure.NoAccess)
        assertTrue(offline is PhotoTasksFailure.Offline)
        assertTrue(retryable is PhotoTasksFailure.Retryable)
        assertTrue(invalid is PhotoTasksFailure.InvalidPayload)
        assertFalse((invalid as PhotoTasksFailure.InvalidPayload).message.retryable)
    }
}
