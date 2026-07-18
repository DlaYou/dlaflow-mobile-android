package pl.dlaflow.mobile.feature.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowFilterChip
import pl.dlaflow.mobile.core.designsystem.DlaFlowIcon
import pl.dlaflow.mobile.core.designsystem.DlaFlowScreenHeader
import pl.dlaflow.mobile.core.designsystem.DlaFlowSearchField
import pl.dlaflow.mobile.core.designsystem.DlaFlowSecondaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowSkeletonBlock
import pl.dlaflow.mobile.core.designsystem.DlaFlowStateCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnail
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnailLoader
import pl.dlaflow.mobile.core.state.DlaFlowUiState

@Composable
internal fun OrdersFeatureScreen(
    colors: DlaFlowComposeColors,
    state: OrdersUiState,
    thumbnailLoader: DlaFlowThumbnailLoader,
    leadContent: @Composable () -> Unit,
    onAction: (OrdersAction) -> Unit,
) {
    val content = state.listContentOrNull()
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
        return
    }

    if (state.listState == DlaFlowUiState.NoAccess) {
        DlaFlowStateCard(
            colors = colors,
            icon = Icons.Rounded.Warning,
            iconColor = colors.danger,
            title = stringResource(R.string.orders_no_access_title),
            description = stringResource(R.string.orders_no_access_description),
        )
        return
    }

    leadContent()

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

    if (state.isRefreshing && content != null) {
        DlaFlowStateCard(
            colors = colors,
            icon = Icons.Rounded.Refresh,
            iconColor = colors.primary,
            title = stringResource(R.string.orders_refreshing_title),
            description = stringResource(R.string.orders_refreshing_description),
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

@Composable
private fun OrdersFilterChips(
    colors: DlaFlowComposeColors,
    selected: OrdersFilter,
    onFilterChange: (OrdersFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OrdersFilter.entries.chunked(3).forEach { row ->
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
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

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
    val statusColor = ordersToneColor(colors, order.statusTone)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        DlaFlowCard(colors, accent = order.statusTone.equals("warning", ignoreCase = true)) {
            Row(verticalAlignment = Alignment.Top) {
                if (order.thumbnailUrl.isNotBlank()) {
                    DlaFlowThumbnail(colors, order.thumbnailUrl, thumbnailLoader)
                } else {
                    DlaFlowIcon(ordersIcon(order), statusColor, modifier = Modifier.size(38.dp))
                }
                Spacer(Modifier.width(10.dp))
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
                    Spacer(Modifier.height(5.dp))
                    Text(
                        order.productSummary.ifBlank { stringResource(R.string.orders_value_products_count, order.itemCount) },
                        color = colors.text,
                        fontSize = 10.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 13.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OrdersTinyPill(ordersStatusLabel(order.status), statusColor)
                        OrdersTinyPill(order.paymentStatus.ifBlank { stringResource(R.string.orders_value_payment) }, ordersToneColor(colors, order.paymentTone))
                    }
                    Spacer(Modifier.height(7.dp))
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
private fun OrdersTinyPill(text: String, tone: Color) {
    Text(
        text = text,
        color = tone,
        fontSize = 8.8.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
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
        OrdersFilter.PROBLEMS -> R.string.orders_filter_problems
        OrdersFilter.MESSAGES -> R.string.orders_filter_messages
    },
)

@Composable
internal fun ordersStatusLabel(status: String): String {
    val trimmed = status.trim()
    val labelRes = when (trimmed.lowercase(Locale.ROOT)) {
        "" -> R.string.orders_status_missing
        "nowe", "new" -> R.string.orders_status_new
        "do wysyłki", "do wysylki", "to_ship", "to-ship" -> R.string.orders_status_to_ship
        "w realizacji", "processing" -> R.string.orders_status_processing
        "dostarczone", "delivered" -> R.string.orders_status_delivered
        "zakończone", "zakonczone", "finished", "completed" -> R.string.orders_status_finished
        else -> null
    }
    return labelRes?.let { stringResource(it) }
        ?: trimmed.replaceFirstChar { it.titlecase(Locale.getDefault()) }
}

private fun ordersToneColor(colors: DlaFlowComposeColors, tone: String): Color = when (tone.trim().lowercase(Locale.ROOT)) {
    "brand" -> colors.primary
    "info" -> colors.info
    "success" -> colors.success
    "warning" -> colors.orange
    else -> colors.textMuted
}

private fun ordersIcon(order: OrdersListItem): ImageVector = when {
    order.statusTone.equals("warning", ignoreCase = true) -> Icons.Rounded.Warning
    order.badges.messages > 0 -> Icons.Rounded.ChatBubbleOutline
    order.badges.shipments > 0 -> Icons.Rounded.LocalShipping
    else -> Icons.AutoMirrored.Rounded.ReceiptLong
}

@Composable
private fun ordersQuickInfo(order: OrdersListItem): String {
    val parts = mutableListOf<String>()
    if (order.shippingMethod.isNotBlank()) parts += order.shippingMethod
    if (order.phone.isNotBlank()) parts += stringResource(R.string.orders_value_phone_short, order.phone)
    if (parts.isEmpty()) {
        parts += stringResource(R.string.orders_value_products_short, order.itemCount.coerceAtLeast(1))
    }
    return parts.joinToString(" · ")
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

private fun ordersShortTime(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("")
