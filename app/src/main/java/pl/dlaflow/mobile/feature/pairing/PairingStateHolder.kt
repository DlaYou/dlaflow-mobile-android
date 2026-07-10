package pl.dlaflow.mobile.feature.pairing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage

internal class PairingStateHolder {
    var state by mutableStateOf(PairingUiState())
        private set

    private var helpReturnStep = PairingStep.CODE
    private var nextRequestId = 0L

    fun updateCode(value: String) {
        if (state.isSubmitting) return
        state = state.copy(
            codeInput = formatPairingCodeInput(value),
            localFeedback = null,
            sharedMessage = null,
        )
    }

    fun continueToName(): Boolean {
        if (state.isSubmitting) return false
        val code = canonicalPairingCodeOrNull(state.codeInput)
        if (code == null) {
            state = state.copy(localFeedback = PairingFeedback.INVALID_CODE, sharedMessage = null)
            return false
        }
        state = state.copy(
            step = PairingStep.NAME,
            codeInput = code,
            localFeedback = null,
            sharedMessage = null,
        )
        return true
    }

    fun acceptQrResult(rawValue: String?): Boolean {
        if (state.isSubmitting) return false
        val code = pairingCodeFromQrOrNull(rawValue)
        if (code == null) {
            state = state.copy(
                step = PairingStep.CODE,
                localFeedback = PairingFeedback.INVALID_QR,
                sharedMessage = null,
            )
            return false
        }
        state = state.copy(
            step = PairingStep.NAME,
            codeInput = code,
            localFeedback = null,
            sharedMessage = null,
        )
        return true
    }

    fun updateDeviceName(value: String) {
        if (state.isSubmitting) return
        state = state.copy(
            deviceNameInput = limitPairingDeviceNameInput(value),
            localFeedback = null,
            sharedMessage = null,
        )
    }

    fun showHelp() {
        if (state.isSubmitting) return
        helpReturnStep = state.step.takeIf { it != PairingStep.HELP } ?: PairingStep.CODE
        state = state.copy(step = PairingStep.HELP, localFeedback = null, sharedMessage = null)
    }

    fun back(): Boolean {
        if (state.isSubmitting) return true
        return when (state.step) {
            PairingStep.HELP -> {
                state = state.copy(step = helpReturnStep, localFeedback = null, sharedMessage = null)
                true
            }
            PairingStep.NAME -> {
                state = state.copy(step = PairingStep.CODE, localFeedback = null, sharedMessage = null)
                true
            }
            PairingStep.CODE -> false
        }
    }

    fun beginSubmission(): PairingSubmission? {
        if (state.isSubmitting) return null
        val code = canonicalPairingCodeOrNull(state.codeInput)
        if (code == null) {
            state = state.copy(
                step = PairingStep.CODE,
                localFeedback = PairingFeedback.INVALID_CODE,
                sharedMessage = null,
            )
            return null
        }
        val nameError = pairingDeviceNameError(state.deviceNameInput)
        if (nameError != null) {
            state = state.copy(
                step = PairingStep.NAME,
                localFeedback = nameError.toFeedback(),
                sharedMessage = null,
            )
            return null
        }
        val requestId = ++nextRequestId
        val name = normalizePairingDeviceName(state.deviceNameInput)
        state = state.copy(
            step = PairingStep.NAME,
            codeInput = code,
            deviceNameInput = name,
            isSubmitting = true,
            activeRequestId = requestId,
            localFeedback = null,
            sharedMessage = null,
        )
        return PairingSubmission(requestId, code, name)
    }

    fun failRetryable(requestId: Long, message: DlaFlowUiMessage): Boolean {
        if (state.activeRequestId != requestId) return false
        state = state.copy(
            isSubmitting = false,
            activeRequestId = null,
            sharedMessage = message,
        )
        return true
    }

    fun rejectCode(requestId: Long, feedback: PairingFeedback): Boolean {
        if (state.activeRequestId != requestId) return false
        state = state.copy(
            step = PairingStep.CODE,
            codeInput = "",
            isSubmitting = false,
            activeRequestId = null,
            localFeedback = feedback,
            sharedMessage = null,
        )
        return true
    }

    fun acceptSuccess(requestId: Long): Boolean {
        if (state.activeRequestId != requestId) return false
        state = state.copy(
            isSubmitting = false,
            activeRequestId = null,
            localFeedback = null,
            sharedMessage = null,
        )
        return true
    }

    fun reset() {
        helpReturnStep = PairingStep.CODE
        state = PairingUiState()
    }
}

private fun PairingDeviceNameError.toFeedback() = when (this) {
    PairingDeviceNameError.REQUIRED -> PairingFeedback.DEVICE_NAME_REQUIRED
    PairingDeviceNameError.TOO_SHORT -> PairingFeedback.DEVICE_NAME_TOO_SHORT
    PairingDeviceNameError.TOO_LONG -> PairingFeedback.DEVICE_NAME_TOO_LONG
    PairingDeviceNameError.CONTROL_CHARACTER -> PairingFeedback.DEVICE_NAME_INVALID
}
