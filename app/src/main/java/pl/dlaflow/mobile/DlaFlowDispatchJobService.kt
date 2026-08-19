package pl.dlaflow.mobile

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import java.util.concurrent.Executors
import pl.dlaflow.mobile.core.session.NotificationSessionKey
import pl.dlaflow.mobile.feature.notifications.NotificationsBackgroundDeliveryMemory
import pl.dlaflow.mobile.feature.notifications.NotificationsBackgroundRuntime

class DlaFlowDispatchJobService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var sessionStore: MobileSessionStore

    override fun onCreate() {
        super.onCreate()
        sessionStore = MobileSessionStore(this)
        DlaFlowNotifications.ensureChannels(this)
    }

    override fun onStartJob(params: JobParameters): Boolean {
        val token = sessionStore.readToken()
        if (token.isBlank()) {
            cancel(this)
            jobFinished(params, false)
            return false
        }

        executor.execute {
            runCatching {
                val client = mobileApiClientForSession(sessionStore)
                val capturedKey = NotificationSessionKey.create(sessionStore.readBaseUrl(), sessionStore.readDeviceId(), token)
                    ?: return@runCatching
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
            }.onFailure { error ->
                handleBackgroundSyncFailure(error, token)
            }
            jobFinished(params, false)
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun handleBackgroundSyncFailure(error: Throwable, token: String) {
        val shouldClearSession = shouldClearMobileSessionAfterUnauthorized(error) {
            mobileApiClientForSession(sessionStore).verifySession(token)
        }

        if (shouldClearSession && isSameMobileSessionToken(sessionStore.readToken(), token)) {
            sessionStore.clearSession()
            cancel(this)
        }
    }

    companion object {
        private const val jobId = 2705
        private const val periodicIntervalMs = 15 * 60 * 1000L

        fun schedule(context: Context) {
            if (MobileSessionStore(context).readToken().isBlank()) {
                cancel(context)
                return
            }

            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            val jobInfo = JobInfo.Builder(jobId, ComponentName(context, DlaFlowDispatchJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(periodicIntervalMs)
                .build()

            jobScheduler.schedule(jobInfo)
        }

        fun cancel(context: Context) {
            context.getSystemService(JobScheduler::class.java).cancel(jobId)
        }
    }
}
