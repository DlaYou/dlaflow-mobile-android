package pl.dlaflow.mobile.feature.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowFilterChip
import pl.dlaflow.mobile.core.designsystem.DlaFlowInter
import pl.dlaflow.mobile.core.designsystem.DlaFlowScreenHeader
import pl.dlaflow.mobile.core.designsystem.DlaFlowSearchField
import pl.dlaflow.mobile.core.designsystem.DlaFlowSecondaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowSkeletonBlock
import pl.dlaflow.mobile.core.designsystem.DlaFlowStateCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusField
import pl.dlaflow.mobile.core.designsystem.dlaFlowHexColor
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusTone
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnail
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnailLoader
import pl.dlaflow.mobile.core.state.DlaFlowUiState

@Composable
internal fun OrdersFeatureScreen(
    colors: DlaFlowComposeColors,
    modifier: Modifier = Modifier,
    state: OrdersUiState,
    thumbnailLoader: DlaFlowThumbnailLoader,
    leadContent: @Composable () -> Unit,
    onAction: (OrdersAction) -> Unit,
) {
    val content = state.listContentOrNull()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("orders_feature_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DlaFlowScreenHeader(
            colors = colors,
            title = stringResource(R.string.orders_title),
            subtitle = ordersSummary(state, content),
        )
        DlaFlowSearchField(
            colors = colors,
            value = state.query.search,
            placeholder = stringResource(R.string.orders_search_placeholder),
            onValueChange = { onAction(OrdersAction.SearchChanged(it)) },
        )
        OrdersFilterChips(
            colors = colors,
            selected = state.query.filter,
            onFilterChange = { onAction(OrdersAction.FilterChanged(it)) },
        )

        if (state.route is OrdersRoute.Detail) {
            OrderDetailPanel(
                colors = colors,
                state = state.detailState,
                onClose = { onAction(OrdersAction.CloseDetail) },
                onRetry = { onAction(OrdersAction.Retry) },
            )
            return@Column
        }

        if (state.listState == DlaFlowUiState.NoAccess) {
            DlaFlowStateCard(
                colors = colors,
                icon = Icons.Rounded.Warning,
                iconColor = colors.danger,
                title = stringResource(R.string.orders_no_access_title),
                description = stringResource(R.string.orders_no_access_description),
            )
            return@Column
        }

        leadContent()

        Box(modifier = Modifier.fillMaxWidth()) {
            when (val listState = state.listState) {
                DlaFlowUiState.Loading -> OrdersListSkeleton(colors)
                DlaFlowUiState.Empty -> OrdersEmptyState(colors)
                is DlaFlowUiState.Content -> OrdersList(
                    colors = colors,
                    content = listState.data,
                    thumbnailLoader = thumbnailLoader,
                    onOpenOrder = { onAction(OrdersAction.OpenOrder(it)) },
                )
                is DlaFlowUiState.Offline -> listState.lastContent?.let { retained ->
                    OrdersList(
                        colors = colors,
                        content = retained,
                        thumbnailLoader = thumbnailLoader,
                        onOpenOrder = { onAction(OrdersAction.OpenOrder(it)) },
                    )
                    OrdersFailureState(colors, state, onAction)
                } ?: OrdersFailureState(colors, state, onAction)

                is DlaFlowUiState.Error -> OrdersFailureState(colors, state, onAction)
                DlaFlowUiState.NoAccess -> Unit
            }
            OrdersRefreshOverlay(
                colors = colors,
                visible = state.isRefreshing && content != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(1f)
                    .testTag("orders_refresh_overlay"),
            )
        }
        if (content?.nextOffset != null && content.items.isNotEmpty()) {
            DlaFlowSecondaryButton(
                colors = colors,
                icon = Icons.Rounded.Refresh,
                text = stringResource(if (state.isLoadingMore) R.string.orders_loading_more else R.string.orders_load_more),
                enabled = !state.isLoadingMore,
                onClick = { onAction(OrdersAction.LoadMore) },
            )
        }
    }
}

@Composable
private fun OrdersFilterChips(
    colors: DlaFlowComposeColors,
    selected: OrdersFilter,
    onFilterChange: (OrdersFilter) -> Unit,
) {
    val columnCount = if (ordersUsesCompactLayout()) {
        2
    } else {
        3
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        visibleOrdersFilters.chunked(columnCount).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { filter ->
                    DlaFlowFilterChip(
                        colors = colors,
                        label = ordersFilterLabel(filter),
                        selected = filter == selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onFilterChange(filter) },
                    )
                }
                repeat(columnCount - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ordersUsesCompactLayout(): Boolean =
    LocalConfiguration.current.screenWidthDp < 480 && LocalDensity.current.fontScale >= 1.2f

@Composable
private fun OrdersListSkeleton(colors: DlaFlowComposeColors) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(4) {
            DlaFlowCard(colors) {
                Row(verticalAlignment = Alignment.Top) {
                    DlaFlowSkeletonBlock(colors, Modifier.size(38.dp), radius = 8.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth(0.68f).height(15.dp))
                        Spacer(Modifier.height(7.dp))
                        DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth(0.44f).height(10.dp))
                        Spacer(Modifier.height(7.dp))
                        DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth(0.88f).height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersRefreshOverlay(
    colors: DlaFlowComposeColors,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
    ) {
        DlaFlowCard(colors, accent = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = colors.primary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.orders_refreshing_title),
                        color = colors.textStrong,
                        fontFamily = DlaFlowInter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.orders_refreshing_description),
                        color = colors.textMuted,
                        fontFamily = DlaFlowInter,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrdersEmptyState(colors: DlaFlowComposeColors) {
    DlaFlowStateCard(
        colors = colors,
        icon = Icons.Rounded.Search,
        iconColor = colors.textMuted,
        title = stringResource(R.string.orders_empty_title),
        description = stringResource(R.string.orders_empty_description),
    )
}

@Composable
private fun OrdersFailureState(
    colors: DlaFlowComposeColors,
    state: OrdersUiState,
    onAction: (OrdersAction) -> Unit,
) {
    val message = when (val current = state.listState) {
        is DlaFlowUiState.Error -> current.message
        else -> state.transientMessage
    }
    DlaFlowStateCard(
        colors = colors,
        icon = Icons.Rounded.Warning,
        iconColor = colors.orange,
        title = message?.let { stringResource(it.titleRes) } ?: stringResource(R.string.mobile_error_unknown_title),
        description = message?.let { stringResource(it.descriptionRes) } ?: stringResource(R.string.mobile_error_unknown_description),
    )
    if (message?.retryable != false) {
        DlaFlowSecondaryButton(
            colors = colors,
            icon = Icons.Rounded.Refresh,
            text = stringResource(R.string.orders_retry),
            onClick = { onAction(OrdersAction.Retry) },
        )
    }
}

@Composable
private fun OrdersList(
    colors: DlaFlowComposeColors,
    content: OrdersListContent,
    thumbnailLoader: DlaFlowThumbnailLoader,
    onOpenOrder: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        content.items.forEach { order ->
            OrdersListCard(colors, order, thumbnailLoader) { onOpenOrder(order.orderNumber) }
        }
    }
}

@Composable
private fun OrdersListCard(
    colors: DlaFlowComposeColors,
    order: OrdersListItem,
    thumbnailLoader: DlaFlowThumbnailLoader,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        DlaFlowCard(colors, accent = order.statusTone.equals("warning", ignoreCase = true)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(order.customer, color = colors.textStrong, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(R.string.orders_number_channel, order.orderNumber, order.channel.ifBlank { stringResource(R.string.orders_value_panel) }),
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(formatOrdersMoney(order.amount), color = colors.textStrong, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                    Spacer(Modifier.height(12.dp))
                    OrdersProductStrip(colors, order, thumbnailLoader)
                    Spacer(Modifier.height(12.dp))
                    OrderStatusFields(colors, order)
                    Spacer(Modifier.height(10.dp))
                    OrderTimingLine(colors, order)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ordersQuickInfo(order), color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(ordersBadgeSummary(order), color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersProductStrip(
    colors: DlaFlowComposeColors,
    order: OrdersListItem,
    thumbnailLoader: DlaFlowThumbnailLoader,
) {
    val products = ordersDisplayProducts(order, stringResource(R.string.orders_value_product))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        products.take(2).forEachIndexed { index, product ->
            Column(
                modifier = if (products.size == 1) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.weight(1f)
                },
            ) {
                DlaFlowThumbnail(
                    colors = colors,
                    url = product.image.ifBlank { if (index == 0) order.thumbnailUrl else "" },
                    loader = thumbnailLoader,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ordersProductImageAspectRatio(products.size)),
                    contentDescription = product.name,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = product.name,
                    color = colors.textStrong,
                    fontSize = 10.8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 13.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ordersProductMeta(product)?.let { meta ->
                    Text(
                        text = meta,
                        color = colors.textMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        repeat((2 - products.size.coerceAtMost(2)).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
    }
    if (products.size > 2) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.orders_products_more, products.size - 2),
            color = colors.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

internal fun ordersProductImageAspectRatio(productCount: Int): Float =
    if (productCount == 1) 1.7f else 1.45f

private fun ordersDisplayProducts(order: OrdersListItem, fallbackName: String): List<OrdersListProduct> {
    if (order.products.isNotEmpty()) return order.products
    val names = order.productNames.map(String::trim).filter(String::isNotBlank).distinct()
    if (names.isNotEmpty()) {
        return names.mapIndexed { index, name ->
            OrdersListProduct(
                image = if (index == 0) order.thumbnailUrl else "",
                name = name,
                quantity = 0,
                sku = "",
            )
        }
    }
    return listOf(
        OrdersListProduct(
            image = order.thumbnailUrl,
            name = order.productSummary.ifBlank { fallbackName },
            quantity = order.itemCount,
            sku = "",
        ),
    )
}

private fun ordersProductMeta(product: OrdersListProduct): String? {
    val parts = buildList {
        if (product.quantity > 0) add("${product.quantity} szt.")
        if (product.sku.isNotBlank()) add("SKU ${product.sku}")
    }
    return parts.takeIf(List<String>::isNotEmpty)?.joinToString(" · ")
}

@Composable
private fun OrderStatusFields(colors: DlaFlowComposeColors, order: OrdersListItem) {
    val fallback = stringResource(R.string.orders_status_check)
    val fulfillment = ordersStatusValue(order.status, fallback)
    val payment = ordersStatusValue(order.paymentStatus, fallback)
    val fulfillmentColor = ordersFulfillmentStatusColor(colors, order)
    if (ordersUsesStackedStatusLayout()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DlaFlowStatusField(
                colors = colors,
                label = stringResource(R.string.orders_status_fulfillment_label),
                value = fulfillment,
                tone = ordersStatusTone(order.statusTone),
                accentColor = fulfillmentColor,
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
            DlaFlowStatusField(
                colors = colors,
                label = stringResource(R.string.orders_status_payment_label),
                value = payment,
                tone = ordersStatusTone(order.paymentTone),
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DlaFlowStatusField(
                colors = colors,
                label = stringResource(R.string.orders_status_fulfillment_label),
                value = fulfillment,
                tone = ordersStatusTone(order.statusTone),
                accentColor = fulfillmentColor,
                compact = true,
                modifier = Modifier.weight(1f),
            )
            DlaFlowStatusField(
                colors = colors,
                label = stringResource(R.string.orders_status_payment_label),
                value = payment,
                tone = ordersStatusTone(order.paymentTone),
                compact = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ordersUsesStackedStatusLayout(): Boolean {
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    return configuration.screenWidthDp <= 360 ||
        (configuration.screenWidthDp < 480 && fontScale >= 1.2f)
}

@Composable
private fun ordersSummary(state: OrdersUiState, content: OrdersListContent?): String {
    val count = content?.total?.coerceAtLeast(content.items.size) ?: 0
    val base = when {
        state.listState == DlaFlowUiState.Loading && content == null -> stringResource(R.string.orders_summary_loading)
        count == 1 -> stringResource(R.string.orders_summary_one)
        count % 10 in 2..4 && count % 100 !in 12..14 -> stringResource(R.string.orders_summary_few, count)
        count > 1 -> stringResource(R.string.orders_summary_many, count)
        else -> stringResource(R.string.orders_summary_list)
    }
    return when {
        state.listState == DlaFlowUiState.NoAccess -> stringResource(R.string.orders_summary_no_access, base)
        state.isRefreshing && content != null -> stringResource(R.string.orders_summary_refreshing, base)
        else -> stringResource(R.string.orders_summary_view, base)
    }
}

@Composable
private fun ordersFilterLabel(filter: OrdersFilter): String = stringResource(
    when (filter) {
        OrdersFilter.ALL -> R.string.orders_filter_all
        OrdersFilter.NEW -> R.string.orders_filter_new
        OrdersFilter.TO_SHIP -> R.string.orders_filter_to_ship
        OrdersFilter.OVERDUE -> R.string.orders_filter_overdue
        OrdersFilter.PROBLEMS -> R.string.orders_filter_problems
        OrdersFilter.MESSAGES -> R.string.orders_filter_messages
    },
)

internal fun ordersStatusValue(value: String, fallback: String): String = value.trim().ifBlank { fallback }

internal fun ordersStatusTone(value: String): DlaFlowStatusTone = when (value.trim().lowercase(Locale.ROOT)) {
    "brand" -> DlaFlowStatusTone.BRAND
    "info" -> DlaFlowStatusTone.INFO
    "success" -> DlaFlowStatusTone.SUCCESS
    "warning" -> DlaFlowStatusTone.WARNING
    "danger" -> DlaFlowStatusTone.DANGER
    else -> DlaFlowStatusTone.NEUTRAL
}

internal fun ordersToneColor(colors: DlaFlowComposeColors, tone: String): Color = when (tone.trim().lowercase(Locale.ROOT)) {
    "brand" -> colors.primary
    "info" -> colors.info
    "success" -> colors.success
    "warning" -> colors.orange
    "danger" -> colors.danger
    else -> colors.textMuted
}

internal fun ordersFulfillmentStatusColor(colors: DlaFlowComposeColors, order: OrdersListItem): Color =
    dlaFlowHexColor(order.statusColor) ?: ordersToneColor(colors, order.statusTone)

@Composable
private fun ordersQuickInfo(order: OrdersListItem): String {
    return order.shippingMethod.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.orders_value_products_short, order.itemCount.coerceAtLeast(1))
}

@Composable
private fun ordersBadgeSummary(order: OrdersListItem): String {
    val parts = mutableListOf<String>()
    if (order.badges.messages > 0) parts += stringResource(R.string.orders_value_messages_short, order.badges.messages)
    if (order.badges.shipments > 0) parts += stringResource(R.string.orders_value_shipments_short, order.badges.shipments)
    if (order.badges.documents > 0) parts += stringResource(R.string.orders_value_documents_short, order.badges.documents)
    if (parts.isEmpty()) {
        parts += ordersShortTime(order.createdAt).ifBlank { stringResource(R.string.orders_value_details) }
    }
    return parts.joinToString(" · ")
}

internal fun formatOrdersMoney(value: Double): String = NumberFormat.getCurrencyInstance(Locale("pl", "PL")).format(value)

private fun ordersShortTime(value: String, zone: ZoneId = ZoneId.systemDefault()): String = runCatching {
    OffsetDateTime.parse(value)
        .toInstant()
        .atZone(zone)
        .format(DateTimeFormatter.ofPattern("HH:mm", Locale("pl", "PL")))
}.getOrDefault("")

internal enum class OrdersDeadlineKind {
    UNAVAILABLE,
    OVERDUE,
    MINUTES,
    HOURS,
    DAYS,
}

internal data class OrdersDeadlinePresentation(
    val kind: OrdersDeadlineKind,
    val amount: Long?,
    val exact: String,
)

internal fun ordersDeadlinePresentation(
    value: String,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): OrdersDeadlinePresentation {
    val deadline = runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: return OrdersDeadlinePresentation(OrdersDeadlineKind.UNAVAILABLE, null, "")
    val remainingMinutes = Duration.between(now, deadline).toMinutes()
    val exact = deadline
        .atZone(zone)
        .format(DateTimeFormatter.ofPattern("dd.MM, HH:mm", Locale("pl", "PL")))
    return when {
        remainingMinutes < 0 -> OrdersDeadlinePresentation(OrdersDeadlineKind.OVERDUE, null, exact)
        remainingMinutes < 60 -> OrdersDeadlinePresentation(OrdersDeadlineKind.MINUTES, remainingMinutes, exact)
        remainingMinutes < 24 * 60 -> OrdersDeadlinePresentation(OrdersDeadlineKind.HOURS, remainingMinutes / 60, exact)
        else -> OrdersDeadlinePresentation(OrdersDeadlineKind.DAYS, remainingMinutes / (24 * 60), exact)
    }
}

@Composable
internal fun ordersShippingDeadlineLabel(value: String): String {
    val presentation = ordersDeadlinePresentation(value)
    return when (presentation.kind) {
        OrdersDeadlineKind.UNAVAILABLE -> stringResource(R.string.orders_deadline_unavailable)
        OrdersDeadlineKind.OVERDUE -> stringResource(R.string.orders_deadline_overdue, presentation.exact)
        OrdersDeadlineKind.MINUTES -> stringResource(
            R.string.orders_deadline_minutes,
            presentation.amount?.coerceAtLeast(0) ?: 0,
            presentation.exact,
        )
        OrdersDeadlineKind.HOURS -> stringResource(R.string.orders_deadline_hours, presentation.amount ?: 0, presentation.exact)
        OrdersDeadlineKind.DAYS -> stringResource(R.string.orders_deadline_days, presentation.amount ?: 0, presentation.exact)
    }
}

internal fun ordersDisplayTimestamp(value: String, zone: ZoneId = ZoneId.systemDefault()): String = runCatching {
    OffsetDateTime.parse(value)
        .toInstant()
        .atZone(zone)
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale("pl", "PL")))
}.getOrDefault("")

@Composable
internal fun ordersShipmentTimingValue(presentation: OrdersShipmentTimingPresentation): String = when (presentation.kind) {
    OrdersShipmentTimingKind.DEADLINE -> presentation.shippingDeadlineAt
        .takeIf { ordersDisplayTimestamp(it).isNotBlank() }
        ?.let { ordersShippingDeadlineLabel(it) }
        ?: stringResource(R.string.orders_shipment_date_missing)
    OrdersShipmentTimingKind.SHIPPED, OrdersShipmentTimingKind.DELIVERED ->
        ordersDisplayTimestamp(presentation.timestamp).ifBlank { stringResource(R.string.orders_shipment_date_missing) }
}

@Composable
private fun OrderTimingLine(colors: DlaFlowComposeColors, order: OrdersListItem) {
    val orderedAt = ordersDisplayTimestamp(order.createdAt)
    val shipmentTiming = ordersShipmentTimingPresentation(
        shipmentStage = order.shipmentStage,
        shippedAt = order.shippedAt,
        deliveredAt = order.deliveredAt,
        shippingDeadlineAt = order.shippingDeadlineAt,
    )
    val shipmentLabel = when (shipmentTiming.kind) {
        OrdersShipmentTimingKind.DEADLINE -> stringResource(R.string.orders_label_shipping_deadline)
        OrdersShipmentTimingKind.SHIPPED -> stringResource(R.string.orders_label_shipped_at)
        OrdersShipmentTimingKind.DELIVERED -> stringResource(R.string.orders_label_delivered_at)
    }
    val shipmentValue = ordersShipmentTimingValue(shipmentTiming)
    val shipmentColor = when (shipmentTiming.kind) {
        OrdersShipmentTimingKind.DEADLINE -> ordersShippingDeadlineColor(colors, order.shippingDeadlineAt)
        OrdersShipmentTimingKind.SHIPPED, OrdersShipmentTimingKind.DELIVERED -> ordersFulfillmentStatusColor(colors, order)
    }
    if (ordersUsesCompactTimingLayout()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            OrderTimelineEntry(
                colors = colors,
                label = stringResource(R.string.orders_label_ordered_at),
                value = orderedAt.ifBlank { stringResource(R.string.orders_value_missing) },
                tone = colors.textMuted,
                showConnector = false,
                showMarker = false,
                modifier = Modifier.weight(1f),
            )
            OrderTimelineEntry(
                colors = colors,
                label = shipmentLabel,
                value = shipmentValue,
                tone = shipmentColor,
                showConnector = false,
                showMarker = false,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            OrderTimelineEntry(
                colors = colors,
                label = stringResource(R.string.orders_label_ordered_at),
                value = orderedAt.ifBlank { stringResource(R.string.orders_value_missing) },
                tone = colors.textMuted,
                showConnector = true,
            )
            OrderTimelineEntry(
                colors = colors,
                label = shipmentLabel,
                value = shipmentValue,
                tone = shipmentColor,
                showConnector = false,
            )
        }
    }
}

@Composable
private fun OrderTimelineEntry(
    colors: DlaFlowComposeColors,
    label: String,
    value: String,
    tone: Color,
    showConnector: Boolean,
    showMarker: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        if (showMarker) {
            Column(
                modifier = Modifier.width(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(tone),
                )
                if (showConnector) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(colors.border),
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (showConnector) 5.dp else 0.dp),
        ) {
            Text(
                text = label,
                color = colors.textMuted,
                fontFamily = DlaFlowInter,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            )
            Text(
                text = value,
                color = tone,
                fontFamily = DlaFlowInter,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ordersUsesCompactTimingLayout(): Boolean {
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    return configuration.screenWidthDp > 360 &&
        !(configuration.screenWidthDp < 480 && fontScale >= 1.2f)
}

private fun ordersShippingDeadlineColor(colors: DlaFlowComposeColors, value: String): Color {
    return when (ordersDeadlinePresentation(value).kind) {
        OrdersDeadlineKind.OVERDUE -> colors.heroNegative
        OrdersDeadlineKind.MINUTES,
        OrdersDeadlineKind.HOURS,
        -> colors.orange
        OrdersDeadlineKind.UNAVAILABLE,
        OrdersDeadlineKind.DAYS,
        -> colors.textMuted
    }
}
