package pl.dlaflow.mobile.feature.orders

import java.time.ZoneId

internal enum class OrdersShipmentTimingKind {
    DEADLINE,
    SHIPPED,
    DELIVERED,
}

internal data class OrdersShipmentTimingPresentation(
    val kind: OrdersShipmentTimingKind,
    val timestamp: String = "",
    val shippingDeadlineAt: String = "",
)

internal fun ordersShipmentTimingPresentation(
    shipmentStage: String,
    shippedAt: String,
    deliveredAt: String,
    shippingDeadlineAt: String,
    zone: ZoneId = ZoneId.systemDefault(),
): OrdersShipmentTimingPresentation {
    val stage = shipmentStage.trim().lowercase()
    val delivered = stage == "delivered"
    val shipped = stage in setOf("sent", "transit", "delivery", "pickup", "delivered", "problem", "returned")

    if (delivered) {
        return OrdersShipmentTimingPresentation(
            kind = OrdersShipmentTimingKind.DELIVERED,
            timestamp = deliveredAt.takeIf { ordersDisplayTimestamp(it, zone).isNotBlank() }.orEmpty(),
        )
    }

    if (shipped) {
        return OrdersShipmentTimingPresentation(
            kind = OrdersShipmentTimingKind.SHIPPED,
            timestamp = shippedAt.takeIf { ordersDisplayTimestamp(it, zone).isNotBlank() }.orEmpty(),
        )
    }

    return OrdersShipmentTimingPresentation(
        kind = OrdersShipmentTimingKind.DEADLINE,
        shippingDeadlineAt = shippingDeadlineAt,
    )
}
