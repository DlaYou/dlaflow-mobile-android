package pl.dlaflow.mobile.feature.pairing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowIcon
import pl.dlaflow.mobile.core.designsystem.DlaFlowInter
import pl.dlaflow.mobile.core.designsystem.DlaFlowPrimaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusStrip
import pl.dlaflow.mobile.core.designsystem.DlaFlowTextField

@Composable
internal fun PairingFeatureScreen(
    colors: DlaFlowComposeColors,
    state: PairingUiState,
    appVersionName: String,
    onCodeChange: (String) -> Unit,
    onContinue: () -> Unit,
    onScanQr: () -> Unit,
    onDeviceNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onShowHelp: () -> Unit,
    onBack: () -> Unit,
) {
    val localMessage = state.localFeedback?.let { stringResource(it.messageRes()) }
    val sharedMessage = state.sharedMessage?.let { stringResource(it.descriptionRes) }
    when (state.step) {
        PairingStep.CODE -> PairingCodeScreen(
            colors = colors,
            pairingCode = state.codeInput,
            appVersionName = appVersionName,
            statusMessage = localMessage ?: sharedMessage.orEmpty(),
            onPairingCodeChange = onCodeChange,
            onContinue = onContinue,
            onScanPairingQr = onScanQr,
            onShowPairingHelp = onShowHelp,
        )
        PairingStep.NAME -> PairingDeviceNameScreen(
            colors = colors,
            state = state,
            onDeviceNameChange = onDeviceNameChange,
            onSubmit = onSubmit,
            onBack = onBack,
        )
        PairingStep.HELP -> PairingHelpScreen(
            colors = colors,
            appVersionName = appVersionName,
            onBack = onBack,
        )
    }
}

@Composable
private fun PairingDeviceNameScreen(
    colors: DlaFlowComposeColors,
    state: PairingUiState,
    onDeviceNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val localMessage = state.localFeedback?.let { stringResource(it.messageRes()) }
    val sharedMessage = state.sharedMessage?.let { stringResource(it.descriptionRes) }
    val message = localMessage ?: sharedMessage
    val nameValid = pairingDeviceNameError(state.deviceNameInput) == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        TextButton(enabled = !state.isSubmitting, onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
            Text(stringResource(R.string.pairing_back))
        }
        Text(
            text = stringResource(R.string.pairing_name_title),
            color = colors.textStrong,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = stringResource(R.string.pairing_name_description),
            color = colors.textMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Medium,
        )
        DlaFlowCard(colors) {
            DlaFlowTextField(
                colors = colors,
                label = stringResource(R.string.pairing_name_label),
                value = state.deviceNameInput,
                onValueChange = onDeviceNameChange,
                enabled = !state.isSubmitting,
                isError = state.localFeedback in setOf(
                    PairingFeedback.DEVICE_NAME_REQUIRED,
                    PairingFeedback.DEVICE_NAME_TOO_SHORT,
                    PairingFeedback.DEVICE_NAME_TOO_LONG,
                    PairingFeedback.DEVICE_NAME_INVALID,
                ),
                placeholder = stringResource(R.string.pairing_name_placeholder),
                supportingText = localMessage ?: stringResource(R.string.pairing_name_hint),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (nameValid && !state.isSubmitting) onSubmit() },
                ),
            )
        }
        DlaFlowPrimaryButton(
            colors = colors,
            icon = Icons.Rounded.Link,
            text = stringResource(if (state.isSubmitting) R.string.pairing_connecting else R.string.pairing_connect),
            modifier = Modifier.fillMaxWidth(),
            enabled = nameValid && !state.isSubmitting,
            onClick = onSubmit,
        )
        if (!message.isNullOrBlank() && localMessage == null) {
            DlaFlowStatusStrip(colors, message)
        }
    }
}

@Composable
private fun PairingCodeScreen(
    colors: DlaFlowComposeColors,
    pairingCode: String,
    appVersionName: String,
    statusMessage: String,
    onPairingCodeChange: (String) -> Unit,
    onContinue: () -> Unit,
    onScanPairingQr: () -> Unit,
    onShowPairingHelp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Image(
            painter = painterResource(if (colors.dark) R.drawable.dlaflow_logo_dark else R.drawable.dlaflow_logo_light),
            contentDescription = stringResource(R.string.pairing_logo_content_description),
            modifier = Modifier
                .height(42.dp)
                .width(168.dp),
        )
        Text(
            text = stringResource(R.string.pairing_title),
            color = colors.textStrong,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 27.sp,
            fontFamily = DlaFlowInter,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.pairing_description),
            color = colors.textMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        PairingQrCard(colors = colors, onScanPairingQr = onScanPairingQr)
        PairingDivider(colors)
        PairingManualCodeCard(
            colors = colors,
            pairingCode = pairingCode,
            onPairingCodeChange = onPairingCodeChange,
            onContinue = onContinue,
        )
        PairingHelpLink(colors, onShowPairingHelp)
        Text(
            text = stringResource(R.string.pairing_version, appVersionName),
            color = colors.textMuted,
            fontSize = 11.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Medium,
        )
        if (statusMessage.isNotBlank()) {
            DlaFlowStatusStrip(colors, statusMessage)
        }
    }
}

@Composable
private fun PairingQrCard(colors: DlaFlowComposeColors, onScanPairingQr: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.pairing_scan_qr),
                    color = colors.textStrong,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.pairing_scan_description),
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.Medium,
                )
            }
            DlaFlowIcon(Icons.Rounded.QrCodeScanner, colors.primary, modifier = Modifier.size(26.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(206.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.primary.copy(alpha = if (colors.dark) 0.18f else 0.08f),
                            colors.primarySoft.copy(alpha = if (colors.dark) 0.2f else 0.55f),
                            colors.surface,
                        ),
                    ),
                )
                .clickable(onClick = onScanPairingQr),
            contentAlignment = Alignment.Center,
        ) {
            PairingDotPattern(colors, Modifier.align(Alignment.CenterStart).padding(start = 24.dp))
            PairingDotPattern(colors, Modifier.align(Alignment.CenterEnd).padding(end = 24.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(124.dp)) {
                PairingQrPreview(colors)
                PairingScanCorners(colors)
            }
            Text(
                stringResource(R.string.pairing_scan_qr),
                color = colors.primary,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontFamily = DlaFlowInter,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun PairingDotPattern(colors: DlaFlowComposeColors, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(56.dp)) {
        val dotColor = colors.primary.copy(alpha = if (colors.dark) 0.16f else 0.14f)
        val gap = size.minDimension / 6f
        repeat(5) { row ->
            repeat(5) { column ->
                drawCircle(
                    color = dotColor,
                    radius = 2.5f,
                    center = Offset(column * gap + gap, row * gap + gap),
                )
            }
        }
    }
}

@Composable
internal fun PairingQrPreview(colors: DlaFlowComposeColors) {
    val previewSize = if (colors.dark) 104.dp else 116.dp
    val previewBackground = colors.pairingPreviewBackground
    Canvas(
        modifier = Modifier
            .size(previewSize)
            .clip(RoundedCornerShape(8.dp))
            .background(previewBackground)
            .padding(10.dp),
    ) {
        val dark = colors.pairingPreviewDark
        val cells = 21
        val cell = size.minDimension / cells
        val moduleInset = cell * 0.08f

        fun module(x: Int, y: Int, width: Int = 1, color: Color = dark) {
            drawRoundRect(
                color = color,
                topLeft = Offset(x * cell + moduleInset, y * cell + moduleInset),
                size = androidx.compose.ui.geometry.Size(width * cell - moduleInset * 2, width * cell - moduleInset * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.08f),
            )
        }

        fun finder(x: Int, y: Int) {
            module(x, y, 7)
            module(x + 1, y + 1, 5, Color.White)
            module(x + 2, y + 2, 3)
        }

        fun reservedByFinder(x: Int, y: Int): Boolean {
            return (x <= 7 && y <= 7) || (x >= 13 && y <= 7) || (x <= 7 && y >= 13)
        }

        finder(0, 0)
        finder(14, 0)
        finder(0, 14)

        for (index in 8..12) {
            if (index % 2 == 0) {
                module(index, 6)
                module(6, index)
            }
        }

        repeat(cells) { y ->
            repeat(cells) { x ->
                if (!reservedByFinder(x, y) && x != 6 && y != 6) {
                    val value = x * 13 + y * 17 + x * y * 7 + (x xor y) * 5
                    val denseBlock = (x in 10..17 && y in 9..18 && value % 13 in 0..5)
                    val quietBreak = x in 8..10 && y in 8..10
                    val shouldDraw = !quietBreak && (value % 11 in 0..3 || denseBlock || ((x + y) % 7 == 0 && value % 5 == 0))
                    if (shouldDraw) {
                        module(x, y)
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingScanCorners(colors: DlaFlowComposeColors) {
    Canvas(modifier = Modifier.size(124.dp)) {
        val strokeWidth = 4.dp.toPx()
        val corner = 23.dp.toPx()
        val inset = 1.dp.toPx()
        val maxX = size.width - inset
        val maxY = size.height - inset
        drawLine(colors.primary, Offset(inset, corner), Offset(inset, inset), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(inset, inset), Offset(corner, inset), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(maxX - corner, inset), Offset(maxX, inset), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(maxX, inset), Offset(maxX, corner), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(inset, maxY - corner), Offset(inset, maxY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(inset, maxY), Offset(corner, maxY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(maxX - corner, maxY), Offset(maxX, maxY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(colors.primary, Offset(maxX, maxY - corner), Offset(maxX, maxY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
private fun PairingDivider(colors: DlaFlowComposeColors) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
        Text(stringResource(R.string.pairing_or), color = colors.textMuted, fontSize = 12.sp, fontFamily = DlaFlowInter, fontWeight = FontWeight.Bold)
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
    }
}

@Composable
private fun PairingManualCodeCard(
    colors: DlaFlowComposeColors,
    pairingCode: String,
    onPairingCodeChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.pairing_code_title),
                    color = colors.textStrong,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.pairing_code_description),
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontFamily = DlaFlowInter,
                    fontWeight = FontWeight.Medium,
                )
            }
            DlaFlowIcon(Icons.Rounded.Keyboard, colors.primary, modifier = Modifier.size(25.dp))
        }
        PairingCodeBoxes(colors, pairingCode, onPairingCodeChange)
        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Text(
                stringResource(R.string.pairing_continue),
                fontSize = 15.sp,
                fontFamily = DlaFlowInter,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun PairingCodeBoxes(
    colors: DlaFlowComposeColors,
    pairingCode: String,
    onPairingCodeChange: (String) -> Unit,
) {
    val normalized = formatPairingCodeInput(pairingCode).replace("-", "")
    BasicTextField(
        value = normalized,
        onValueChange = { onPairingCodeChange(formatPairingCodeInput(it)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
        ),
        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(6) { index ->
                    val char = normalized.getOrNull(index)?.toString()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(colors.surfaceSubtle)
                            .border(1.dp, colors.border, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = char ?: "•",
                            color = if (char == null) colors.textMuted.copy(alpha = 0.75f) else colors.textStrong,
                            fontSize = if (char == null) 18.sp else 17.sp,
                            fontFamily = DlaFlowInter,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun PairingHelpLink(colors: DlaFlowComposeColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(1.dp, colors.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.QuestionMark,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.pairing_help_link),
            color = colors.primary,
            fontSize = 14.sp,
            fontFamily = DlaFlowInter,
            fontWeight = FontWeight.Bold,
        )
    }
}
