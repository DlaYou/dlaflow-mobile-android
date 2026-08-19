package pl.dlaflow.mobile.feature.settings

import pl.dlaflow.mobile.R

internal fun buildSettingsContent(input: SettingsInput): SettingsContent {
    val displayName = input.displayName.ifBlank {
        input.userEmail.substringBefore("@").ifBlank { input.text(R.string.settings_operator_fallback) }
    }
    val tenantName = input.tenantName.ifBlank { input.text(R.string.settings_tenant_fallback) }
    val deviceName = input.deviceName.ifBlank { input.text(R.string.settings_device_fallback) }
    val callerIdLabel = input.callerIdLabel.ifBlank { input.text(R.string.settings_caller_id_fallback) }
    val updateProgress = input.updateDownloadProgress.coerceIn(0, 100)
    return SettingsContent(
        displayName = displayName,
        userEmail = input.userEmail,
        tenantName = tenantName,
        deviceName = deviceName,
        phoneStatusMessage = input.phoneStatusMessage.ifBlank { input.text(R.string.settings_phone_normal) },
        callerIdLabel = callerIdLabel,
        callerIdPreview = input.callerIdPreview,
        callerIdAvailable = input.callerIdAvailable,
        callerIdOperational = input.callerIdOperational,
        canAutoOpenTasks = input.canAutoOpenTasks,
        notificationAllowed = input.notificationAllowed,
        appVersionName = input.appVersionName,
        update = input.update,
        updateChecking = input.updateChecking,
        updateDownloading = input.updateDownloading,
        updateDownloadProgress = updateProgress,
        updateError = input.updateError,
        items = buildSettingsItems(input, callerIdLabel),
        textResolver = input.textResolver,
        notificationPreferenceSummary = input.notificationPreferenceSummary,
        notificationPreferences = input.notificationPreferences,
    )
}

internal fun SettingsContent.detail(kind: SettingsKind): SettingsDetail = detail(kind, textResolver)

private fun SettingsContent.detail(kind: SettingsKind, text: SettingsTextResolver): SettingsDetail = when (kind) {
    SettingsKind.ACCOUNT -> SettingsDetail(
        kind = kind,
        title = text(R.string.settings_account_title),
        description = text(R.string.settings_account_description),
        rows = listOf(
            SettingsDetailRow(text(R.string.settings_label_operator), displayName),
            SettingsDetailRow(text(R.string.settings_label_email), userEmail),
            SettingsDetailRow(text(R.string.settings_label_company), tenantName),
            SettingsDetailRow(text(R.string.settings_label_phone), deviceName),
        ),
    )
    SettingsKind.SECURITY -> SettingsDetail(
        kind = kind,
        title = text(R.string.settings_security_title),
        description = text(R.string.settings_security_description),
        rows = listOf(
            SettingsDetailRow(text(R.string.settings_label_phone_connection), text(R.string.settings_value_active)),
            SettingsDetailRow(text(R.string.settings_label_protection), text(R.string.settings_value_android_protection)),
            SettingsDetailRow(text(R.string.settings_label_access), text(R.string.settings_value_mobile_access)),
        ),
        dangerActionLabel = text(R.string.settings_disconnect),
    )
    SettingsKind.NOTIFICATIONS -> SettingsDetail(
        kind = kind,
        title = text(R.string.settings_notifications_title),
        description = text(R.string.settings_notifications_description),
        rows = listOf(
            SettingsDetailRow(
                text(R.string.settings_label_status),
                text(if (notificationAllowed) R.string.settings_value_enabled else R.string.settings_value_android_permission),
            ),
            SettingsDetailRow(text(R.string.settings_label_photo_tasks), text(R.string.settings_value_photo_task_notification)),
            SettingsDetailRow(text(R.string.settings_label_updates), text(R.string.settings_value_update_notification)),
        ),
        primaryActionLabel = text(R.string.settings_notifications_open),
    )
    SettingsKind.PREFERENCES -> SettingsDetail(
        kind = kind,
        title = text(R.string.settings_preferences_title),
        description = text(R.string.settings_preferences_description),
        rows = listOf(
            SettingsDetailRow(
                text(R.string.settings_label_auto_open),
                text(if (canAutoOpenTasks) R.string.settings_value_enabled else R.string.settings_value_via_notification),
            ),
            SettingsDetailRow(text(R.string.settings_label_theme), text(R.string.settings_value_system_theme)),
            SettingsDetailRow(text(R.string.settings_label_layout), text(R.string.settings_value_mobile_layout)),
        ),
        primaryActionLabel = if (canAutoOpenTasks) null else text(R.string.settings_overlay_enable),
    )
    SettingsKind.INTEGRATIONS -> SettingsDetail(
        kind = kind,
        title = text(R.string.settings_integrations_title),
        description = text(R.string.settings_integrations_description),
        rows = listOf(
            SettingsDetailRow(text(R.string.settings_label_plugin), text(R.string.settings_value_mobile_assistant)),
            SettingsDetailRow(text(R.string.settings_label_connection), text(R.string.settings_value_company_active)),
            SettingsDetailRow(text(R.string.settings_label_management), text(R.string.settings_value_integrations_path)),
        ),
    )
    SettingsKind.TEAM -> SettingsDetail(
        kind = kind,
        title = text(R.string.settings_team_title),
        description = text(R.string.settings_team_description),
        rows = listOf(
            SettingsDetailRow(text(R.string.settings_label_employee), displayName),
            SettingsDetailRow(text(R.string.settings_label_company), tenantName),
            SettingsDetailRow(text(R.string.settings_label_management), text(R.string.settings_value_team_path)),
        ),
    )
    SettingsKind.APP -> SettingsDetail(
        kind = kind,
        title = text(R.string.settings_app_title),
        description = text(R.string.settings_app_description),
        rows = listOf(
            SettingsDetailRow(text(R.string.settings_label_version), appVersionName),
            SettingsDetailRow(text(R.string.settings_label_updates), text(R.string.settings_value_safe_updates)),
            SettingsDetailRow(
                text(R.string.settings_label_status),
                text(if (update != null) R.string.settings_value_update_available else R.string.settings_value_current_version),
            ),
        ),
        primaryActionLabel = text(R.string.settings_update_check),
        secondaryActionLabel = text(R.string.settings_system_open),
    )
    SettingsKind.CALLER_ID -> SettingsDetail(
        kind = kind,
        title = text(R.string.settings_caller_id_title),
        description = text(R.string.settings_caller_id_description),
        rows = listOf(
            SettingsDetailRow(text(R.string.settings_label_status), callerIdLabel),
            SettingsDetailRow(text(R.string.settings_label_phone_test), text(R.string.settings_value_phone_test)),
            SettingsDetailRow(text(R.string.settings_label_calls), text(R.string.settings_value_android_calls)),
        ),
        primaryActionLabel = text(R.string.settings_caller_id_test),
        secondaryActionLabel = text(R.string.settings_caller_id_enable),
    )
}

private fun buildSettingsItems(input: SettingsInput, callerIdLabel: String): List<SettingsMenuItem> = listOf(
    SettingsMenuItem(
        SettingsKind.ACCOUNT,
        input.text(R.string.settings_account_title),
        input.text(R.string.settings_menu_account_subtitle),
    ),
    SettingsMenuItem(
        SettingsKind.SECURITY,
        input.text(R.string.settings_security_title),
        input.text(R.string.settings_menu_security_subtitle),
    ),
    SettingsMenuItem(
        SettingsKind.NOTIFICATIONS,
        input.text(R.string.settings_notifications_title),
        input.text(R.string.settings_menu_notifications_subtitle),
    ),
    SettingsMenuItem(
        SettingsKind.PREFERENCES,
        input.text(R.string.settings_preferences_title),
        input.text(if (input.canAutoOpenTasks) R.string.settings_menu_auto_open else R.string.settings_value_via_notification),
    ),
    SettingsMenuItem(
        SettingsKind.INTEGRATIONS,
        input.text(R.string.settings_integrations_title),
        input.text(R.string.settings_menu_integrations_subtitle),
    ),
    SettingsMenuItem(
        SettingsKind.TEAM,
        input.text(R.string.settings_team_title),
        input.text(R.string.settings_menu_team_subtitle),
    ),
    SettingsMenuItem(
        SettingsKind.APP,
        input.text(R.string.settings_app_title),
        if (input.update != null) {
            input.text(R.string.settings_value_update_available)
        } else {
            input.text(R.string.settings_menu_app_version, input.appVersionName)
        },
    ),
    SettingsMenuItem(SettingsKind.CALLER_ID, input.text(R.string.settings_caller_id_title), callerIdLabel),
)

private fun SettingsInput.text(resourceId: Int, vararg arguments: Any): String =
    textResolver(resourceId, *arguments)

private fun SettingsTextResolver.text(resourceId: Int, vararg arguments: Any): String =
    this(resourceId, *arguments)
