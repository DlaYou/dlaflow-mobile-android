package pl.dlaflow.mobile.feature.messages

import java.util.Locale
import pl.dlaflow.mobile.MobileMessage
import pl.dlaflow.mobile.MobileMessageAttachment
import pl.dlaflow.mobile.MobileMessageThread
import pl.dlaflow.mobile.MobileMessageThreadDetail
import pl.dlaflow.mobile.MobileMessageOperation
import pl.dlaflow.mobile.MobileMessagesPage
import pl.dlaflow.mobile.MobileMessageOrderLink
import pl.dlaflow.mobile.normalizeMobileOrderMediaUrl
import pl.dlaflow.mobile.MobileOrderDetail

internal fun MobileMessagesPage.toMessagesContent(): MessagesContent = MessagesContent(
    items = items.map(MobileMessageThread::toMessageListItem),
    total = total.coerceAtLeast(0),
    nextCursor = nextCursor.cleanNullable(),
    unreadCount = unreadCount.coerceAtLeast(0),
)

internal fun MobileMessageThread.toMessageListItem(): MessageListItem {
    val safeProvider = providerId.clean()
    return MessageListItem(
        id = id.clean(),
        providerId = safeProvider,
        integrationId = integrationId.clean(),
        providerLabel = providerLabel(safeProvider),
        customerName = buyer.name.clean().ifBlank { "Nieznany klient" },
        customerLogin = buyer.login.clean(),
        subject = subject.clean().ifBlank { "Bez tematu" },
        preview = lastMessage?.toMessagePreview(),
        lastMessageAt = lastMessageAt.clean(),
        messageCount = messageCount.coerceAtLeast(0),
        orderId = orderLink?.orderId.cleanNullable(),
        orderNumber = orderLink?.id.cleanNullable(),
        readAt = readAt.cleanNullable(),
        status = status.clean(),
        channel = providerChannel(safeProvider),
    )
}

internal fun MobileMessageThreadDetail.toMessageThreadDetail(): MessageThreadDetail {
    val safeProvider = providerId.clean()
    return MessageThreadDetail(
        id = id.clean(),
        providerId = safeProvider,
        integrationId = integrationId.clean(),
        providerLabel = providerLabel(safeProvider),
        customerName = buyer.name.clean().ifBlank { "Nieznany klient" },
        customerLogin = buyer.login.clean(),
        customerEmail = buyer.email.cleanNullable(),
        subject = subject.clean().ifBlank { "Bez tematu" },
        lastMessageAt = lastMessageAt.clean(),
        readAt = readAt.cleanNullable(),
        status = status.clean(),
        orderId = orderLink?.orderId.cleanNullable(),
        orderNumber = orderLink?.id.cleanNullable(),
        messages = messages.map(MobileMessage::toMessageBubble),
        nextCursor = nextCursor.cleanNullable(),
        relatedOrder = orderLink?.toMessageRelatedOrderFallback(),
        customerContext = customerContext?.let {
            MessageCustomerContext(
                orderCount = it.orderCount.coerceAtLeast(0),
                totalOrderAmount = it.totalOrderAmount.takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0,
                currency = it.currency.clean().ifBlank { "PLN" },
                customerSince = it.customerSince.clean(),
                activeConversationCount = it.activeConversationCount.coerceAtLeast(0),
            )
        },
    )
}

internal fun MobileOrderDetail.toMessageRelatedOrder(): MessageRelatedOrder = MessageRelatedOrder(
    id = id.clean(),
    orderNumber = orderNumber.clean(),
    amount = amount.takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0,
    currency = currency.clean().ifBlank { "PLN" },
    status = status.clean(),
    statusTone = statusTone.clean(),
    statusColor = statusColor.clean(),
    items = items.map { item ->
        MessageRelatedOrderItem(
            name = item.name.clean().ifBlank { "Produkt" },
            image = normalizeMobileOrderMediaUrl(item.image),
            sku = item.sku.clean(),
            quantity = item.quantity.coerceAtLeast(0),
            unitPrice = item.unitPrice.takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0,
            lineTotal = item.lineTotal.takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0,
        )
    },
)

private fun MobileMessageOrderLink.toMessageRelatedOrderFallback(): MessageRelatedOrder = MessageRelatedOrder(
    id = orderId.clean(),
    orderNumber = id.clean(),
    amount = amount?.takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0,
    currency = currency.clean().ifBlank { "PLN" },
    status = status.clean(),
    statusTone = "",
    statusColor = "",
    items = emptyList(),
)

internal fun MobileMessageOperation.toMessageOperation(): MessageOperation = MessageOperation(
    operationId = operationId.clean(),
    messageId = messageId.cleanNullable(),
    queued = queued,
    duplicate = duplicate,
    status = status.clean(),
)

internal fun MobileMessage.toMessageBubble(): MessageBubble = MessageBubble(
    id = id.clean(),
    author = author.clean().ifBlank { "Wiadomość" },
    direction = direction.toMessageDirection(),
    body = body.clean(),
    messageAt = messageAt.clean(),
    status = status.clean(),
    attachments = attachments.map(MobileMessageAttachment::toMessageAttachment),
)

private fun pl.dlaflow.mobile.MobileMessagePreview.toMessagePreview() = MessagePreview(
    body = body.clean(),
    direction = direction.toMessageDirection(),
    messageAt = messageAt.clean(),
)

private fun MobileMessageAttachment.toMessageAttachment() = MessageAttachment(
    id = id.clean(),
    filename = filename.clean().ifBlank { "Załącznik" },
    contentType = contentType.clean().ifBlank { "application/octet-stream" },
    size = size.coerceAtLeast(0),
    status = status.clean(),
    url = url.clean(),
)

private fun String.toMessageDirection(): MessageDirection = when (trim().lowercase(Locale.ROOT)) {
    "inbound", "incoming", "received" -> MessageDirection.INBOUND
    "outbound", "outgoing", "sent" -> MessageDirection.OUTBOUND
    else -> MessageDirection.UNKNOWN
}

private fun providerChannel(provider: String): MessagesChannel = when (provider.lowercase(Locale.ROOT)) {
    "allegro", "amazon", "ebay", "marketplace" -> MessagesChannel.MARKETPLACE
    "shopify", "woocommerce", "store" -> MessagesChannel.STORE
    "gmail", "email" -> MessagesChannel.EMAIL
    "facebook", "instagram", "messenger", "social" -> MessagesChannel.SOCIAL
    else -> MessagesChannel.ALL
}

private fun providerLabel(provider: String): String = when (provider.lowercase(Locale.ROOT)) {
    "allegro" -> "Allegro"
    "gmail", "email" -> "Gmail"
    "woocommerce" -> "WooCommerce"
    "shopify" -> "Shopify"
    "facebook" -> "Facebook"
    "instagram" -> "Instagram"
    else -> provider.ifBlank { "Panel" }
}

private fun String?.cleanNullable(): String? = this?.clean()?.takeIf(String::isNotBlank)
private fun String.clean(): String = trim().take(2000)
