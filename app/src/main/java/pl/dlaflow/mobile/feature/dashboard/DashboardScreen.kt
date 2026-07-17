package pl.dlaflow.mobile.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sqrt
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowIcon
import pl.dlaflow.mobile.core.designsystem.DlaFlowInter
import pl.dlaflow.mobile.core.designsystem.DlaFlowKpiTile
import pl.dlaflow.mobile.core.designsystem.DlaFlowNotificationPreviewCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowNotificationRow
import pl.dlaflow.mobile.core.designsystem.DlaFlowPhotoTaskCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowScreenHeader

internal fun dashboardQuickAction(index: Int): DashboardAction = when (index) {
    0 -> DashboardAction.ScanPackage
    1 -> DashboardAction.OpenProductWork
    2 -> DashboardAction.OpenStatistics
    3 -> DashboardAction.OpenProducts
    else -> error("Unsupported dashboard quick action index: $index")
}

@Composable
internal fun DashboardFeatureScreen(
    colors: DlaFlowComposeColors,
    sessionUserName: String,
    state: DashboardUiState,
    fallbackPhotoTask: DashboardPhotoTask?,
    onAction: (DashboardAction) -> Unit,
) {
    val content = state.contentOrNull()
    GreetingRow(colors, content?.userName ?: sessionUserName) { onAction(DashboardAction.Refresh) }
    RevenueCard(colors, content)
    KpiGrid(colors, content?.kpis)
    NotificationsList(colors, content?.notifications.orEmpty()) { onAction(DashboardAction.OpenNotifications) }
    QuickActions(colors, onAction)
    ActivePhotoTaskSection(colors, content?.activePhotoTask, fallbackPhotoTask, onAction)
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun GreetingRow(colors: DlaFlowComposeColors, userName: String, onRefresh: () -> Unit) {
    val firstName = displayFirstName(
        value = userName,
        displayFallback = stringResource(R.string.dashboard_name_fallback_display),
        firstNameFallback = stringResource(R.string.dashboard_name_fallback_first),
    )
    DlaFlowScreenHeader(
        colors = colors,
        title = stringResource(R.string.dashboard_greeting, firstName),
        subtitle = stringResource(R.string.dashboard_greeting_subtitle),
    )
}

@Composable
private fun RevenueCard(colors: DlaFlowComposeColors, content: DashboardContent?) {
    val changePercent = content?.revenueChangePercent ?: 0.0
    val positive = changePercent >= 0.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(colors.primaryDeep, colors.primary, colors.primaryGlow),
                    start = Offset(0f, 0f),
                    end = Offset(760f, 440f),
                ),
            )
            .padding(horizontal = 17.dp, vertical = 13.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.62f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_revenue_today),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 10.5.sp,
                fontFamily = DlaFlowInter,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatMoney(content?.todayRevenue ?: 0.0),
                color = Color.White,
                fontSize = 21.5.sp,
                fontFamily = DlaFlowInter,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (positive) {
                        stringResource(R.string.dashboard_revenue_change_up, String.format(Locale.US, "%.1f", changePercent))
                    } else {
                        stringResource(
                            R.string.dashboard_revenue_change_down,
                            String.format(Locale.US, "%.1f", kotlin.math.abs(changePercent)),
                        )
                    },
                    color = if (positive) colors.heroPositive else colors.heroNegative,
                    fontSize = 8.5.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 11.sp,
                    maxLines = 1,
                )
                Text(
                    text = if (positive) {
                        stringResource(R.string.dashboard_revenue_more_than_yesterday)
                    } else {
                        stringResource(R.string.dashboard_revenue_less_than_yesterday)
                    },
                    color = Color.White.copy(alpha = 0.84f),
                    fontSize = 8.5.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(76.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.13f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.dashboard_revenue_details),
                color = Color.White,
                fontSize = 8.5.sp,
                fontFamily = DlaFlowInter,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.width(1.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
        }
        RevenueTrendChart(
            colors = colors,
            trend = content?.trend.orEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(154.dp)
                .height(44.dp)
                .padding(end = 12.dp, bottom = 4.dp),
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun RevenueTrendChart(
    colors: DlaFlowComposeColors,
    trend: List<DashboardTrendPoint>,
    modifier: Modifier = Modifier,
) {
    val points = revenueSparklinePoints(trend)

    Canvas(modifier = modifier) {
        val topInset = 3.dp.toPx()
        val bottomInset = 7.dp.toPx()
        val drawingHeight = (size.height - topInset - bottomInset).coerceAtLeast(1f)
        val baselineY = size.height - 3.dp.toPx()
        drawLine(
            color = Color.White.copy(alpha = 0.16f),
            start = Offset(0f, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 1.dp.toPx(),
        )
        if (points.isEmpty()) {
            return@Canvas
        }
        val step = size.width / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = if (points.size == 1) size.width else step * index
            val y = topInset + drawingHeight - (drawingHeight * value.toFloat())
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val previousX = step * (index - 1)
                val previousY = topInset + drawingHeight - (drawingHeight * points[index - 1].toFloat())
                val controlOffset = step * 0.45f
                path.cubicTo(previousX + controlOffset, previousY, x - controlOffset, y, x, y)
            }
        }
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.9f),
            style = Stroke(width = 1.55.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(
            color = Color.White,
            radius = 2.35.dp.toPx(),
            center = Offset(
                if (points.size == 1) size.width else step * (points.size - 1),
                topInset + drawingHeight - (drawingHeight * points.last().toFloat()),
            ),
        )
    }
}

private fun revenueSparklinePoints(trend: List<DashboardTrendPoint>): List<Double> {
    val values = trend.takeLast(14).map { it.revenue.coerceAtLeast(0.0) }

    if (values.isEmpty() || values.all { it == 0.0 }) {
        return emptyList()
    }

    val min = values.minOrNull() ?: return emptyList()
    val max = values.maxOrNull() ?: return emptyList()
    if (max <= min) {
        return values.map { 0.56 }
    }

    return values.map { value ->
        val normalized = ((value - min) / (max - min)).coerceIn(0.0, 1.0)
        (0.18 + sqrt(normalized) * 0.56).coerceIn(0.18, 0.74)
    }
}

@Composable
private fun KpiGrid(colors: DlaFlowComposeColors, kpis: DashboardKpis?) {
    val visibleKpis = kpis ?: DashboardKpis(0, 0, 0, 0)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        KpiTile(colors, stringResource(R.string.dashboard_kpi_new_orders), visibleKpis.newOrders.toString(), Icons.Rounded.ShoppingCart, colors.primary, Modifier.weight(1f))
        KpiTile(colors, stringResource(R.string.dashboard_kpi_to_ship), visibleKpis.toShip.toString(), Icons.Rounded.LocalShipping, colors.orange, Modifier.weight(1f))
        KpiTile(colors, stringResource(R.string.dashboard_kpi_overdue), visibleKpis.overdueOrProblems.toString(), Icons.Rounded.Inventory2, colors.success, Modifier.weight(1f))
        KpiTile(colors, stringResource(R.string.dashboard_kpi_messages), visibleKpis.messages.toString(), Icons.Rounded.ChatBubbleOutline, colors.info, Modifier.weight(1f))
    }
}

@Composable
private fun KpiTile(
    colors: DlaFlowComposeColors,
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
) {
    DlaFlowKpiTile(colors, label, value, icon, iconColor, modifier)
}

@Composable
private fun QuickActions(colors: DlaFlowComposeColors, onAction: (DashboardAction) -> Unit) {
    Text(
        text = stringResource(R.string.dashboard_quick_actions),
        color = colors.textStrong,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 15.5.sp,
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        QuickActionButton(colors, stringResource(R.string.dashboard_quick_scan), stringResource(R.string.dashboard_quick_scan_subtitle), Icons.Rounded.QrCodeScanner, colors.primary, Modifier.weight(1f)) { onAction(dashboardQuickAction(0)) }
        QuickActionButton(colors, stringResource(R.string.dashboard_quick_product_work), stringResource(R.string.dashboard_quick_product_work_subtitle), Icons.Rounded.AddBox, colors.success, Modifier.weight(1f)) { onAction(dashboardQuickAction(1)) }
        QuickActionButton(colors, stringResource(R.string.dashboard_quick_statistics), stringResource(R.string.dashboard_quick_statistics_subtitle), Icons.AutoMirrored.Rounded.ShowChart, colors.orange, Modifier.weight(1f)) { onAction(dashboardQuickAction(2)) }
        QuickActionButton(colors, stringResource(R.string.dashboard_quick_products), stringResource(R.string.dashboard_quick_products_subtitle), Icons.Rounded.Inventory2, colors.info, Modifier.weight(1f)) { onAction(dashboardQuickAction(3)) }
    }
}

@Composable
private fun QuickActionButton(
    colors: DlaFlowComposeColors,
    label: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.surface,
            contentColor = colors.textStrong,
        ),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.76f)),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.height(84.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DlaFlowIcon(icon, iconColor, modifier = Modifier.size(37.dp))
            Spacer(Modifier.height(7.dp))
            Text(label, color = colors.textStrong, fontSize = 8.2.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 9.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = colors.textMuted, fontSize = 7.4.sp, fontWeight = FontWeight.SemiBold, lineHeight = 8.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NotificationsList(
    colors: DlaFlowComposeColors,
    notifications: List<DashboardNotification>,
    onOpenNotifications: () -> Unit,
) {
    DlaFlowNotificationPreviewCard(
        colors = colors,
        heading = stringResource(R.string.dashboard_notifications_heading),
        openAllLabel = stringResource(R.string.dashboard_notifications_open_all),
        emptyTitle = stringResource(R.string.dashboard_notifications_empty_title),
        emptySubtitle = stringResource(R.string.dashboard_notifications_empty_subtitle),
        isEmpty = notifications.isEmpty(),
        onOpenNotifications = onOpenNotifications,
    ) {
        Column {
            notifications.take(4).forEachIndexed { index, notification ->
                DlaFlowNotificationRow(
                    colors = colors,
                    title = notification.title,
                    description = notification.description,
                    tone = notification.tone,
                    occurredLabel = relativeTime(notification.occurredAt),
                )
                if (index < notifications.take(4).lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp)
                            .height(1.dp)
                            .background(colors.borderSubtle),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePhotoTaskSection(
    colors: DlaFlowComposeColors,
    activePhotoTask: DashboardPhotoTask?,
    fallbackPhotoTask: DashboardPhotoTask?,
    onAction: (DashboardAction) -> Unit,
) {
    val task = activePhotoTask ?: fallbackPhotoTask ?: return
    DlaFlowPhotoTaskCard(
        colors = colors,
        title = stringResource(R.string.dashboard_photo_task_title),
        productName = task.productName,
        skuText = if (task.productSku.isBlank()) "" else stringResource(R.string.dashboard_photo_task_sku, task.productSku),
        photosLabel = stringResource(R.string.dashboard_photo_task_photos),
        photosProgress = stringResource(R.string.dashboard_photo_task_photo_count, task.mediaCount, task.maxPhotos),
        mediaCount = task.mediaCount,
        maxPhotos = task.maxPhotos,
        takePhotoLabel = stringResource(R.string.dashboard_photo_task_take),
        pickPhotoLabel = stringResource(R.string.dashboard_photo_task_pick),
        completeTaskLabel = stringResource(R.string.dashboard_photo_task_complete),
        highlighted = true,
        onTakePhoto = { onAction(DashboardAction.TakePhoto(task.id)) },
        onPickPhoto = { onAction(DashboardAction.PickPhoto(task.id)) },
        onCompletePhotoTask = { onAction(DashboardAction.CompletePhotoTask(task.id)) },
    )
}

private fun displayFirstName(
    value: String,
    displayFallback: String,
    firstNameFallback: String,
): String {
    val clean = value.trim()
    val name = if (clean.isBlank()) {
        displayFallback
    } else {
        clean.substringBefore("@").replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }
    return name.substringBefore(" ").ifBlank { firstNameFallback }
}

private fun formatMoney(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pl", "PL")).format(value)
}

private fun shortTime(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault("")
}

@Composable
private fun relativeTime(value: String): String {
    val minutes = runCatching {
        Duration.between(OffsetDateTime.parse(value), OffsetDateTime.now()).toMinutes().coerceAtLeast(0)
    }.getOrNull()

    return when {
        minutes == null -> shortTime(value)
        minutes < 1 -> stringResource(R.string.dashboard_time_now)
        minutes < 60 -> stringResource(R.string.dashboard_time_minutes_ago, minutes)
        minutes < 24 * 60 -> stringResource(R.string.dashboard_time_hours_ago, minutes / 60)
        else -> stringResource(R.string.dashboard_time_days_ago, minutes / (24 * 60))
    }
}
