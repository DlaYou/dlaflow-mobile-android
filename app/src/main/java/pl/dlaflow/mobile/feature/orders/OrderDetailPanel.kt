package pl.dlaflow.mobile.feature.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.OffsetDateTime
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowKeyValue
import pl.dlaflow.mobile.core.designsystem.DlaFlowMetricBox
import pl.dlaflow.mobile.core.designsystem.dlaFlowHexColor
import pl.dlaflow.mobile.core.designsystem.DlaFlowSecondaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowSkeletonBlock
import pl.dlaflow.mobile.core.designsystem.DlaFlowStateCard
import pl.dlaflow.mobile.core.state.DlaFlowUiState
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnail
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnailLoader

@Composable
internal fun OrderDetailPanel(
    colors: DlaFlowComposeColors,
    state: DlaFlowUiState<OrderDetailContent>?,
    thumbnailLoader: DlaFlowThumbnailLoader,
    onClose: () -> Unit,
    onRetry: () -> Unit,
) {
    DlaFlowCard(colors, accent = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.orders_detail_title),
                color = colors.textStrong,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.orders_detail_back), color = colors.primary, fontWeight = FontWeight.ExtraBold)
            }
        }
        when (state) {
            null, DlaFlowUiState.Loading -> OrderDetailSkeleton(colors)
            is DlaFlowUiState.Content -> OrderDetailContentBody(colors, state.data, thumbnailLoader)
            is DlaFlowUiState.Offline -> state.lastContent?.let { OrderDetailContentBody(colors, it, thumbnailLoader) }
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
) {
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.orders_number, order.orderNumber), color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    Text(order.customer.name, color = colors.textStrong, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DlaFlowMetricBox(colors, stringResource(R.string.orders_metric_value), formatOrdersMoney(order.amount), modifier = Modifier.weight(1f))
        DlaFlowMetricBox(
            colors,
            stringResource(R.string.orders_metric_status),
            ordersStatusValue(order.status, stringResource(R.string.orders_status_check)),
            valueColor = dlaFlowHexColor(order.statusColor) ?: ordersToneColor(colors, order.statusTone),
            modifier = Modifier.weight(1f),
        )
    }
    OrderDetailSection(colors, stringResource(R.string.orders_section_timing)) {
        DlaFlowKeyValue(colors, stringResource(R.string.orders_label_ordered_at), ordersDisplayTimestamp(order.createdAt).ifBlank { stringResource(R.string.orders_value_missing) })
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
        DlaFlowKeyValue(colors, stringResource(R.string.orders_label_delivery_method), order.delivery.method.ifBlank { stringResource(R.string.orders_value_delivery) })
        Text(orderAddressLabel(order.delivery.address), color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
    }
    OrderDetailSection(colors, stringResource(R.string.orders_section_products)) {
        val items = order.items.ifEmpty {
            listOf(OrderItem("", order.productSummary.ifBlank { stringResource(R.string.orders_value_product) }, "", order.itemCount, order.amount, order.amount))
        }
        items.forEach { item ->
            OrderDetailProductRow(colors, item, thumbnailLoader)
        }
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
                    shipment.trackingNumber.ifBlank { shipment.status },
                    "$timingLabel: $timingValue",
                )
            }
        }
    }
    if (order.messages.isNotEmpty()) {
        OrderDetailSection(colors, stringResource(R.string.orders_section_messages)) {
            order.messages.take(3).forEach { message ->
                OrderDetailListRow(
                    colors,
                    message.author.ifBlank { stringResource(R.string.orders_value_customer) },
                    message.body,
                    orderRelativeTime(message.messageAt),
                )
            }
        }
    }
}

@Composable
private fun OrderDetailProductRow(
    colors: DlaFlowComposeColors,
    item: OrderItem,
    thumbnailLoader: DlaFlowThumbnailLoader,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        DlaFlowThumbnail(
            colors = colors,
            url = item.image,
            loader = thumbnailLoader,
            modifier = Modifier.size(88.dp),
            contentDescription = item.name,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                color = colors.textStrong,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            listOfNotNull(
                item.sku.takeIf { it.isNotBlank() }?.let { stringResource(R.string.orders_value_sku, it) },
                stringResource(R.string.orders_value_quantity, item.quantity),
            ).joinToString(" · ").let { metadata ->
                Text(metadata, color = colors.textMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                formatOrdersMoney(item.lineTotal.takeIf { it > 0.0 } ?: item.unitPrice * item.quantity.coerceAtLeast(1)),
                color = colors.textStrong,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
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
private fun OrderDetailListRow(colors: DlaFlowComposeColors, title: String, subtitle: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textStrong, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = colors.textMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(value, color = colors.textStrong, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
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
