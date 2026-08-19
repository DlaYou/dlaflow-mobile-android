package pl.dlaflow.mobile.feature.products

import java.net.UnknownHostException
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException

class ProductsFailureTest {
    @Test
    fun `only 401 requires unauthorized confirmation`() {
        assertTrue(mapProductsFailure(MobileApiException(401, "AUTH_REQUIRED", "private")) is ProductsFailure.Unauthorized)
        assertTrue(mapProductsFailure(MobileApiException(500, "AUTH_REQUIRED", "private")) is ProductsFailure.Retryable)
    }

    @Test
    fun `access offline and invalid result use controlled typed failures`() {
        assertTrue(mapProductsFailure(MobileApiException(403, "FORBIDDEN", "private")) is ProductsFailure.NoAccess)
        assertTrue(mapProductsFailure(UnknownHostException("private host")) is ProductsFailure.Offline)
        assertTrue(mapProductsFailure(InvalidProductsResultException()) is ProductsFailure.InvalidResult)
    }
}
