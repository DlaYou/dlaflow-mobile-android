package pl.dlaflow.mobile.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.dlaflow.mobile.feature.orders.OrdersFilter

class MobileKpiDestinationTest {
    @Test
    fun `order kpi destinations map to canonical orders filters`() {
        assertEquals(OrdersFilter.NEW, MobileKpiDestination.NEW_ORDERS.toOrdersFilterOrNull())
        assertEquals(OrdersFilter.TO_SHIP, MobileKpiDestination.TO_SHIP.toOrdersFilterOrNull())
        assertEquals(OrdersFilter.OVERDUE, MobileKpiDestination.OVERDUE.toOrdersFilterOrNull())
    }

    @Test
    fun `messages destination has no orders filter`() {
        assertEquals(null, MobileKpiDestination.MESSAGES.toOrdersFilterOrNull())
    }
}
