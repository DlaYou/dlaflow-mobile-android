package pl.dlaflow.mobile.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
internal fun DlaFlowTheme(
    dark: Boolean,
    content: @Composable (DlaFlowComposeColors) -> Unit,
) {
    val colors = dlaFlowColors(dark)
    val base = MaterialTheme.typography
    val typography = base.copy(
        displayLarge = base.displayLarge.withDlaFlowTypography(),
        displayMedium = base.displayMedium.withDlaFlowTypography(),
        displaySmall = base.displaySmall.withDlaFlowTypography(),
        headlineLarge = base.headlineLarge.withDlaFlowTypography(),
        headlineMedium = base.headlineMedium.withDlaFlowTypography(),
        headlineSmall = base.headlineSmall.withDlaFlowTypography(),
        titleLarge = base.titleLarge.withDlaFlowTypography(),
        titleMedium = base.titleMedium.withDlaFlowTypography(),
        titleSmall = base.titleSmall.withDlaFlowTypography(),
        bodyLarge = base.bodyLarge.withDlaFlowTypography(),
        bodyMedium = base.bodyMedium.withDlaFlowTypography(),
        bodySmall = base.bodySmall.withDlaFlowTypography(),
        labelLarge = base.labelLarge.withDlaFlowTypography(),
        labelMedium = base.labelMedium.withDlaFlowTypography(),
        labelSmall = base.labelSmall.withDlaFlowTypography(),
    )

    MaterialTheme(
        colorScheme = colors.material,
        typography = typography,
    ) {
        content(colors)
    }
}
