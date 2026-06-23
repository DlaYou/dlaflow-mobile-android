package pl.dlaflow.mobile

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import java.util.concurrent.Executors

class DlaFlowCallScreeningService : CallScreeningService() {
    private val executor = Executors.newSingleThreadExecutor()

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
                    startActivity(CallerIdActivity.createIntent(this, lookup))
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun allowCallResponse(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
    }
}
