package pl.dlaflow.mobile.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class SettingsStateHolder {
    var state by mutableStateOf(SettingsUiState())
        private set

    private var nextDisconnectRequestId = 0L

    fun replaceSession(sessionEpoch: Long): Boolean {
        if (state.sessionEpoch == sessionEpoch) return false
        state = SettingsUiState(sessionEpoch = sessionEpoch)
        return true
    }

    fun select(kind: SettingsKind): Boolean {
        if (state.disconnectConfirmationVisible || state.disconnecting) return false
        val route = SettingsRoute.Detail(kind)
        if (state.route == route) return false
        state = state.copy(route = route)
        return true
    }

    fun back(): Boolean {
        if (state.disconnectConfirmationVisible) {
            state = state.copy(disconnectConfirmationVisible = false)
            return true
        }
        if (state.route == SettingsRoute.List) return false
        state = state.copy(route = SettingsRoute.List)
        return true
    }

    fun leaveFeature(): Boolean {
        if (state.route == SettingsRoute.List && !state.disconnectConfirmationVisible) return false
        state = state.copy(route = SettingsRoute.List, disconnectConfirmationVisible = false)
        return true
    }

    fun updateCallerIdPhone(value: String): Boolean {
        if ((state.route as? SettingsRoute.Detail)?.kind != SettingsKind.CALLER_ID) return false
        val accepted = value.take(SETTINGS_CALLER_ID_PHONE_MAX_LENGTH)
        if (state.callerIdPhone == accepted) return false
        state = state.copy(callerIdPhone = accepted)
        return true
    }

    fun requestDisconnect(): Boolean {
        val kind = (state.route as? SettingsRoute.Detail)?.kind
        if (kind != null && kind != SettingsKind.SECURITY) return false
        if (state.disconnectConfirmationVisible || state.disconnecting) return false
        state = state.copy(disconnectConfirmationVisible = true)
        return true
    }

    fun dismissDisconnect(): Boolean {
        if (!state.disconnectConfirmationVisible || state.disconnecting) return false
        state = state.copy(disconnectConfirmationVisible = false)
        return true
    }

    fun beginDisconnect(): SettingsDisconnectRequest? {
        if (!state.disconnectConfirmationVisible || state.disconnecting) return null
        val request = SettingsDisconnectRequest(++nextDisconnectRequestId, state.sessionEpoch)
        state = state.copy(
            disconnectConfirmationVisible = false,
            disconnecting = true,
            activeDisconnectRequestId = request.requestId,
        )
        return request
    }

    fun acceptDisconnectFailure(request: SettingsDisconnectRequest): Boolean {
        if (!matches(request)) return false
        state = state.copy(disconnecting = false, activeDisconnectRequestId = null)
        return true
    }

    fun acceptsDisconnectSuccess(request: SettingsDisconnectRequest): Boolean = matches(request)

    fun reset() {
        state = SettingsUiState()
    }

    private fun matches(request: SettingsDisconnectRequest): Boolean =
        state.sessionEpoch == request.sessionEpoch && state.activeDisconnectRequestId == request.requestId
}

internal fun settingsDisconnectActionEnabled(state: SettingsUiState): Boolean = !state.disconnecting
