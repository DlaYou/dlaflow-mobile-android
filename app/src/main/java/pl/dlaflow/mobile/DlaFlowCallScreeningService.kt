package pl.dlaflow.mobile

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import java.util.concurrent.Executors

class DlaFlowCallScreeningService : CallScreeningService() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        DlaFlowNotifications.ensureChannels(this)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            return
        }

        respondToCall(callDetails, allowCallResponse())

        val phone = callDetails.handle?.schemeSpecificPart.orEmpty()
        if (phone.isBlank()) {
            return
        }

        executor.execute {
            val sessionStore = MobileSessionStore(this)
            val token = sessionStore.readToken()
            if (token.isBlank()) {
                return@execute
            }

            runCatching {
                MobileApiClient(sessionStore.readBaseUrl()).lookupCallerId(token, phone)
            }.onSuccess { lookup ->
                if (lookup.primaryOrder != null) {
                    DlaFlowNotifications.showCallerIdNotification(this, lookup)
                    runCatching {
                        startActivity(CallerIdActivity.createIntent(this, lookup))
                    }
                }
            }.onFailure { error ->
                if (error is MobileApiException && error.statusCode == 401) {
                    sessionStore.clearSession()
                    DlaFlowBackgroundSyncService.stop(this)
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun allowCallResponse(): CallResponse {
        val builder = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setSilenceCall(false)
        }

        return builder.build()
    }
}
