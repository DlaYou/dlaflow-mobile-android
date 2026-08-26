package pl.dlaflow.mobile.feature.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.OffsetDateTime
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowBackHeader
import pl.dlaflow.mobile.core.designsystem.DlaFlowKeyValue
import pl.dlaflow.mobile.core.designsystem.DlaFlowSecondaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowSkeletonBlock
import pl.dlaflow.mobile.core.designsystem.DlaFlowStateCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnail
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnailLoader
import pl.dlaflow.mobile.core.designsystem.dlaFlowHexColor
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal fun orderMessagesPreview(messages: List<OrderMessage>): List<OrderMessage> =
    messages.sortedByDescending { it.messageAt }.take(3)

@Composable
internal fun OrderDetailPanel(
    colors: DlaFlowComposeColors,
    orderNumber: String,
    state: DlaFlowUiState<OrderDetailContent>?,
    thumbnailLoader: DlaFlowThumbnailLoader,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onOpenMessages: (String) -> Unit = {},
) {
    DlaFlowCard(colors, accent = true) {
        DlaFlowBackHeader(
            colors = colors,
            title = stringResource(R.string.orders_detail_title),
            subtitle = orderNumber.takeIf { it.isNotBlank() }?.let { "#$it" }.orEmpty(),
            subtitleColor = colors.primary,
            backButtonVisualSize = 34.dp,
            backContentDescription = stringResource(R.string.orders_detail_back),
            onBack = onClose,
        )
        when (state) {
            null, DlaFlowUiState.Loading -> OrderDetailSkeleton(colors)
            is DlaFlowUiState.Content -> OrderDetailContentBody(colors, state.data, thumbnailLoader, onOpenMessages)
            is DlaFlowUiState.Offline -> state.lastContent?.let { OrderDetailContentBody(colors, it, thumbnailLoader, onOpenMessages) }
                ?: OrderDetailFailure(colors, state = state, onRetry = onRetry)
            is DlaFlowUiState.Error -> OrderDetailFailure(colors, state = state, onRetry = onRetry)
            DlaFlowUiState.Empty -> Text(stringResource(R.string.orders_detail_load_failed), color = colors.textMuted, fontSize = 12.sp)
            DlaFlowUiState.NoAccess -> Unit
        }
    }
}

@Composable
private fun OrderDetailSkeleton(colors: DlaFlowComposeColors) {
    Spacer(Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(3) {
            DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth().height(58.dp))
        }
    }
}

@Composable
private fun OrderDetailFailure(
    colors: DlaFlowComposeColors,
    state: DlaFlowUiState<OrderDetailContent>,
    onRetry: () -> Unit,
) {
    val message = (state as? DlaFlowUiState.Error)?.message
    DlaFlowStateCard(
        colors = colors,
        icon = Icons.Rounded.Warning,
        iconColor = colors.orange,
        title = message?.let { stringResource(it.titleRes) } ?: stringResource(R.string.mobile_error_offline_title),
        description = message?.let { stringResource(it.descriptionRes) } ?: stringResource(R.string.mobile_error_offline_description),
    )
    if (message?.retryable != false) {
        DlaFlowSecondaryButton(
            colors = colors,
            icon = Icons.Rounded.Refresh,
            text = stringResource(R.string.orders_retry),
            onClick = onRetry,
        )
    }
}

@Composable
private fun OrderDetailContentBody(
    colors: DlaFlowComposeColors,
    order: OrderDetailContent,
    thumbnailLoader: DlaFlowThumbnailLoader,
    onOpenMessages: (String) -> Unit,
) {
    Spacer(Modifier.height(6.dp))
    OrderDetailProductCards(colors, order, thumbnailLoader)
    OrderDetailSection(colors, stringResource(R.string.orders_section_timing)) {
        DlaFlowKeyValue(colors, stringResource(R.string.orders_label_ordered_at), ordersDisplayTimestamp(order.createdAt).ifBlank { stringResource(R.string.orders_value_missing) })
        OrderDetailColoredValue(
            colors = colors,
            label = stringResource(R.string.orders_label_order_status),
            value = ordersStatusValue(order.status, stringResource(R.string.orders_status_check)),
            color = dlaFlowHexColor(order.statusColor) ?: ordersToneColor(colors, order.statusTone),
        )
        val shipment = order.shipments.firstOrNull()
        val shipmentTiming = shipment?.let {
            ordersShipmentTimingPresentation(
                shipmentStage = it.stage,
                shippedAt = it.shippedAt,
                deliveredAt = it.deliveredAt,
                shippingDeadlineAt = order.shippingDeadlineAt,
            )
        }
        DlaFlowKeyValue(
            colors,
            when (shipmentTiming?.kind) {
                OrdersShipmentTimingKind.SHIPPED -> stringResource(R.string.orders_label_shipped_at)
                OrdersShipmentTimingKind.DELIVERED -> stringResource(R.string.orders_label_delivered_at)
                else -> stringResource(R.string.orders_label_shipping_deadline)
            },
            shipmentTiming?.let { ordersShipmentTimingValue(it) } ?: order.shippingDeadlineAt.takeIf { it.isNotBlank() }?.let { ordersShippingDeadlineLabel(it) }
                ?: stringResource(R.string.orders_deadline_unavailable),
        )
    }
    OrderDetailSection(colors, stringResource(R.string.orders_section_customer)) {
        DlaFlowKeyValue(colors, stringResource(R.string.orders_label_customer_name), order.customer.name.ifBlank { stringResource(R.string.orders_value_missing) })
        DlaFlowKeyValue(colors, stringResource(R.string.orders_label_phone), order.customer.phone.ifBlank { stringResource(R.string.orders_value_missing) })
        DlaFlowKeyValue(colors, stringResource(R.string.orders_label_email), order.customer.email.ifBlank { stringResource(R.string.orders_value_missing) })
        DlaFlowKeyValue(colors, stringResource(R.string.orders_label_login), order.customer.nick.ifBlank { stringResource(R.string.orders_value_missing) })
    }
    OrderDetailSection(colors, stringResource(R.string.orders_section_payment)) {
        DlaFlowKeyValue(colors, stringResource(R.string.orders_label_payment_status), order.payment.status.ifBlank { stringResource(R.string.orders_value_check) })
        DlaFlowKeyValue(colors, stringResource(R.string.orders_label_payment_method), order.payment.method.ifBlank { stringResource(R.string.orders_value_missing) })
        DlaFlowKeyValue(colors, stringResource(R.string.orders_label_paid), formatOrdersMoney(order.payment.paidAmount))
    }
    OrderDetailSection(colors, stringResource(R.string.orders_section_delivery)) {
        OrderDetailDeliveryCard(colors, order)
    }
    if (order.shipments.isNotEmpty()) {
        OrderDetailSection(colors, stringResource(R.string.orders_section_shipments)) {
            order.shipments.forEach { shipment ->
                val timing = ordersShipmentTimingPresentation(
                    shipmentStage = shipment.stage,
                    shippedAt = shipment.shippedAt,
                    deliveredAt = shipment.deliveredAt,
                    shippingDeadlineAt = order.shippingDeadlineAt,
                )
                val timingLabel = when (timing.kind) {
                    OrdersShipmentTimingKind.SHIPPED -> stringResource(R.string.orders_label_shipped_at)
                    OrdersShipmentTimingKind.DELIVERED -> stringResource(R.string.orders_label_delivered_at)
                    OrdersShipmentTimingKind.DEADLINE -> if (shipment.labelReady) stringResource(R.string.orders_value_label_ready) else shipment.status
                }
                val timingValue = ordersShipmentTimingValue(timing)
                OrderDetailListRow(
                    colors,
                    shipment.carrier.ifBlank { stringResource(R.string.orders_value_shipment) },
                    shipment.status,
                    "$timingLabel: $timingValue",
                )
            }
        }
    }
    if (order.messages.isNotEmpty()) {
        OrderDetailSection(colors, stringResource(R.string.orders_section_messages)) {
            orderMessagesPreview(order.messages).forEach { message ->
                OrderDetailListRow(
                    colors,
                    message.author.ifBlank { stringResource(R.string.orders_value_customer) },
                    message.body,
                    orderRelativeTime(message.messageAt),
                )
            }
            val threadId = orderMessagesPreview(order.messages).firstNotNullOfOrNull { it.threadId.takeIf(String::isNotBlank) }
            if (threadId != null) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onOpenMessages(threadId) }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResource(R.string.orders_messages_view_conversation),
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun OrderDetailProductCards(
    colors: DlaFlowComposeColors,
    order: OrderDetailContent,
    thumbnailLoader: DlaFlowThumbnailLoader,
) {
    val items = order.items.ifEmpty {
        listOf(
            OrderItem(
                id = "",
                name = order.productSummary.ifBlank { stringResource(R.string.orders_value_product) },
                sku = "",
                quantity = order.itemCount,
                lineTotal = order.amount,
                unitPrice = order.amount,
            ),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("orders_detail_product_card")
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            OrderDetailProductCard(colors, item, thumbnailLoader)
        }
    }
}

@Composable
private fun OrderDetailProductCard(
    colors: DlaFlowComposeColors,
    item: OrderItem,
    thumbnailLoader: DlaFlowThumbnailLoader,
) {
    DlaFlowCard(colors, accent = true) {
        Row(verticalAlignment = Alignment.Top) {
            DlaFlowThumbnail(
                colors = colors,
                url = item.image,
                loader = thumbnailLoader,
                modifier = Modifier.size(96.dp),
                contentDescription = stringResource(R.string.orders_label_product_image, item.name),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    color = colors.textStrong,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 19.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    formatOrdersMoney(item.lineTotal.takeIf { it > 0.0 } ?: item.unitPrice * item.quantity.coerceAtLeast(1)),
                    color = colors.textStrong,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(5.dp))
                val metadata = listOfNotNull(
                    item.sku.takeIf { it.isNotBlank() }?.let { stringResource(R.string.orders_value_sku, it) },
                    item.variantId.takeIf { it.isNotBlank() }?.let { stringResource(R.string.orders_value_variant, it) },
                    stringResource(R.string.orders_value_quantity, item.quantity),
                ).joinToString(" · ")
                Text(
                    metadata,
                    color = colors.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun OrderDetailColoredValue(
    colors: DlaFlowComposeColors,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun OrderDetailDeliveryCard(colors: DlaFlowComposeColors, order: OrderDetailContent) {
    val shipment = order.shipments.firstOrNull()
    val carrier = shipment?.carrier.orEmpty().ifBlank { order.delivery.method }
    val isInPost = carrier.contains("inpost", ignoreCase = true) ||
        order.delivery.method.contains("paczkomat", ignoreCase = true)
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (isInPost) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.inpost_logo),
                    contentDescription = stringResource(R.string.orders_delivery_inpost),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                )
            } else {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.LocalShipping,
                    contentDescription = stringResource(R.string.orders_value_delivery),
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                order.delivery.method.ifBlank { carrier.ifBlank { stringResource(R.string.orders_value_delivery) } },
                color = colors.textStrong,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(orderAddressLabel(order.delivery.address), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
        }
    }
    Spacer(Modifier.height(10.dp))
    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(R.string.orders_label_tracking), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(
            shipment?.trackingNumber?.ifBlank { stringResource(R.string.orders_value_tracking_missing) }
                ?: stringResource(R.string.orders_value_tracking_missing),
            color = colors.textStrong,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun OrderDetailSection(
    colors: DlaFlowComposeColors,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Spacer(Modifier.height(10.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 13.dp),
    ) {
        Text(title, color = colors.textStrong, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 16.sp)
        Spacer(Modifier.height(7.dp))
        content()
    }
}

@Composable
private fun OrderDetailListRow(
    colors: DlaFlowComposeColors,
    title: String,
    subtitle: String,
    value: String,
    imageUrl: String = "",
    thumbnailLoader: DlaFlowThumbnailLoader? = null,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        if (thumbnailLoader != null) {
            DlaFlowThumbnail(
                colors = colors,
                url = imageUrl,
                loader = thumbnailLoader,
                modifier = Modifier.size(46.dp),
                contentDescription = title,
            )
            Spacer(Modifier.width(9.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textStrong, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = colors.textMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(value, color = colors.textStrong, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun orderAddressLabel(address: OrderAddress): String {
    val label = listOf(
        address.name,
        address.company,
        address.pointName,
        address.street,
        listOf(address.postalCode, address.city).filter { it.isNotBlank() }.joinToString(" "),
        address.country,
    ).filter { it.isNotBlank() }.joinToString("\n")
    return label.ifBlank { stringResource(R.string.orders_value_no_address) }
}

@Composable
private fun orderRelativeTime(value: String): String {
    val minutes = runCatching {
        Duration.between(OffsetDateTime.parse(value), OffsetDateTime.now()).toMinutes().coerceAtLeast(0)
    }.getOrNull() ?: return ""
    return when {
        minutes < 1 -> stringResource(R.string.orders_time_now)
        minutes < 60 -> stringResource(R.string.orders_time_minutes_ago, minutes)
        minutes < 24 * 60 -> stringResource(R.string.orders_time_hours, minutes / 60)
        else -> stringResource(R.string.orders_time_days, minutes / (24 * 60))
    }
}
