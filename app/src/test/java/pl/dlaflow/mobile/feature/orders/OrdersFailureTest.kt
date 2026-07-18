package pl.dlaflow.mobile.feature.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.network.MobileApiException

class OrdersFailureTest {
    @Test
    fun `not found is a specific non retryable business error`() {
        val failure = mapOrdersFailure(
            MobileApiException(404, "ORDER_NOT_FOUND", "missing"),
        )

        assertTrue(failure is OrdersFailure.NotFound)
        val message = (failure as OrdersFailure.NotFound).message
        assertEquals(R.string.orders_not_found_title, message.titleRes)
        assertEquals(R.string.orders_not_found_description, message.descriptionRes)
        assertFalse(message.retryable)
    }
}
