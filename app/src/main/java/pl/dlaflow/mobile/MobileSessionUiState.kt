package pl.dlaflow.mobile

internal enum class MobileSessionUiState {
    PAIRING,
    CHECKING,
    OFFLINE,
    CONNECTED,
}

internal enum class MobileSessionRoute {
    PAIRING,
    RECOVERY,
    ASSISTANT,
}

internal fun mobileSessionRoute(state: MobileSessionUiState): MobileSessionRoute = when (state) {
    MobileSessionUiState.PAIRING -> MobileSessionRoute.PAIRING
    MobileSessionUiState.CHECKING,
    MobileSessionUiState.OFFLINE,
    -> MobileSessionRoute.RECOVERY
    MobileSessionUiState.CONNECTED -> MobileSessionRoute.ASSISTANT
}

internal fun mobileSessionRetryDelayMs(attempt: Int): Long {
    val safeAttempt = attempt.coerceAtLeast(0).coerceAtMost(4)
    return (2_000L shl safeAttempt).coerceAtMost(30_000L)
}
