package pl.dlaflow.mobile.feature.orders

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusTone

class OrdersStatusPresentationTest {
    @Test
    fun `status value trims API text`() {
        assertEquals("Gotowe do odbioru", ordersStatusValue("  Gotowe do odbioru  ", "Do sprawdzenia"))
    }

    @Test
    fun `blank status value uses fallback`() {
        assertEquals("Do sprawdzenia", ordersStatusValue("   ", "Do sprawdzenia"))
    }

    @Test
    fun `status tone maps supported API values and rejects provider colors`() {
        assertEquals(DlaFlowStatusTone.INFO, ordersStatusTone("info"))
        assertEquals(DlaFlowStatusTone.SUCCESS, ordersStatusTone("SUCCESS"))
        assertEquals(DlaFlowStatusTone.WARNING, ordersStatusTone("warning"))
        assertEquals(DlaFlowStatusTone.DANGER, ordersStatusTone("danger"))
        assertEquals(DlaFlowStatusTone.NEUTRAL, ordersStatusTone("provider-purple"))
    }
}
