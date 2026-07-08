package pl.dlaflow.mobile

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import java.util.concurrent.Executors

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
                client to client.getPhotoTaskDispatch(token)
            }.onSuccess { (client, dispatch) ->
                val task = dispatch.pendingOpenTask
                if (task != null && sessionStore.readLastBackgroundPhotoTaskId() != task.id) {
                    sessionStore.saveLastBackgroundPhotoTaskId(task.id)
                    DlaFlowNotifications.showPhotoTaskNotification(this, task)
                }

                runCatching {
                    pollUnreadPanelAlertNotifications(this, sessionStore, client, token)
                }.onFailure { error ->
                    handleBackgroundSyncFailure(error, token)
                }
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
