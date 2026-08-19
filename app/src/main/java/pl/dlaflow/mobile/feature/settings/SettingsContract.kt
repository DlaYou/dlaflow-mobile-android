package pl.dlaflow.mobile.feature.settings

internal const val SETTINGS_CALLER_ID_PHONE_MAX_LENGTH = 64

internal enum class SettingsKind {
    ACCOUNT,
    SECURITY,
    NOTIFICATIONS,
    PREFERENCES,
    INTEGRATIONS,
    TEAM,
    APP,
    CALLER_ID,
}

internal data class SettingsMenuItem(
    val kind: SettingsKind,
    val title: String,
    val subtitle: String,
)

internal data class SettingsDetailRow(
    val label: String,
    val value: String,
)

internal data class SettingsDetail(
    val kind: SettingsKind,
    val title: String,
    val description: String,
    val rows: List<SettingsDetailRow>,
    val primaryActionLabel: String? = null,
    val secondaryActionLabel: String? = null,
    val dangerActionLabel: String? = null,
)

internal data class SettingsUpdateInfo(
    val releaseTitle: String,
    val latestVersionName: String,
    val sizeBytes: Long,
)

internal data class SettingsCallerIdOrder(
    val orderNumber: String,
    val status: String,
)

internal data class SettingsCallerIdPreview(
    val displayName: String,
    val phone: String,
    val primaryOrder: SettingsCallerIdOrder?,
)

internal data class SettingsNotificationPreference(
    val key: String,
    val label: String,
    val description: String,
    val enabled: Boolean,
)

internal fun interface SettingsTextResolver {
    operator fun invoke(resourceId: Int, vararg arguments: Any): String
}

internal data class SettingsInput(
    val displayName: String,
    val userEmail: String,
    val tenantName: String,
    val deviceName: String,
    val phoneStatusMessage: String,
    val callerIdLabel: String,
    val callerIdPreview: SettingsCallerIdPreview?,
    val callerIdAvailable: Boolean,
    val callerIdOperational: Boolean,
    val canAutoOpenTasks: Boolean,
    val notificationAllowed: Boolean,
    val appVersionName: String,
    val update: SettingsUpdateInfo?,
    val updateChecking: Boolean,
    val updateDownloading: Boolean,
    val updateDownloadProgress: Int,
    val updateError: String,
    val textResolver: SettingsTextResolver,
    val notificationPreferenceSummary: String = "",
    val notificationPreferences: List<SettingsNotificationPreference> = emptyList(),
)

internal data class SettingsContent(
    val displayName: String,
    val userEmail: String,
    val tenantName: String,
    val deviceName: String,
    val phoneStatusMessage: String,
    val callerIdLabel: String,
    val callerIdPreview: SettingsCallerIdPreview?,
    val callerIdAvailable: Boolean,
    val callerIdOperational: Boolean,
    val canAutoOpenTasks: Boolean,
    val notificationAllowed: Boolean,
    val appVersionName: String,
    val update: SettingsUpdateInfo?,
    val updateChecking: Boolean,
    val updateDownloading: Boolean,
    val updateDownloadProgress: Int,
    val updateError: String,
    val items: List<SettingsMenuItem>,
    val textResolver: SettingsTextResolver,
    val notificationPreferenceSummary: String,
    val notificationPreferences: List<SettingsNotificationPreference>,
)

internal sealed interface SettingsRoute {
    data object List : SettingsRoute
    data class Detail(val kind: SettingsKind) : SettingsRoute
}

internal data class SettingsUiState(
    val sessionEpoch: Long = 0L,
    val route: SettingsRoute = SettingsRoute.List,
    val callerIdPhone: String = "",
    val disconnectConfirmationVisible: Boolean = false,
    val disconnecting: Boolean = false,
    val activeDisconnectRequestId: Long? = null,
)

internal data class SettingsDisconnectRequest(
    val requestId: Long,
    val sessionEpoch: Long,
)

internal data class SettingsCallerIdLookupRequest(
    val requestId: Long,
    val sessionEpoch: Long,
    val normalizedPhone: String,
) {
    fun accepts(requestId: Long, sessionEpoch: Long, phone: String): Boolean =
        this.requestId == requestId &&
            this.sessionEpoch == sessionEpoch &&
            normalizedPhone == normalizeSettingsCallerIdPhone(phone)
}

internal data class SettingsUpdateOperation(
    val operationId: Long,
    val sessionEpoch: Long,
    val lifecycleId: Long,
) {
    fun accepts(operationId: Long, sessionEpoch: Long, lifecycleId: Long): Boolean =
        this.operationId == operationId && this.sessionEpoch == sessionEpoch && this.lifecycleId == lifecycleId
}

internal fun normalizeSettingsCallerIdPhone(phone: String): String = phone.trim()

internal fun settingsSignerSetsMatch(expected: Set<String>, actual: Set<String>): Boolean =
    expected.isNotEmpty() && actual.isNotEmpty() && expected.intersect(actual).isNotEmpty()

internal sealed interface SettingsAction {
    data class Select(val kind: SettingsKind) : SettingsAction
    data object Back : SettingsAction
    data object LeaveFeature : SettingsAction
    data class CallerIdPhoneChanged(val value: String) : SettingsAction
    data object EnableCallerId : SettingsAction
    data object TestCallerId : SettingsAction
    data object ShowCallerIdPreview : SettingsAction
    data object CheckAppUpdate : SettingsAction
    data object InstallAppUpdate : SettingsAction
    data object OpenNotificationSettings : SettingsAction
    data object OpenOverlaySettings : SettingsAction
    data object OpenAppSystemSettings : SettingsAction
    data class NotificationPreferenceChanged(val key: String, val enabled: Boolean) : SettingsAction
    data object DisconnectRequested : SettingsAction
    data object DisconnectDismissed : SettingsAction
    data object DisconnectConfirmed : SettingsAction
}

internal sealed interface SettingsEffect {
    data class CallerIdPhoneChanged(val phone: String) : SettingsEffect
    data object EnableCallerId : SettingsEffect
    data class TestCallerId(val phone: String) : SettingsEffect
    data object ShowCallerIdPreview : SettingsEffect
    data object CheckAppUpdate : SettingsEffect
    data object InstallAppUpdate : SettingsEffect
    data object OpenNotificationSettings : SettingsEffect
    data object OpenOverlaySettings : SettingsEffect
    data object OpenAppSystemSettings : SettingsEffect
    data class NotificationPreferenceChanged(val key: String, val enabled: Boolean) : SettingsEffect
    data class Disconnect(val request: SettingsDisconnectRequest) : SettingsEffect
}
