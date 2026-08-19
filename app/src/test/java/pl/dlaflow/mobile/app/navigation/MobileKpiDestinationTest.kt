package pl.dlaflow.mobile.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.dlaflow.mobile.feature.orders.OrdersFilter

class MobileKpiDestinationTest {
    @Test
    fun `kpi destinations map to canonical orders filters`() {
        assertEquals(OrdersFilter.NEW, MobileKpiDestination.NEW_ORDERS.toOrdersFilter())
        assertEquals(OrdersFilter.TO_SHIP, MobileKpiDestination.TO_SHIP.toOrdersFilter())
        assertEquals(OrdersFilter.PROBLEMS, MobileKpiDestination.OVERDUE.toOrdersFilter())
        assertEquals(OrdersFilter.MESSAGES, MobileKpiDestination.MESSAGES.toOrdersFilter())
    }
}
