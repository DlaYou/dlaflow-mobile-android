package pl.dlaflow.mobile.feature.notifications

import pl.dlaflow.mobile.MobileAssistantNotification
import pl.dlaflow.mobile.MobilePhotoTask
import pl.dlaflow.mobile.core.session.AppNotificationSessionSynchronization
import pl.dlaflow.mobile.core.session.NotificationSessionKey
import pl.dlaflow.mobile.core.session.NotificationSessionSynchronization
import pl.dlaflow.mobile.forgetShownNotificationId
import pl.dlaflow.mobile.hasShownNotificationId
import pl.dlaflow.mobile.rememberShownNotificationId
import pl.dlaflow.mobile.shouldShowNativePanelNotification

internal interface NotificationsBackgroundDeliveryMemory {
    fun readLastPhotoTaskId(): String
    fun saveLastPhotoTaskId(taskId: String)
    fun readShownPanelAlertIds(): String
    fun saveShownPanelAlertIds(ids: String)
}

internal enum class NotificationsBackgroundPollOutcome {
    COMPLETED,
    SKIPPED_IN_FLIGHT,
    SKIPPED_STALE_SESSION,
}

internal class NotificationsBackgroundCoordinator(
    private val synchronization: NotificationSessionSynchronization = AppNotificationSessionSynchronization.instance,
) {
    private var pollInFlight = false

    fun poll(
        capturedSessionKey: NotificationSessionKey,
        currentSessionKey: () -> NotificationSessionKey?,
        memory: NotificationsBackgroundDeliveryMemory,
        loadPhotoTask: () -> MobilePhotoTask?,
        loadPanelNotifications: () -> List<MobileAssistantNotification>,
        showPhotoTask: (MobilePhotoTask) -> Boolean,
        showPanelAlert: (MobileAssistantNotification) -> Boolean,
    ): NotificationsBackgroundPollOutcome {
        val rejectedOutcome = synchronization.withLock {
            when {
                currentSessionKey() != capturedSessionKey -> NotificationsBackgroundPollOutcome.SKIPPED_STALE_SESSION
                pollInFlight -> NotificationsBackgroundPollOutcome.SKIPPED_IN_FLIGHT
                else -> {
                    pollInFlight = true
                    null
                }
            }
        }
        if (rejectedOutcome != null) {
            return rejectedOutcome
        }

        return try {
            val photoTask = loadPhotoTask()
            if (photoTask != null) {
                deliverPhotoTask(
                    capturedSessionKey = capturedSessionKey,
                    currentSessionKey = currentSessionKey,
                    memory = memory,
                    task = photoTask,
                    effect = showPhotoTask,
                )
            }
            if (!isCurrent(capturedSessionKey, currentSessionKey)) {
                NotificationsBackgroundPollOutcome.SKIPPED_STALE_SESSION
            } else {
                val notifications = loadPanelNotifications()
                notifications
                    .asSequence()
                    .filter { it.id.isNotBlank() }
                    .filter { it.readAt.isNullOrBlank() }
                    .filter { shouldShowNativePanelNotification(it.tone, it.mobileAction.type) }
                    .forEach { notification ->
                        deliverPanelAlert(
                            capturedSessionKey = capturedSessionKey,
                            currentSessionKey = currentSessionKey,
                            memory = memory,
                            notification = notification,
                            effect = showPanelAlert,
                        )
                    }

                if (isCurrent(capturedSessionKey, currentSessionKey)) {
                    NotificationsBackgroundPollOutcome.COMPLETED
                } else {
                    NotificationsBackgroundPollOutcome.SKIPPED_STALE_SESSION
                }
            }
        } finally {
            synchronization.withLock {
                pollInFlight = false
            }
        }
    }

    private fun deliverPhotoTask(
        capturedSessionKey: NotificationSessionKey,
        currentSessionKey: () -> NotificationSessionKey?,
        memory: NotificationsBackgroundDeliveryMemory,
        task: MobilePhotoTask,
        effect: (MobilePhotoTask) -> Boolean,
    ) {
        synchronization.withLock {
            if (currentSessionKey() != capturedSessionKey || memory.readLastPhotoTaskId() == task.id) {
                return@withLock
            }

            memory.saveLastPhotoTaskId(task.id)
            val delivered = runCatching { effect(task) }.getOrDefault(false)
            if (!delivered && memory.readLastPhotoTaskId() == task.id) {
                memory.saveLastPhotoTaskId("")
            }
        }
    }

    private fun deliverPanelAlert(
        capturedSessionKey: NotificationSessionKey,
        currentSessionKey: () -> NotificationSessionKey?,
        memory: NotificationsBackgroundDeliveryMemory,
        notification: MobileAssistantNotification,
        effect: (MobileAssistantNotification) -> Boolean,
    ) {
        synchronization.withLock {
            if (currentSessionKey() != capturedSessionKey) {
                return@withLock
            }
            val shownIds = memory.readShownPanelAlertIds()
            if (hasShownNotificationId(shownIds, notification.id)) {
                return@withLock
            }

            memory.saveShownPanelAlertIds(rememberShownNotificationId(shownIds, notification.id))
            val delivered = runCatching { effect(notification) }.getOrDefault(false)
            if (!delivered) {
                memory.saveShownPanelAlertIds(
                    forgetShownNotificationId(memory.readShownPanelAlertIds(), notification.id),
                )
            }
        }
    }

    private fun isCurrent(
        capturedSessionKey: NotificationSessionKey,
        currentSessionKey: () -> NotificationSessionKey?,
    ): Boolean = synchronization.withLock { currentSessionKey() == capturedSessionKey }
}

internal object NotificationsBackgroundRuntime {
    val coordinator = NotificationsBackgroundCoordinator()
}
