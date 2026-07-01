package pl.dlaflow.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DlaFlowBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (shouldScheduleBackgroundSyncForAction(intent.action)) {
            DlaFlowDispatchJobService.schedule(context)
        }
    }
}

internal fun shouldScheduleBackgroundSyncForAction(action: String?): Boolean {
    return action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED
}
