package pl.dlaflow.mobile.feature.settings

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
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowBackHeader
import pl.dlaflow.mobile.core.designsystem.DlaFlowCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowConfirmationDialog
import pl.dlaflow.mobile.core.designsystem.DlaFlowDangerButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowIcon
import pl.dlaflow.mobile.core.designsystem.DlaFlowKeyValue
import pl.dlaflow.mobile.core.designsystem.DlaFlowNavigationRow
import pl.dlaflow.mobile.core.designsystem.DlaFlowPrimaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowScreenHeader
import pl.dlaflow.mobile.core.designsystem.DlaFlowSecondaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusBadge
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusStrip
import pl.dlaflow.mobile.core.designsystem.DlaFlowTextField

@Composable
internal fun SettingsFeatureScreen(
    colors: DlaFlowComposeColors,
    state: SettingsUiState,
    content: SettingsContent,
    onAction: (SettingsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_feature_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val route = state.route) {
            SettingsRoute.List -> SettingsList(colors, state, content, onAction)
            is SettingsRoute.Detail -> SettingsDetailScreen(colors, state, content, route.kind, onAction)
        }
        Spacer(Modifier.height(80.dp))
    }

    if (state.disconnectConfirmationVisible) {
        DlaFlowConfirmationDialog(
            colors = colors,
            title = stringResource(R.string.settings_disconnect_confirm_title),
            description = stringResource(R.string.settings_disconnect_confirm_description),
            confirmLabel = stringResource(R.string.settings_disconnect_confirm),
            dismissLabel = stringResource(R.string.settings_cancel),
            destructive = true,
            confirmEnabled = !state.disconnecting,
            modifier = Modifier.testTag("settings_disconnect_dialog"),
            confirmButtonModifier = Modifier.testTag("settings_disconnect_confirm"),
            dismissButtonModifier = Modifier.testTag("settings_disconnect_dismiss"),
            onConfirm = { onAction(SettingsAction.DisconnectConfirmed) },
            onDismiss = { onAction(SettingsAction.DisconnectDismissed) },
        )
    }
}

@Composable
private fun SettingsList(
    colors: DlaFlowComposeColors,
    state: SettingsUiState,
    content: SettingsContent,
    onAction: (SettingsAction) -> Unit,
) {
    DlaFlowScreenHeader(
        colors = colors,
        title = stringResource(R.string.settings_title),
        subtitle = stringResource(R.string.settings_subtitle),
    )
    SettingsAccountCard(colors, content)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
    ) {
        content.items.forEach { item ->
            DlaFlowNavigationRow(
                colors = colors,
                icon = settingsIcon(item.kind),
                title = item.title,
                subtitle = item.subtitle,
                modifier = Modifier.testTag("settings_menu_${item.kind.name.lowercase(Locale.ROOT)}"),
                onClick = { onAction(SettingsAction.Select(item.kind)) },
            )
        }
    }
    DlaFlowCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DlaFlowIcon(Icons.Rounded.PhoneAndroid, colors.primary, modifier = Modifier.size(38.dp))
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_phone_status), color = colors.textStrong, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(content.phoneStatusMessage, color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(14.dp))
        DlaFlowDangerButton(
            colors = colors,
            text = stringResource(R.string.settings_disconnect),
            modifier = Modifier.fillMaxWidth().testTag("settings_disconnect_action"),
            enabled = settingsDisconnectActionEnabled(state),
            onClick = { onAction(SettingsAction.DisconnectRequested) },
        )
    }
}

@Composable
private fun SettingsDetailScreen(
    colors: DlaFlowComposeColors,
    state: SettingsUiState,
    content: SettingsContent,
    kind: SettingsKind,
    onAction: (SettingsAction) -> Unit,
) {
    val detail = content.detail(kind)
    DlaFlowBackHeader(
        colors = colors,
        title = detail.title,
        subtitle = detail.description,
        backContentDescription = stringResource(R.string.settings_back),
        backButtonModifier = Modifier.testTag("settings_back"),
        onBack = { onAction(SettingsAction.Back) },
    )
    DlaFlowCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DlaFlowIcon(settingsIcon(kind), colors.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Text(detail.title, color = colors.textStrong, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(10.dp))
        detail.rows.forEachIndexed { index, row ->
            DlaFlowKeyValue(
                colors,
                row.label,
                row.value,
                adaptive = true,
                modifier = Modifier.testTag("settings_key_value_$index"),
                labelModifier = Modifier.testTag("settings_key_value_${index}_label"),
                valueModifier = Modifier.testTag("settings_key_value_${index}_value"),
            )
        }
        if (kind == SettingsKind.APP) {
            SettingsUpdateActions(colors, content, detail, onAction)
        }
    }

    when (kind) {
        SettingsKind.NOTIFICATIONS -> {
            SettingsNotificationPreferences(colors, content, onAction)
            DlaFlowPrimaryButton(
                colors,
                Icons.Rounded.NotificationsNone,
                detail.primaryActionLabel ?: stringResource(R.string.settings_notifications_open),
                modifier = Modifier.fillMaxWidth().testTag("settings_notifications_action"),
                onClick = { onAction(SettingsAction.OpenNotificationSettings) },
            )
        }
        SettingsKind.PREFERENCES -> if (detail.primaryActionLabel != null) {
            DlaFlowPrimaryButton(
                colors,
                Icons.Rounded.Tune,
                detail.primaryActionLabel,
                modifier = Modifier.fillMaxWidth().testTag("settings_overlay_action"),
                onClick = { onAction(SettingsAction.OpenOverlaySettings) },
            )
        }
        SettingsKind.CALLER_ID -> SettingsCallerIdActions(colors, state, content, onAction)
        SettingsKind.SECURITY -> DlaFlowCard(colors) {
            Text(stringResource(R.string.settings_disconnect_description), color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            DlaFlowDangerButton(
                colors = colors,
                text = detail.dangerActionLabel ?: stringResource(R.string.settings_disconnect),
                modifier = Modifier.fillMaxWidth().testTag("settings_disconnect_action"),
                enabled = settingsDisconnectActionEnabled(state),
                onClick = { onAction(SettingsAction.DisconnectRequested) },
            )
        }
        SettingsKind.ACCOUNT,
        SettingsKind.INTEGRATIONS,
        SettingsKind.TEAM,
        SettingsKind.APP -> Unit
    }
}

@Composable
private fun SettingsNotificationPreferences(
    colors: DlaFlowComposeColors,
    content: SettingsContent,
    onAction: (SettingsAction) -> Unit,
) {
    if (content.notificationPreferences.isEmpty()) return

    DlaFlowCard(colors) {
        Text(
            text = stringResource(R.string.settings_notification_preferences_title),
            color = colors.textStrong,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = content.notificationPreferenceSummary,
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 15.sp,
        )
        Spacer(Modifier.height(8.dp))
        content.notificationPreferences.forEachIndexed { index, preference ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(1.dp)
                        .background(colors.borderSubtle),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preference.label,
                        color = colors.textStrong,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 16.sp,
                    )
                    Text(
                        text = preference.description,
                        color = colors.textMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(
                    checked = preference.enabled,
                    onCheckedChange = { enabled ->
                        onAction(SettingsAction.NotificationPreferenceChanged(preference.key, enabled))
                    },
                    modifier = Modifier.testTag("settings_notification_${preference.key}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.primary,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.surfaceSubtle,
                        uncheckedBorderColor = colors.border,
                    ),
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SettingsCallerIdActions(
    colors: DlaFlowComposeColors,
    state: SettingsUiState,
    content: SettingsContent,
    onAction: (SettingsAction) -> Unit,
) {
    DlaFlowCard(colors) {
        if (content.callerIdAvailable && !content.callerIdOperational) {
            DlaFlowPrimaryButton(
                colors,
                Icons.Rounded.Call,
                stringResource(R.string.settings_caller_id_enable),
                modifier = Modifier.fillMaxWidth().testTag("settings_caller_id_enable"),
                onClick = { onAction(SettingsAction.EnableCallerId) },
            )
            Spacer(Modifier.height(10.dp))
        }
        DlaFlowTextField(
            colors = colors,
            label = stringResource(R.string.settings_caller_id_phone),
            value = state.callerIdPhone,
            onValueChange = { onAction(SettingsAction.CallerIdPhoneChanged(it)) },
            modifier = Modifier.testTag("settings_caller_id_input"),
        )
        Spacer(Modifier.height(10.dp))
        DlaFlowSecondaryButton(
            colors,
            Icons.Rounded.Call,
            stringResource(R.string.settings_caller_id_test),
            modifier = Modifier.fillMaxWidth().testTag("settings_caller_id_test"),
            enabled = state.callerIdPhone.isNotBlank(),
            onClick = { onAction(SettingsAction.TestCallerId) },
        )
        content.callerIdPreview?.let { preview ->
            Spacer(Modifier.height(12.dp))
            Text(
                preview.primaryOrder?.let { order ->
                    stringResource(R.string.settings_caller_id_preview_order, preview.displayName.ifBlank { preview.phone }, order.orderNumber, order.status)
                } ?: stringResource(R.string.settings_caller_id_preview_empty, preview.phone),
                color = colors.textStrong,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (preview.primaryOrder != null) {
                Spacer(Modifier.height(10.dp))
                DlaFlowPrimaryButton(
                    colors,
                    Icons.Rounded.Call,
                    stringResource(R.string.settings_caller_id_show),
                    modifier = Modifier.fillMaxWidth().testTag("settings_caller_id_preview"),
                    onClick = { onAction(SettingsAction.ShowCallerIdPreview) },
                )
            }
        }
    }
    DlaFlowStatusStrip(colors, content.phoneStatusMessage)
}

@Composable
private fun SettingsUpdateActions(
    colors: DlaFlowComposeColors,
    content: SettingsContent,
    detail: SettingsDetail,
    onAction: (SettingsAction) -> Unit,
) {
    Spacer(Modifier.height(12.dp))
    content.update?.let { update ->
        Text(update.releaseTitle, color = colors.textStrong, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            stringResource(R.string.settings_update_available, update.latestVersionName, formatSettingsBytes(update.sizeBytes)),
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
    }
    if (content.updateDownloading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .progressSemantics(content.updateDownloadProgress / 100f)
                .testTag("settings_update_progress"),
        ) {
            DlaFlowStatusStrip(colors, stringResource(R.string.settings_update_progress, content.updateDownloadProgress))
        }
        Spacer(Modifier.height(10.dp))
    }
    if (content.updateError.isNotBlank()) {
        DlaFlowStatusStrip(colors, content.updateError)
        Spacer(Modifier.height(10.dp))
    }
    val busy = content.updateChecking || content.updateDownloading
    val label = when {
        content.updateDownloading -> stringResource(R.string.settings_update_downloading)
        content.updateChecking -> stringResource(R.string.settings_update_checking)
        content.update != null -> stringResource(R.string.settings_update_install)
        else -> detail.primaryActionLabel ?: stringResource(R.string.settings_update_check)
    }
    DlaFlowPrimaryButton(
        colors,
        Icons.Rounded.Refresh,
        label,
        modifier = Modifier.fillMaxWidth().testTag("settings_update_primary"),
        enabled = !busy,
        onClick = { onAction(if (content.update != null) SettingsAction.InstallAppUpdate else SettingsAction.CheckAppUpdate) },
    )
    Spacer(Modifier.height(8.dp))
    DlaFlowSecondaryButton(
        colors,
        Icons.Rounded.Settings,
        detail.secondaryActionLabel ?: stringResource(R.string.settings_system_open),
        modifier = Modifier.fillMaxWidth().testTag("settings_update_system"),
        enabled = !busy,
        onClick = { onAction(SettingsAction.OpenAppSystemSettings) },
    )
}

@Composable
private fun SettingsAccountCard(colors: DlaFlowComposeColors, content: SettingsContent) {
    DlaFlowCard(colors) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.primary, colors.primary.copy(alpha = 0.72f)))),
                contentAlignment = Alignment.Center,
            ) {
                Text(content.displayName.take(1).uppercase(Locale("pl", "PL")), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(content.displayName, color = colors.textStrong, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(content.tenantName, color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(content.userEmail, color = colors.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DlaFlowStatusBadge(colors, stringResource(R.string.settings_connected))
        }
    }
}

private fun settingsIcon(kind: SettingsKind): ImageVector = when (kind) {
    SettingsKind.ACCOUNT -> Icons.Rounded.AccountCircle
    SettingsKind.SECURITY -> Icons.Rounded.Security
    SettingsKind.NOTIFICATIONS -> Icons.Rounded.NotificationsNone
    SettingsKind.PREFERENCES -> Icons.Rounded.Tune
    SettingsKind.INTEGRATIONS -> Icons.Rounded.Settings
    SettingsKind.TEAM -> Icons.Rounded.Groups
    SettingsKind.APP -> Icons.Rounded.PhoneAndroid
    SettingsKind.CALLER_ID -> Icons.Rounded.Call
}

private fun formatSettingsBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale("pl", "PL"), "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale("pl", "PL"), "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
