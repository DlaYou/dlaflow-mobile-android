package pl.dlaflow.mobile.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

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
