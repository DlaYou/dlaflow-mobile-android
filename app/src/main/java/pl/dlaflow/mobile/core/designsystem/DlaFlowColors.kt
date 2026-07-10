package pl.dlaflow.mobile.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal data class DlaFlowComposeColors(
    val dark: Boolean,
    val appBg: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val border: Color,
    val borderSubtle: Color,
    val textStrong: Color,
    val text: Color,
    val textMuted: Color,
    val primary: Color,
    val primaryDeep: Color,
    val primaryGlow: Color,
    val primarySoft: Color,
    val primarySoftBorder: Color,
    val info: Color,
    val success: Color,
    val orange: Color,
    val warning: Color,
    val danger: Color,
    val pairingPreviewBackground: Color,
    val pairingPreviewDark: Color,
    val heroPositive: Color,
    val heroNegative: Color,
    val material: ColorScheme,
)

internal fun dlaFlowColors(dark: Boolean): DlaFlowComposeColors {
    val material = if (dark) {
        darkColorScheme(
            primary = Color(0xFF9B83FF),
            background = Color(0xFF0F131D),
            surface = Color(0xFF171C27),
            onPrimary = Color.White,
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF7B5CF6),
            background = Color(0xFFF8F9FC),
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
        )
    }

    return if (dark) {
        DlaFlowComposeColors(
            dark = true,
            appBg = Color(0xFF0F131D),
            surface = Color(0xFF171C27),
            surfaceSubtle = Color(0xFF151A24),
            border = Color(0xFF2A3342),
            borderSubtle = Color(0xFF202735),
            textStrong = Color(0xFFF8FAFC),
            text = Color(0xFFD7DEEA),
            textMuted = Color(0xFF9AA7BA),
            primary = Color(0xFF9B83FF),
            primaryDeep = Color(0xFF5F47D8),
            primaryGlow = Color(0xFFA78BFA),
            primarySoft = Color(0x297B5CF6),
            primarySoftBorder = Color(0x669B83FF),
            info = Color(0xFF60A5FA),
            success = Color(0xFF5EEAD4),
            orange = Color(0xFFF59E0B),
            warning = Color(0xFFFBBF24),
            danger = Color(0xFFF87171),
            pairingPreviewBackground = Color(0xFFF1EEFF),
            pairingPreviewDark = Color(0xFF151A2E),
            heroPositive = Color(0xFF3CF2B1),
            heroNegative = Color(0xFFFFB4B4),
            material = material,
        )
    } else {
        DlaFlowComposeColors(
            dark = false,
            appBg = Color.White,
            surface = Color.White,
            surfaceSubtle = Color(0xFFFBFCFE),
            border = Color(0xFFDFE4EC),
            borderSubtle = Color(0xFFEDF0F5),
            textStrong = Color(0xFF0F172A),
            text = Color(0xFF334155),
            textMuted = Color(0xFF64748B),
            primary = Color(0xFF7B5CF6),
            primaryDeep = Color(0xFF5B3FE0),
            primaryGlow = Color(0xFFA78BFA),
            primarySoft = Color(0xFFF1ECFF),
            primarySoftBorder = Color(0xFFE4DCFF),
            info = Color(0xFF2563EB),
            success = Color(0xFF0B8F78),
            orange = Color(0xFFF97316),
            warning = Color(0xFFC2410C),
            danger = Color(0xFFDC2626),
            pairingPreviewBackground = Color.White,
            pairingPreviewDark = Color(0xFF151A2E),
            heroPositive = Color(0xFF3CF2B1),
            heroNegative = Color(0xFFFFB4B4),
            material = material,
        )
    }
}
