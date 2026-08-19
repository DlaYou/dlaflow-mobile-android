package pl.dlaflow.mobile.feature.orders

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderTimingTest {
    private val warsaw = ZoneId.of("Europe/Warsaw")
    private val now = Instant.parse("2026-07-18T10:00:00Z")

    @Test
    fun `order timestamp converts UTC to device zone`() {
        assertEquals(
            "18.07.2026, 12:00",
            ordersDisplayTimestamp("2026-07-18T10:00:00Z", warsaw),
        )
    }

    @Test
    fun `deadline presentation uses local exact time and remaining hours`() {
        assertEquals(
            OrdersDeadlinePresentation(OrdersDeadlineKind.HOURS, 2, "18.07, 14:00"),
            ordersDeadlinePresentation("2026-07-18T12:00:00Z", now, warsaw),
        )
    }

    @Test
    fun `deadline presentation distinguishes minutes and overdue values`() {
        assertEquals(
            OrdersDeadlinePresentation(OrdersDeadlineKind.MINUTES, 45, "18.07, 12:45"),
            ordersDeadlinePresentation("2026-07-18T10:45:00Z", now, warsaw),
        )
        assertEquals(
            OrdersDeadlinePresentation(OrdersDeadlineKind.OVERDUE, null, "18.07, 11:00"),
            ordersDeadlinePresentation("2026-07-18T09:00:00Z", now, warsaw),
        )
    }

    @Test
    fun `invalid or missing deadline is unavailable`() {
        val expected = OrdersDeadlinePresentation(OrdersDeadlineKind.UNAVAILABLE, null, "")
        assertEquals(expected, ordersDeadlinePresentation("", now, warsaw))
        assertEquals(expected, ordersDeadlinePresentation("not-a-date", now, warsaw))
    }

    @Test
    fun `delivered shipment shows delivery date`() {
        assertEquals(
            OrdersShipmentTimingPresentation(OrdersShipmentTimingKind.DELIVERED, timestamp = "2026-08-19T09:00:00Z"),
            ordersShipmentTimingPresentation(
                shipmentStage = "delivered",
                shippedAt = "2026-08-18T14:00:00Z",
                deliveredAt = "2026-08-19T09:00:00Z",
                shippingDeadlineAt = "2026-08-20T12:00:00Z",
                zone = warsaw,
            ),
        )
    }

    @Test
    fun `shipped or later shipment shows dispatch date`() {
        assertEquals(
            OrdersShipmentTimingPresentation(OrdersShipmentTimingKind.SHIPPED, timestamp = "2026-07-18T14:00:00Z"),
            ordersShipmentTimingPresentation(
                shipmentStage = "transit",
                shippedAt = "2026-07-18T14:00:00Z",
                deliveredAt = "",
                shippingDeadlineAt = "2026-07-20T12:00:00Z",
                zone = warsaw,
            ),
        )
    }

    @Test
    fun `pre-shipment status shows deadline`() {
        assertEquals(
            OrdersShipmentTimingPresentation(OrdersShipmentTimingKind.DEADLINE, shippingDeadlineAt = "2026-07-20T12:00:00Z"),
            ordersShipmentTimingPresentation(
                shipmentStage = "pending",
                shippedAt = "",
                deliveredAt = "",
                shippingDeadlineAt = "2026-07-20T12:00:00Z",
                zone = warsaw,
            ),
        )
    }

    @Test
    fun `missing integration date is explicit and never guessed`() {
        assertEquals(
            OrdersShipmentTimingPresentation(OrdersShipmentTimingKind.DELIVERED),
            ordersShipmentTimingPresentation(
                shipmentStage = "delivered",
                shippedAt = "",
                deliveredAt = "not-a-date",
                shippingDeadlineAt = "2026-07-20T12:00:00Z",
                zone = warsaw,
            ),
        )
    }
}
