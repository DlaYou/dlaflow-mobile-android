package pl.dlaflow.mobile

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class DlaFlowBackgroundSyncService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val pollRunnable = object : Runnable {
        override fun run() {
            checkDispatch()
            handler.postDelayed(this, backgroundPollIntervalMs)
        }
    }
    private lateinit var sessionStore: MobileSessionStore

    override fun onCreate() {
        super.onCreate()
        sessionStore = MobileSessionStore(this)
        DlaFlowNotifications.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (sessionStore.readToken().isBlank()) {
            DlaFlowDispatchJobService.cancel(this)
            stopSelf()
            return START_NOT_STICKY
        }

        DlaFlowDispatchJobService.schedule(this)
        startForeground(
            DlaFlowNotifications.backgroundServiceNotificationId,
            DlaFlowNotifications.backgroundServiceNotification(this),
        )
        startPolling()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTimeout(startId: Int, fgsType: Int) {
        DlaFlowDispatchJobService.schedule(this)
        stopSelf(startId)
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun startPolling() {
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
    }

    private fun checkDispatch() {
        val token = sessionStore.readToken()
        if (token.isBlank()) {
            DlaFlowDispatchJobService.cancel(this)
            stopSelf()
            return
        }

        executor.execute {
            runCatching {
                MobileApiClient(sessionStore.readBaseUrl()).getPhotoTaskDispatch(token)
            }.onSuccess { dispatch ->
                val task = dispatch.pendingOpenTask ?: return@onSuccess
                if (sessionStore.readLastBackgroundPhotoTaskId() == task.id) {
                    return@onSuccess
                }

                sessionStore.saveLastBackgroundPhotoTaskId(task.id)
                DlaFlowNotifications.showPhotoTaskNotification(this, task)
            }.onFailure { error ->
                if (error is MobileApiException && error.statusCode == 401) {
                    sessionStore.clearSession()
                    DlaFlowDispatchJobService.cancel(this)
                    stopSelf()
                }
            }
        }
    }

    companion object {
        internal const val backgroundPollIntervalMs = 60_000L

        fun start(context: Context) {
            if (MobileSessionStore(context).readToken().isBlank()) {
                DlaFlowDispatchJobService.cancel(context)
                return
            }
            DlaFlowDispatchJobService.schedule(context)
            val intent = Intent(context, DlaFlowBackgroundSyncService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            DlaFlowDispatchJobService.cancel(context)
            context.stopService(Intent(context, DlaFlowBackgroundSyncService::class.java))
        }
    }
}
