package pl.dlaflow.mobile.feature.settings

internal data class SettingsIntentLaunchResult(
    val launched: Boolean,
    val candidateIndex: Int?,
)

internal inline fun <T> launchFirstResolvedSettingsTarget(
    candidates: List<T>,
    canResolve: (T) -> Boolean,
    launch: (T) -> Unit,
): SettingsIntentLaunchResult {
    candidates.forEachIndexed { index, candidate ->
        if (!canResolve(candidate)) return@forEachIndexed
        try {
            launch(candidate)
            return SettingsIntentLaunchResult(launched = true, candidateIndex = index)
        } catch (_: RuntimeException) {
            // A matching activity can disappear or reject the intent between resolution and launch.
        }
    }
    return SettingsIntentLaunchResult(launched = false, candidateIndex = null)
}
