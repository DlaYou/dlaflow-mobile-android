package pl.dlaflow.mobile.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class DlaFlowStatusTone {
    NEUTRAL,
    BRAND,
    INFO,
    SUCCESS,
    WARNING,
    DANGER,
}

@Composable
internal fun DlaFlowStatusField(
    colors: DlaFlowComposeColors,
    label: String,
    value: String,
    tone: DlaFlowStatusTone,
    accentColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val toneColor = accentColor ?: tone.color(colors)
    val shape = RoundedCornerShape(DlaFlowDimensions.controlRadius)
    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(shape)
            .background(colors.surfaceSubtle)
            .border(DlaFlowDimensions.borderWidth, toneColor.copy(alpha = 0.24f), shape)
            .semantics(mergeDescendants = true) { contentDescription = "$label, $value" }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            color = colors.textMuted,
            fontFamily = DlaFlowInter,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            maxLines = 1,
        )
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            color = toneColor,
            fontFamily = DlaFlowInter,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun dlaFlowHexColor(value: String): Color? {
    val normalized = value.trim()
    if (!normalized.matches(Regex("^#[0-9a-fA-F]{6}$"))) return null
    val rgb = normalized.drop(1).toLongOrNull(16) ?: return null
    return Color(0xff000000L or rgb)
}

private fun DlaFlowStatusTone.color(colors: DlaFlowComposeColors): Color = when (this) {
    DlaFlowStatusTone.NEUTRAL -> colors.textMuted
    DlaFlowStatusTone.BRAND -> colors.primary
    DlaFlowStatusTone.INFO -> colors.info
    DlaFlowStatusTone.SUCCESS -> colors.success
    DlaFlowStatusTone.WARNING -> colors.orange
    DlaFlowStatusTone.DANGER -> colors.danger
}
