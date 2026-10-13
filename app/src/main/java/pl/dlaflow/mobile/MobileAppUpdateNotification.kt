package pl.dlaflow.mobile

import android.content.Context

/** Checks the existing release contract from background work and claims one alert per version. */
internal fun checkAndNotifyMobileAppUpdate(
    context: Context,
    client: MobileApiClient,
    sessionStore: MobileSessionStore,
    token: String,
) {
    if (!DlaFlowNotifications.canPostNotifications(context)) {
        return
    }

    val update = client.checkAppUpdate(
        token = token,
        currentVersionCode = BuildConfig.VERSION_CODE,
        currentVersionName = BuildConfig.VERSION_NAME,
    ) ?: return

    if (sessionStore.claimUpdateNotification(update)) {
        DlaFlowNotifications.showAppUpdateNotification(context, update)
    }
}
