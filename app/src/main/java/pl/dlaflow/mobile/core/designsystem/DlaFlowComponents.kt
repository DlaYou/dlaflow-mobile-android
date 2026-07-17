package pl.dlaflow.mobile.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
internal fun DlaFlowScreenHeader(
    colors: DlaFlowComposeColors,
    title: String,
    subtitle: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = colors.textStrong,
            fontSize = 16.5.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
            lineHeight = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(DlaFlowDimensions.headerGap))
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 10.5.sp,
                fontFamily = DlaFlowInter,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun DlaFlowCard(
    colors: DlaFlowComposeColors,
    accent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(DlaFlowDimensions.controlRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(DlaFlowDimensions.borderWidth, if (accent) colors.primarySoftBorder else colors.border, shape)
            .padding(DlaFlowDimensions.cardPadding),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DlaFlowTextField(
    colors: DlaFlowComposeColors,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    placeholder: String? = null,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        isError = isError,
        label = { Text(label) },
        placeholder = placeholder?.let { text -> { Text(text) } },
        supportingText = supportingText?.let { text -> { Text(text) } },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(DlaFlowDimensions.controlRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.border,
            errorBorderColor = colors.danger,
            focusedLabelColor = colors.primary,
            cursorColor = colors.primary,
            focusedTextColor = colors.textStrong,
            unfocusedTextColor = colors.textStrong,
            focusedContainerColor = colors.surfaceSubtle,
            unfocusedContainerColor = colors.surfaceSubtle,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun DlaFlowPrimaryButton(
    colors: DlaFlowComposeColors,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(DlaFlowDimensions.controlRadius),
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
        modifier = modifier.height(DlaFlowDimensions.minimumTouchTarget),
    ) {
        DlaFlowIcon(icon, Color.White, modifier = Modifier.size(DlaFlowDimensions.smallIcon))
        Spacer(Modifier.width(DlaFlowDimensions.inlineGap))
        Text(text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun DlaFlowSecondaryButton(
    colors: DlaFlowComposeColors,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(DlaFlowDimensions.controlRadius),
        colors = ButtonDefaults.buttonColors(containerColor = colors.primarySoft, contentColor = colors.primary),
        modifier = modifier.height(DlaFlowDimensions.minimumTouchTarget),
    ) {
        DlaFlowIcon(icon, colors.primary, modifier = Modifier.size(DlaFlowDimensions.smallIcon))
        Spacer(Modifier.width(DlaFlowDimensions.inlineGap))
        Text(text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun DlaFlowIcon(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(DlaFlowDimensions.controlRadius))
            .background(color.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(DlaFlowDimensions.smallIcon),
        )
    }
}

@Composable
internal fun DlaFlowStatusBadge(colors: DlaFlowComposeColors, text: String) {
    val shape = RoundedCornerShape(DlaFlowDimensions.pillRadius)
    Text(
        text = text,
        color = colors.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(shape)
            .background(colors.primarySoft)
            .border(DlaFlowDimensions.borderWidth, colors.primarySoftBorder, shape)
            .padding(
                horizontal = DlaFlowDimensions.badgeHorizontalPadding,
                vertical = DlaFlowDimensions.badgeVerticalPadding,
            ),
    )
}

@Composable
internal fun DlaFlowStatusStrip(colors: DlaFlowComposeColors, message: String) {
    if (message.isBlank()) return

    val shape = RoundedCornerShape(DlaFlowDimensions.controlRadius)
    Text(
        text = message,
        color = colors.textMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 17.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceSubtle)
            .border(DlaFlowDimensions.borderWidth, colors.border, shape)
            .padding(DlaFlowDimensions.statusPadding),
    )
}

@Composable
internal fun DlaFlowKeyValue(colors: DlaFlowComposeColors, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = DlaFlowDimensions.rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = colors.textStrong,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun DlaFlowKpiTile(
    colors: DlaFlowComposeColors,
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 98.dp,
) {
    Column(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = 0.78f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 9.dp),
    ) {
        DlaFlowIcon(icon, iconColor, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, color = colors.textStrong, fontSize = 18.5.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = colors.textMuted, fontSize = 8.4.sp, fontWeight = FontWeight.SemiBold, lineHeight = 9.2.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun DlaFlowNotificationPreviewCard(
    colors: DlaFlowComposeColors,
    heading: String,
    openAllLabel: String,
    emptyTitle: String,
    emptySubtitle: String,
    isEmpty: Boolean,
    onOpenNotifications: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(heading, color = colors.textStrong, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { onOpenNotifications() }
                    .heightIn(min = DlaFlowDimensions.minimumTouchTarget)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(openAllLabel, color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (isEmpty) {
            DlaFlowNotificationEmptyRow(colors, emptyTitle, emptySubtitle)
        } else {
            content()
        }
    }
}

@Composable
internal fun DlaFlowPhotoTaskCard(
    colors: DlaFlowComposeColors,
    title: String,
    productName: String,
    skuText: String,
    photosLabel: String,
    photosProgress: String,
    mediaCount: Int,
    maxPhotos: Int,
    takePhotoLabel: String,
    pickPhotoLabel: String,
    completeTaskLabel: String,
    highlighted: Boolean,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onCompletePhotoTask: () -> Unit,
) {
    DlaFlowCard(colors, accent = highlighted) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                DlaFlowIcon(Icons.Rounded.PhotoCamera, colors.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    productName,
                    color = colors.textStrong,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (skuText.isNotBlank()) {
                    Text(skuText, color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        DlaFlowPhotoProgress(colors, photosLabel, photosProgress, mediaCount, maxPhotos)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DlaFlowPrimaryButton(
                colors,
                Icons.Rounded.PhotoCamera,
                takePhotoLabel,
                modifier = Modifier.weight(1f),
                onClick = onTakePhoto,
            )
            DlaFlowSecondaryButton(
                colors,
                Icons.Rounded.PhotoLibrary,
                pickPhotoLabel,
                modifier = Modifier.weight(1f),
                onClick = onPickPhoto,
            )
        }
        Spacer(Modifier.height(10.dp))
        DlaFlowSecondaryButton(
            colors,
            Icons.Rounded.CheckCircle,
            completeTaskLabel,
            onClick = onCompletePhotoTask,
        )
    }
}

@Composable
private fun DlaFlowPhotoProgress(
    colors: DlaFlowComposeColors,
    label: String,
    progressText: String,
    current: Int,
    max: Int,
) {
    val safeMax = max.coerceAtLeast(1)
    val ratio = current.coerceIn(0, safeMax).toFloat() / safeMax.toFloat()
    Column {
        Row {
            Text(label, color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(progressText, color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.borderSubtle),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.primary),
            )
        }
    }
}

@Composable
internal fun DlaFlowNotificationRow(
    colors: DlaFlowComposeColors,
    title: String,
    description: String,
    tone: String,
    occurredLabel: String,
) {
    val color = notificationPresentationToneColor(colors, tone)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        DlaFlowNotificationToneIcon(notificationPresentationIcon(title, description), color)
        Spacer(Modifier.width(11.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 1.dp),
        ) {
            Text(
                title,
                color = colors.textStrong,
                fontSize = 10.4.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                description,
                color = colors.textMuted,
                fontSize = 8.3.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 10.3.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .height(42.dp)
                .padding(top = 1.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(6.5.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                occurredLabel,
                color = colors.textMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun DlaFlowNotificationEmptyRow(
    colors: DlaFlowComposeColors,
    title: String,
    subtitle: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DlaFlowIcon(Icons.Rounded.CheckCircle, colors.success, modifier = Modifier.size(38.dp))
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textStrong, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = colors.textMuted, fontSize = 11.sp, maxLines = 2, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun DlaFlowNotificationToneIcon(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.5.dp),
        )
    }
}

private fun notificationPresentationIcon(title: String, description: String): ImageVector {
    val text = "$title $description".lowercase(Locale.ROOT)
    return when {
        "nowe zamówienie" in text || "nowe zamowienie" in text -> Icons.Rounded.ShoppingCart
        "wiadomo" in text || "klient" in text -> Icons.Rounded.ChatBubbleOutline
        "wysy" in text || "pacz" in text -> Icons.Rounded.LocalShipping
        "problem" in text || "błąd" in text || "blad" in text -> Icons.Rounded.Warning
        else -> Icons.Rounded.ShoppingCart
    }
}

private fun notificationPresentationToneColor(colors: DlaFlowComposeColors, tone: String): Color {
    return when (tone.lowercase(Locale.ROOT)) {
        "error" -> colors.danger
        "success" -> colors.success
        "warning" -> colors.warning
        else -> colors.primary
    }
}
