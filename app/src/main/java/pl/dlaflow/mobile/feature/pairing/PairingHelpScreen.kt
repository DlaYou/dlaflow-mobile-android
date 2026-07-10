package pl.dlaflow.mobile.feature.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowIcon
import pl.dlaflow.mobile.core.designsystem.DlaFlowInter

@Composable
internal fun PairingHelpScreen(
    colors: DlaFlowComposeColors,
    appVersionName: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.pairing_back),
                    color = colors.primary,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
        Image(
            painter = painterResource(if (colors.dark) R.drawable.dlaflow_logo_dark else R.drawable.dlaflow_logo_light),
            contentDescription = stringResource(R.string.pairing_logo_content_description),
            modifier = Modifier
                .height(34.dp)
                .width(136.dp)
                .align(Alignment.CenterHorizontally),
        )
        Text(
            text = stringResource(R.string.pairing_help_title),
            color = colors.textStrong,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.pairing_help_description),
            color = colors.textMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        PairingHelpPathCard(colors)
        PairingHelpStepCard(
            colors = colors,
            step = "1",
            title = stringResource(R.string.pairing_help_step_1_title),
            description = stringResource(R.string.pairing_help_step_1_description),
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_login,
                darkRes = R.drawable.help_pairing_panel_login_dark,
                height = 212.dp,
            )
        }
        PairingHelpStepCard(
            colors = colors,
            step = "2",
            title = stringResource(R.string.pairing_help_step_2_title),
            description = stringResource(R.string.pairing_help_step_2_description),
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_integracje,
                darkRes = R.drawable.help_pairing_panel_integracje_dark,
                height = 178.dp,
            )
        }
        PairingHelpStepCard(
            colors = colors,
            step = "3",
            title = stringResource(R.string.pairing_help_step_3_title),
            description = stringResource(R.string.pairing_help_step_3_description),
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_wtyczki,
                darkRes = R.drawable.help_pairing_panel_wtyczki_dark,
                height = 156.dp,
            )
        }
        PairingHelpStepCard(
            colors = colors,
            step = "4",
            title = stringResource(R.string.pairing_help_step_4_title),
            description = stringResource(R.string.pairing_help_step_4_description),
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_mobile_card,
                darkRes = R.drawable.help_pairing_panel_mobile_card_dark,
                height = 166.dp,
            )
        }
        PairingHelpStepCard(
            colors = colors,
            step = "5",
            title = stringResource(R.string.pairing_help_step_5_title),
            description = stringResource(R.string.pairing_help_step_5_description),
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_pair_button,
                darkRes = R.drawable.help_pairing_panel_pair_button_dark,
                height = 188.dp,
            )
        }
        PairingHelpStepCard(
            colors = colors,
            step = "6",
            title = stringResource(R.string.pairing_help_step_6_title),
            description = stringResource(R.string.pairing_help_step_6_description),
        ) {
            HelpPanelScreenshot(
                colors = colors,
                lightRes = R.drawable.help_pairing_panel_pairing_qr,
                darkRes = R.drawable.help_pairing_panel_pairing_qr_dark,
                height = 212.dp,
            )
        }
        Text(
            text = stringResource(R.string.pairing_version, appVersionName),
            color = colors.textMuted,
            fontSize = 11.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PairingHelpPathCard(colors: DlaFlowComposeColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.primarySoft)
            .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stringResource(R.string.pairing_help_path_title),
            color = colors.primary,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            stringResource(R.string.pairing_help_path),
            color = colors.textStrong,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PairingHelpStepCard(
    colors: DlaFlowComposeColors,
    step: String,
    title: String,
    description: String,
    graphic: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    step,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.textStrong,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        graphic()
    }
}

@Composable
private fun HelpPanelScreenshot(
    colors: DlaFlowComposeColors,
    lightRes: Int,
    darkRes: Int,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (colors.dark) darkRes else lightRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
private fun HelpGraphicFrame(colors: DlaFlowComposeColors, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        content()
    }
}

@Composable
private fun HelpLoginGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            HelpMiniTopBar(colors, stringResource(R.string.pairing_help_mock_panel_url))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            ) {
                Text(stringResource(R.string.pairing_help_mock_dlaflow), color = colors.primary, fontSize = 13.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold)
                HelpClickButton(colors, stringResource(R.string.pairing_help_mock_login), Modifier.align(Alignment.BottomEnd))
            }
        }
    }
}

@Composable
private fun HelpSidebarGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(96.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HelpMenuItem(colors, stringResource(R.string.pairing_help_mock_dashboard), false)
                HelpMenuItem(colors, stringResource(R.string.pairing_help_mock_orders), false)
                HelpMenuItem(colors, stringResource(R.string.pairing_help_mock_products), false)
                HelpMenuItem(colors, stringResource(R.string.pairing_help_mock_integrations), true)
            }
            HelpPanelBlank(colors, stringResource(R.string.pairing_help_mock_click_integrations))
        }
    }
}

@Composable
private fun HelpPluginCategoryGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HelpMiniTopBar(colors, stringResource(R.string.pairing_help_mock_integrations))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                HelpChip(colors, stringResource(R.string.pairing_help_mock_marketplaces), false, Modifier.weight(1f))
                HelpChip(colors, stringResource(R.string.pairing_help_mock_carriers), false, Modifier.weight(1f))
                HelpChip(colors, stringResource(R.string.pairing_help_mock_plugins), true, Modifier.weight(1f))
            }
            HelpPanelBlank(colors, stringResource(R.string.pairing_help_mock_choose_plugins))
        }
    }
}

@Composable
private fun HelpPluginCardGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) {
            DlaFlowIcon(Icons.Rounded.PhoneAndroid, colors.primary, modifier = Modifier.size(34.dp))
            Column(modifier = Modifier.padding(start = 44.dp, end = 96.dp)) {
                Text(stringResource(R.string.pairing_help_mock_mobile_assistant), color = colors.textStrong, fontSize = 12.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.pairing_help_mock_phone), color = colors.textMuted, fontSize = 10.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Medium)
            }
            HelpClickButton(colors, stringResource(R.string.pairing_help_mock_configure), Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun HelpPairingButtonGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            ) {
                Text(stringResource(R.string.pairing_help_mock_connect_phone), color = colors.textMuted, fontSize = 10.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.pairing_help_mock_code_after_click), color = colors.textStrong, fontSize = 11.sp, lineHeight = 14.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                HelpClickButton(colors, stringResource(R.string.pairing_help_mock_pair_phone), Modifier.align(Alignment.BottomCenter).fillMaxWidth())
            }
            Box(
                modifier = Modifier
                    .width(82.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primarySoft)
                    .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.pairing_help_mock_mobile_assistant_short), color = colors.primary, fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun HelpCodeGraphic(colors: DlaFlowComposeColors) {
    HelpGraphicFrame(colors) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                PairingQrPreview(colors.copyForQrPreview())
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.pairing_help_mock_scan_qr), color = colors.textStrong, fontSize = 12.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.pairing_help_mock_emergency_code), color = colors.textMuted, fontSize = 10.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.pairing_help_mock_code), color = colors.primary, fontSize = 21.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
    }
}

private fun DlaFlowComposeColors.copyForQrPreview(): DlaFlowComposeColors = copy(dark = false)

@Composable
private fun HelpMiniTopBar(colors: DlaFlowComposeColors, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, color = colors.textStrong, fontSize = 11.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HelpMenuItem(colors: DlaFlowComposeColors, text: String, active: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) colors.primary else colors.surfaceSubtle)
            .border(1.dp, if (active) colors.primary else colors.border, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, color = if (active) Color.White else colors.textMuted, fontSize = 9.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HelpChip(colors: DlaFlowComposeColors, text: String, active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (active) colors.primary else colors.surface)
            .border(1.dp, if (active) colors.primary else colors.border, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (active) Color.White else colors.textMuted, fontSize = 10.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun HelpPanelBlank(colors: DlaFlowComposeColors, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.primary, fontSize = 12.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun HelpClickButton(colors: DlaFlowComposeColors, text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(31.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(colors.primary)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 10.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
