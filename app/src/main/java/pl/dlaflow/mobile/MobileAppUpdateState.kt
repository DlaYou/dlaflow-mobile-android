package pl.dlaflow.mobile

const val MOBILE_APP_UPDATE_MAX_DEFERRALS = 3

enum class MobileAppUpdateStatus {
    NO_RELEASE,
    UP_TO_DATE,
    OPTIONAL_UPDATE,
    REQUIRED_UPDATE;

    companion object {
        fun fromApi(value: String): MobileAppUpdateStatus {
            return when (value.trim().lowercase()) {
                "optional_update" -> OPTIONAL_UPDATE
                "required_update" -> REQUIRED_UPDATE
                "no_release" -> NO_RELEASE
                else -> UP_TO_DATE
            }
        }
    }
}

data class MobileAppUpdate(
    val currentVersionCode: Int,
    val currentVersionName: String,
    val downloadUrl: String,
    val expiresAt: String,
    val latestVersionCode: Int,
    val latestVersionName: String,
    val minSupportedVersionCode: Int,
    val releaseNotes: List<String>,
    val releaseTitle: String,
    val required: Boolean,
    val sha256: String,
    val sizeBytes: Int,
    val status: MobileAppUpdateStatus,
    val updatePriority: String,
)

data class MobileAppUpdateDismissalState(
    val versionCode: Int = 0,
    val count: Int = 0,
)

fun mobileAppUpdateIsBlocking(update: MobileAppUpdate?, dismissals: MobileAppUpdateDismissalState): Boolean {
    if (update == null) {
        return false
    }

    return update.required || mobileAppUpdateDismissalsUsed(update, dismissals) >= MOBILE_APP_UPDATE_MAX_DEFERRALS
}

fun mobileAppUpdateDismissalsRemaining(update: MobileAppUpdate?, dismissals: MobileAppUpdateDismissalState): Int {
    if (update == null || update.required) {
        return 0
    }

    return (MOBILE_APP_UPDATE_MAX_DEFERRALS - mobileAppUpdateDismissalsUsed(update, dismissals)).coerceAtLeast(0)
}

fun nextMobileAppUpdateDismissalState(update: MobileAppUpdate, dismissals: MobileAppUpdateDismissalState): MobileAppUpdateDismissalState {
    val used = mobileAppUpdateDismissalsUsed(update, dismissals)

    return MobileAppUpdateDismissalState(
        versionCode = update.latestVersionCode,
        count = (used + 1).coerceAtMost(MOBILE_APP_UPDATE_MAX_DEFERRALS),
    )
}

private fun mobileAppUpdateDismissalsUsed(update: MobileAppUpdate, dismissals: MobileAppUpdateDismissalState): Int {
    return if (dismissals.versionCode == update.latestVersionCode) {
        dismissals.count.coerceIn(0, MOBILE_APP_UPDATE_MAX_DEFERRALS)
    } else {
        0
    }
}
