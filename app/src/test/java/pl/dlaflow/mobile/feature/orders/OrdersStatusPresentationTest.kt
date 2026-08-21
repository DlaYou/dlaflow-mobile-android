package pl.dlaflow.mobile.feature.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import pl.dlaflow.mobile.core.designsystem.dlaFlowColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusTone
import pl.dlaflow.mobile.core.designsystem.dlaFlowHexColor

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

    @Test
    fun `status color accepts exact six digit panel hex only`() {
        assertNotNull(dlaFlowHexColor("#112233"))
        assertNotNull(dlaFlowHexColor(" #AABBCC "))
        assertNull(dlaFlowHexColor("provider-purple"))
        assertNull(dlaFlowHexColor("#123"))
        assertNull(dlaFlowHexColor(""))
    }

    @Test
    fun `shipment milestone reuses fulfillment status color`() {
        val colors = dlaFlowColors(dark = false)
        val explicit = orderListDto().copy(statusColor = "#AABBCC").toOrdersListItem()
        val fallback = orderListDto().copy(statusColor = "", statusTone = "success").toOrdersListItem()

        assertEquals(dlaFlowHexColor("#AABBCC"), ordersFulfillmentStatusColor(colors, explicit))
        assertEquals(colors.success, ordersFulfillmentStatusColor(colors, fallback))
    }
}
