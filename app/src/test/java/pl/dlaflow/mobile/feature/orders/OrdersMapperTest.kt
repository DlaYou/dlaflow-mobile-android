package pl.dlaflow.mobile.feature.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import pl.dlaflow.mobile.MobileOrderAddress
import pl.dlaflow.mobile.MobileOrderBadges
import pl.dlaflow.mobile.MobileOrderCustomer
import pl.dlaflow.mobile.MobileOrderDelivery
import pl.dlaflow.mobile.MobileOrderDetail
import pl.dlaflow.mobile.MobileOrderDocument
import pl.dlaflow.mobile.MobileOrderFilter
import pl.dlaflow.mobile.MobileOrderItem
import pl.dlaflow.mobile.MobileOrderListItem
import pl.dlaflow.mobile.MobileOrderMessage
import pl.dlaflow.mobile.MobileOrderPayment
import pl.dlaflow.mobile.MobileOrderShipment
import pl.dlaflow.mobile.MobileOrderStatusHistory
import pl.dlaflow.mobile.MobileOrdersPage

class OrdersMapperTest {
    @Test
    fun `list page maps transport rows to an independent presentation snapshot`() {
        val transportRows = mutableListOf(orderListDto())
        val transport = MobileOrdersPage(
            data = transportRows,
            count = 1,
            limit = 20,
            nextOffset = 20,
            offset = 0,
            total = 21,
        )

        val content = transport.toOrdersListContent()
        transportRows.clear()

        assertEquals(1, content.items.size)
        assertEquals(21, content.total)
        assertEquals(20, content.nextOffset)
        assertEquals(
            OrdersListItem(
                id = "order-id-1",
                orderNumber = "ORD-1001",
                amount = 123.45,
                currency = "PLN",
                customer = "Klient testowy",
                channel = "Allegro",
                createdAt = "2026-07-18T08:00:00Z",
                itemCount = 2,
                productSummary = "Produkt A + Produkt B",
                paymentStatus = "Opłacone",
                paymentTone = "success",
                phone = "+48100000000",
                shippingMethod = "Paczkomat",
                status = "processing",
                statusTone = "info",
                statusColor = "#112233",
                thumbnailUrl = "/api/mobile/media/order-1",
                badges = OrdersBadges(documents = 1, messages = 2, shipments = 1),
            ),
            content.items.single(),
        )
        assertNotSame(transportRows, content.items)
    }

    @Test
    fun `list page preserves API status and payment presentation fields`() {
        val transportItem = orderListDto().copy(
            status = "Gotowe do odbioru",
            statusTone = "warning",
            paymentStatus = "Płatność przy odbiorze",
            paymentTone = "info",
        )

        val item = MobileOrdersPage(
            data = listOf(transportItem),
            count = 1,
            limit = 20,
            nextOffset = null,
            offset = 0,
            total = 1,
        ).toOrdersListContent().items.single()

        assertEquals("Gotowe do odbioru", item.status)
        assertEquals("warning", item.statusTone)
        assertEquals("Płatność przy odbiorze", item.paymentStatus)
        assertEquals("info", item.paymentTone)
    }

    @Test
    fun `list mapper preserves product names supplied by the API`() {
        val item = MobileOrdersPage(
            data = listOf(orderListDto().copy(productNames = listOf("Produkt A", "Produkt B"))),
            count = 1,
            limit = 20,
            nextOffset = null,
            offset = 0,
            total = 1,
        ).toOrdersListContent().items.single()

        assertEquals(listOf("Produkt A", "Produkt B"), item.productNames)
    }

    @Test
    fun `list mapper preserves canonical shipment timing fields`() {
        val item = MobileOrdersPage(
            data = listOf(orderListDto().copy(
                shipmentStatus = "W trasie",
                shipmentStage = "transit",
                shippedAt = "2026-08-18T14:00:00Z",
                deliveredAt = "",
            )),
            count = 1,
            limit = 20,
            nextOffset = null,
            offset = 0,
            total = 1,
        ).toOrdersListContent().items.single()

        assertEquals("W trasie", item.shipmentStatus)
        assertEquals("transit", item.shipmentStage)
        assertEquals("2026-08-18T14:00:00Z", item.shippedAt)
        assertEquals("", item.deliveredAt)
    }

    @Test
    fun `detail maps only presentation projections and copies nested lists`() {
        val transportItems = mutableListOf(orderItemDto())
        val transportMessages = mutableListOf(orderMessageDto())
        val transport = orderDetailDto(items = transportItems, messages = transportMessages)

        val detail = transport.toOrderDetailContent()
        transportItems.clear()
        transportMessages.clear()

        assertEquals("order-id-1", detail.id)
        assertEquals("ORD-1001", detail.orderNumber)
        assertEquals(123.45, detail.amount, 0.0)
        assertEquals("PLN", detail.currency)
        assertEquals("processing", detail.status)
        assertEquals("info", detail.statusTone)
        assertEquals("#112233", detail.statusColor)
        assertEquals("Produkt A + Produkt B", detail.productSummary)
        assertEquals(2, detail.itemCount)
        assertEquals(
            OrderCustomer(
                name = "Klient testowy",
                email = "test@example.invalid",
                nick = "tester",
                phone = "+48100000000",
            ),
            detail.customer,
        )
        assertEquals("Paczkomat", detail.delivery.method)
        assertEquals("Warszawa", detail.delivery.address.city)
        assertEquals(OrderPayment("PLN", "Przelew", 123.45, "Opłacone", "success"), detail.payment)
        assertEquals(OrderItem("item-1", "Produkt A", "SKU-1", 2, 123.45, 61.725), detail.items.single())
        assertEquals(OrderShipment("shipment-1", "InPost", true, "Nadana", "TRACK-1", "2026-07-18T14:00:00Z", "", "sent"), detail.shipments.single())
        assertEquals(OrderMessage("message-1", "Klient", "Dziękuję", "2026-07-18T09:00:00Z"), detail.messages.single())
        assertEquals(1, detail.documentsCount)
        assertEquals(1, detail.internalNotesCount)
        assertEquals(1, detail.statusHistoryCount)
    }

    @Test
    fun `orders filters map to unchanged legacy API values only at mapper seam`() {
        assertEquals(MobileOrderFilter.ALL, OrdersFilter.ALL.toMobileOrderFilter())
        assertEquals(MobileOrderFilter.NEW, OrdersFilter.NEW.toMobileOrderFilter())
        assertEquals(MobileOrderFilter.TO_SHIP, OrdersFilter.TO_SHIP.toMobileOrderFilter())
        assertEquals(MobileOrderFilter.PROBLEMS, OrdersFilter.PROBLEMS.toMobileOrderFilter())
        assertEquals(MobileOrderFilter.OVERDUE, OrdersFilter.OVERDUE.toMobileOrderFilter())
        assertEquals(MobileOrderFilter.MESSAGES, OrdersFilter.MESSAGES.toMobileOrderFilter())
    }

    @Test
    fun `visible orders filters keep secondary workflows off the toolbar`() {
        assertEquals(
            listOf(OrdersFilter.ALL, OrdersFilter.NEW, OrdersFilter.TO_SHIP),
            visibleOrdersFilters,
        )
    }

    @Test
    fun `list product presentation keeps two names and exposes remaining items`() {
        assertEquals(
            listOf("Produkt A", "Produkt B", "+1 więcej"),
            ordersProductLines(
                productNames = listOf("Produkt A", "Produkt B", "Produkt C"),
                fallbackSummary = "Produkt A + 2",
            ),
        )
    }

    @Test
    fun `list product presentation falls back to API summary`() {
        assertEquals(
            listOf("Produkt A + 1"),
            ordersProductLines(
                productNames = emptyList(),
                fallbackSummary = "Produkt A + 1",
            ),
        )
    }
}

internal fun orderListDto(
    id: String = "order-id-1",
    orderNumber: String = "ORD-1001",
) = MobileOrderListItem(
    amount = 123.45,
    badges = MobileOrderBadges(documents = 1, messages = 2, shipments = 1),
    channel = "Allegro",
    createdAt = "2026-07-18T08:00:00Z",
    currency = "PLN",
    customer = "Klient testowy",
    email = "test@example.invalid",
    externalId = "external-1",
    id = id,
    itemCount = 2,
    orderNumber = orderNumber,
    paymentStatus = "Opłacone",
    paymentTone = "success",
    phone = "+48100000000",
    productSummary = "Produkt A + Produkt B",
    shippingMethod = "Paczkomat",
    status = "processing",
    statusTone = "info",
    statusColor = "#112233",
    thumbnailUrl = "/api/mobile/media/order-1",
    updatedAt = "2026-07-18T09:00:00Z",
)

internal fun orderDetailDto(
    items: List<MobileOrderItem> = listOf(orderItemDto()),
    messages: List<MobileOrderMessage> = listOf(orderMessageDto()),
) = MobileOrderDetail(
    amount = 123.45,
    billingAddress = orderAddressDto(),
    channel = "Allegro",
    createdAt = "2026-07-18T08:00:00Z",
    currency = "PLN",
    customer = MobileOrderCustomer(
        email = "test@example.invalid",
        name = "Klient testowy",
        nick = "tester",
        phone = "+48100000000",
    ),
    delivery = MobileOrderDelivery(address = orderAddressDto(), method = "Paczkomat"),
    documents = listOf(MobileOrderDocument("document-1", "2026-07-18", "FV/1", "issued", "invoice")),
    externalId = "external-1",
    id = "order-id-1",
    itemCount = 2,
    items = items,
    messages = messages,
    internalNotes = listOf(orderMessageDto(id = "note-1")),
    orderNumber = "ORD-1001",
    payment = MobileOrderPayment("PLN", 0.0, "Przelew", 123.45, "Opłacone", "success", 123.45),
    productSummary = "Produkt A + Produkt B",
    shipments = listOf(MobileOrderShipment("InPost", "2026-07-18", "shipment-1", true, "Nadana", "TRACK-1", "", "2026-07-18T14:00:00Z", "", "sent")),
    status = "processing",
    statusHistory = listOf(MobileOrderStatusHistory("2026-07-18", "panel", "processing")),
    statusTone = "info",
    statusColor = "#112233",
    updatedAt = "2026-07-18T09:00:00Z",
)

private fun orderAddressDto() = MobileOrderAddress(
    city = "Warszawa",
    company = "",
    country = "PL",
    name = "Klient testowy",
    phone = "+48100000000",
    pointName = "WAW01",
    postalCode = "00-001",
    street = "Testowa 1",
)

private fun orderItemDto() = MobileOrderItem(
    currency = "PLN",
    ean = "5900000000000",
    id = "item-1",
    image = "",
    lineTotal = 123.45,
    name = "Produkt A",
    offerId = "offer-1",
    productId = "product-1",
    quantity = 2,
    sku = "SKU-1",
    unitPrice = 61.725,
    variantId = "",
)

private fun orderMessageDto(id: String = "message-1") = MobileOrderMessage(
    author = "Klient",
    body = "Dziękuję",
    direction = "incoming",
    id = id,
    messageAt = "2026-07-18T09:00:00Z",
    source = "Allegro",
    status = "read",
)
