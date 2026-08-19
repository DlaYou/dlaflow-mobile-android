package pl.dlaflow.mobile.feature.settings

internal class SettingsCoordinator(
    private val stateHolder: SettingsStateHolder,
    private val onEffect: (SettingsEffect) -> Unit,
) {
    fun onAction(action: SettingsAction, content: SettingsContent) {
        when (action) {
            is SettingsAction.Select -> stateHolder.select(action.kind)
            SettingsAction.Back -> stateHolder.back()
            SettingsAction.LeaveFeature -> stateHolder.leaveFeature()
            is SettingsAction.CallerIdPhoneChanged -> {
                if (stateHolder.updateCallerIdPhone(action.value)) {
                    onEffect(SettingsEffect.CallerIdPhoneChanged(stateHolder.state.callerIdPhone))
                }
            }
            SettingsAction.DisconnectRequested -> stateHolder.requestDisconnect()
            SettingsAction.DisconnectDismissed -> stateHolder.dismissDisconnect()
            SettingsAction.DisconnectConfirmed -> stateHolder.beginDisconnect()?.let {
                onEffect(SettingsEffect.Disconnect(it))
            }
            SettingsAction.EnableCallerId -> emitFor(SettingsKind.CALLER_ID) {
                if (content.callerIdAvailable && !content.callerIdOperational) SettingsEffect.EnableCallerId else null
            }
            SettingsAction.TestCallerId -> emitFor(SettingsKind.CALLER_ID) {
                stateHolder.state.callerIdPhone.trim().takeIf(String::isNotEmpty)?.let(SettingsEffect::TestCallerId)
            }
            SettingsAction.ShowCallerIdPreview -> emitFor(SettingsKind.CALLER_ID) {
                SettingsEffect.ShowCallerIdPreview.takeIf { content.callerIdPreview?.primaryOrder != null }
            }
            SettingsAction.CheckAppUpdate -> emitFor(SettingsKind.APP) {
                SettingsEffect.CheckAppUpdate.takeIf {
                    content.update == null && !content.updateChecking && !content.updateDownloading
                }
            }
            SettingsAction.InstallAppUpdate -> emitFor(SettingsKind.APP) {
                SettingsEffect.InstallAppUpdate.takeIf {
                    content.update != null && !content.updateChecking && !content.updateDownloading
                }
            }
            SettingsAction.OpenNotificationSettings -> emitFor(SettingsKind.NOTIFICATIONS) {
                SettingsEffect.OpenNotificationSettings
            }
            SettingsAction.OpenOverlaySettings -> emitFor(SettingsKind.PREFERENCES) {
                SettingsEffect.OpenOverlaySettings.takeIf { !content.canAutoOpenTasks }
            }
            SettingsAction.OpenAppSystemSettings -> emitFor(SettingsKind.APP) {
                SettingsEffect.OpenAppSystemSettings
            }
            is SettingsAction.NotificationPreferenceChanged -> emitFor(SettingsKind.NOTIFICATIONS) {
                SettingsEffect.NotificationPreferenceChanged(action.key, action.enabled)
            }
        }
    }

    fun replaceSession(sessionEpoch: Long): Boolean = stateHolder.replaceSession(sessionEpoch)

    fun acceptDisconnectFailure(request: SettingsDisconnectRequest): Boolean =
        stateHolder.acceptDisconnectFailure(request)

    fun acceptsDisconnectSuccess(request: SettingsDisconnectRequest): Boolean =
        stateHolder.acceptsDisconnectSuccess(request)

    fun reset() {
        stateHolder.reset()
    }

    private inline fun emitFor(kind: SettingsKind, effect: () -> SettingsEffect?) {
        if ((stateHolder.state.route as? SettingsRoute.Detail)?.kind != kind) return
        effect()?.let(onEffect)
    }
}
