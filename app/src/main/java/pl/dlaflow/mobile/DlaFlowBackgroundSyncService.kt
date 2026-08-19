package pl.dlaflow.mobile

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import pl.dlaflow.mobile.core.session.NotificationSessionKey
import pl.dlaflow.mobile.feature.notifications.NotificationsBackgroundDeliveryMemory
import pl.dlaflow.mobile.feature.notifications.NotificationsBackgroundRuntime

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

        val capturedKey = NotificationSessionKey.create(sessionStore.readBaseUrl(), sessionStore.readDeviceId(), token)
            ?: return
        executor.execute {
            runCatching {
                val client = mobileApiClientForSession(sessionStore)
                NotificationsBackgroundRuntime.coordinator.poll(
                    capturedSessionKey = capturedKey,
                    currentSessionKey = {
                        NotificationSessionKey.create(sessionStore.readBaseUrl(), sessionStore.readDeviceId(), sessionStore.readToken())
                    },
                    memory = object : NotificationsBackgroundDeliveryMemory {
                        override fun readLastPhotoTaskId() = sessionStore.readLastBackgroundPhotoTaskId()
                        override fun saveLastPhotoTaskId(taskId: String) = sessionStore.saveLastBackgroundPhotoTaskId(taskId)
                        override fun readShownPanelAlertIds() = sessionStore.readShownPanelNotificationIds()
                        override fun saveShownPanelAlertIds(ids: String) = sessionStore.saveShownPanelNotificationIds(ids)
                    },
                    loadPhotoTask = { client.getPhotoTaskDispatch(token).pendingOpenTask },
                    loadPanelNotifications = {
                        client.listNotifications(token, limit = 10).also { page ->
                            DlaFlowNotifications.updateBackgroundServiceNotification(this, page.unreadCount)
                        }.notifications
                    },
                    showPhotoTask = { task ->
                        if (!shouldShowNativePhotoTaskNotification(sessionStore.readNotificationPreferences())) false
                        else {
                            DlaFlowNotifications.showPhotoTaskNotification(this, task)
                            true
                        }
                    },
                    showPanelAlert = { notification ->
                        if (!shouldShowNativePanelNotification(notification, sessionStore.readNotificationPreferences())) false
                        else DlaFlowNotifications.showPanelAlertNotification(this, notification)
                    },
                )
            }.onFailure { error -> handleBackgroundSyncFailure(error, token) }
        }
    }

    private fun handleBackgroundSyncFailure(error: Throwable, token: String) {
        val shouldClearSession = shouldClearMobileSessionAfterUnauthorized(error) {
            mobileApiClientForSession(sessionStore).verifySession(token)
        }

        if (shouldClearSession && isSameMobileSessionToken(sessionStore.readToken(), token)) {
            sessionStore.clearSession()
            DlaFlowDispatchJobService.cancel(this)
            stopSelf()
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

internal fun pollUnreadPanelAlertNotifications(
    context: Context,
    sessionStore: MobileSessionStore,
    client: MobileApiClient,
    token: String,
) {
    val notificationPage = client.listNotifications(token, limit = 10)
    DlaFlowNotifications.updateBackgroundServiceNotification(context, notificationPage.unreadCount)
    var shownIds = sessionStore.readShownPanelNotificationIds()
    val preferences = sessionStore.readNotificationPreferences()

    notificationPage.notifications
        .filter { it.readAt.isNullOrBlank() }
        .filter { shouldShowNativePanelNotification(it, preferences) }
        .forEach { notification ->
            if (!hasShownNotificationId(shownIds, notification.id)) {
                val shown = DlaFlowNotifications.showPanelAlertNotification(context, notification)
                if (shown) {
                    shownIds = rememberShownNotificationId(shownIds, notification.id)
                }
            }
        }

    sessionStore.saveShownPanelNotificationIds(shownIds)
}
