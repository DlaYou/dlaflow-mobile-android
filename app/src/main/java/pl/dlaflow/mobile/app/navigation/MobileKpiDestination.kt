package pl.dlaflow.mobile.app.navigation

import pl.dlaflow.mobile.feature.orders.OrdersFilter

internal enum class MobileKpiDestination {
    NEW_ORDERS,
    TO_SHIP,
    OVERDUE,
    MESSAGES,
}

internal fun MobileKpiDestination.toOrdersFilterOrNull(): OrdersFilter? = when (this) {
    MobileKpiDestination.NEW_ORDERS -> OrdersFilter.NEW
    MobileKpiDestination.TO_SHIP -> OrdersFilter.TO_SHIP
    MobileKpiDestination.OVERDUE -> OrdersFilter.OVERDUE
    MobileKpiDestination.MESSAGES -> null
}
