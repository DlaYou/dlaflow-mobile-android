package pl.dlaflow.mobile.feature.orders

import pl.dlaflow.mobile.MobileOrderAddress
import pl.dlaflow.mobile.MobileOrderDetail
import pl.dlaflow.mobile.MobileOrderFilter
import pl.dlaflow.mobile.MobileOrderListItem
import pl.dlaflow.mobile.MobileOrdersPage

internal fun MobileOrdersPage.toOrdersListContent() = OrdersListContent(
    items = data.map(MobileOrderListItem::toOrdersListItem),
    total = total,
    nextOffset = nextOffset,
)
internal fun MobileOrderListItem.toOrdersListItem() = OrdersListItem(
    id = id,
    orderNumber = orderNumber,
    amount = amount,
    currency = currency,
    customer = customer,
    channel = channel,
    createdAt = createdAt,
    shippingDeadlineAt = shippingDeadlineAt,
    shipmentStatus = shipmentStatus,
    shipmentStage = shipmentStage,
    shippedAt = shippedAt,
    deliveredAt = deliveredAt,
    itemCount = itemCount,
    productSummary = productSummary,
    paymentStatus = paymentStatus,
    paymentTone = paymentTone,
    phone = phone,
    shippingMethod = shippingMethod,
    status = status,
    statusTone = statusTone,
    statusColor = statusColor,
    thumbnailUrl = thumbnailUrl,
    badges = OrdersBadges(
        documents = badges.documents,
        messages = badges.messages,
        shipments = badges.shipments,
    ),
    productNames = productNames,
)

internal fun MobileOrderDetail.toOrderDetailContent() = OrderDetailContent(
    id = id,
    orderNumber = orderNumber,
    amount = amount,
    currency = currency,
    createdAt = createdAt,
    shippingDeadlineAt = shippingDeadlineAt,
    status = status,
    statusTone = statusTone,
    statusColor = statusColor,
    productSummary = productSummary,
    itemCount = itemCount,
    customer = OrderCustomer(
        name = customer.name,
        email = customer.email,
        nick = customer.nick,
        phone = customer.phone,
    ),
    delivery = OrderDelivery(
        address = delivery.address.toOrderAddress(),
        method = delivery.method,
    ),
    payment = OrderPayment(
        currency = payment.currency,
        method = payment.method,
        paidAmount = payment.paidAmount,
        status = payment.status,
        tone = payment.tone,
    ),
    items = items.map { item ->
        OrderItem(
            id = item.id,
            name = item.name,
            sku = item.sku,
            quantity = item.quantity,
            lineTotal = item.lineTotal,
            unitPrice = item.unitPrice,
        )
    },
    shipments = shipments.map { shipment ->
        OrderShipment(
            id = shipment.id,
            carrier = shipment.carrier,
            labelReady = shipment.labelReady,
            stage = shipment.stage,
            status = shipment.status,
            trackingNumber = shipment.trackingNumber,
            shippedAt = shipment.shippedAt,
            deliveredAt = shipment.deliveredAt,
        )
    },
    messages = messages.map { message ->
        OrderMessage(
            id = message.id,
            author = message.author,
            body = message.body,
            messageAt = message.messageAt,
        )
    },
    documentsCount = documents.size,
    internalNotesCount = internalNotes.size,
    statusHistoryCount = statusHistory.size,
)

internal fun OrdersFilter.toMobileOrderFilter(): MobileOrderFilter = when (this) {
    OrdersFilter.ALL -> MobileOrderFilter.ALL
    OrdersFilter.NEW -> MobileOrderFilter.NEW
    OrdersFilter.TO_SHIP -> MobileOrderFilter.TO_SHIP
    OrdersFilter.OVERDUE -> MobileOrderFilter.OVERDUE
    OrdersFilter.PROBLEMS -> MobileOrderFilter.PROBLEMS
    OrdersFilter.MESSAGES -> MobileOrderFilter.MESSAGES
}

private fun MobileOrderAddress.toOrderAddress() = OrderAddress(
    name = name,
    company = company,
    pointName = pointName,
    street = street,
    postalCode = postalCode,
    city = city,
    country = country,
    phone = phone,
)
