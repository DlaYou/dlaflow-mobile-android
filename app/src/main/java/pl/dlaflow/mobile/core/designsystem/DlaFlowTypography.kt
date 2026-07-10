package pl.dlaflow.mobile.core.designsystem

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pl.dlaflow.mobile.R

@OptIn(ExperimentalTextApi::class)
private fun dlaFlowInterFont(weight: FontWeight, axisWeight: Int): Font = Font(
    resId = R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(axisWeight)),
)

internal val DlaFlowInter = FontFamily(
    dlaFlowInterFont(FontWeight.Normal, 400),
    dlaFlowInterFont(FontWeight.Medium, 500),
    dlaFlowInterFont(FontWeight.SemiBold, 600),
    dlaFlowInterFont(FontWeight.Bold, 700),
    dlaFlowInterFont(FontWeight.ExtraBold, 800),
)

internal fun TextStyle.withDlaFlowTypography(): TextStyle = copy(
    fontFamily = DlaFlowInter,
    letterSpacing = 0.sp,
)
