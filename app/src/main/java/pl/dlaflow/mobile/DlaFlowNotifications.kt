package pl.dlaflow.mobile

import android.annotation.SuppressLint
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object DlaFlowDeepLinks {
    const val extraFocusPhotoTaskId = "pl.dlaflow.mobile.FOCUS_PHOTO_TASK_ID"
    const val extraSmokePackageCode = "pl.dlaflow.mobile.SMOKE_PACKAGE_CODE"

    fun photoTaskIntent(context: Context, taskId: String): Intent {
        return Intent(context, MainActivity::class.java)
            .putExtra(extraFocusPhotoTaskId, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

object DlaFlowNotifications {
    const val backgroundServiceNotificationId = 2700
    const val photoTaskNotificationId = 2701
    const val callerIdNotificationId = 2702

    private const val photoTaskChannelId = "product-photo-tasks"
    private const val callerIdChannelId = "caller-id"
    private const val backgroundChannelId = "mobile-background-sync"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val photoTaskChannel = NotificationChannel(photoTaskChannelId, "Zdjęcia produktów", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Zadania zdjęciowe wysłane z panelu."
        }
        val callerIdChannel = NotificationChannel(callerIdChannelId, "Caller ID", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Karta klienta podczas połączenia przychodzącego."
        }
        val backgroundChannel = NotificationChannel(backgroundChannelId, "Synchronizacja DlaFlow", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Lekka synchronizacja zadań i powiadomień w tle."
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(photoTaskChannel)
        notificationManager.createNotificationChannel(callerIdChannel)
        notificationManager.createNotificationChannel(backgroundChannel)
    }

    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun backgroundServiceNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        return NotificationCompat.Builder(context, backgroundChannelId)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle("DlaFlow działa w tle")
            .setContentText("Caller ID i zadania z panelu są aktywne.")
            .setContentIntent(PendingIntent.getActivity(context, 2700, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun showPhotoTaskNotification(context: Context, task: MobilePhotoTask) {
        if (!canPostNotifications(context)) {
            return
        }

        val notification = NotificationCompat.Builder(context, photoTaskChannelId)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle("Zrób zdjęcia produktu")
            .setContentText(task.productName)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    task.id.hashCode(),
                    DlaFlowDeepLinks.photoTaskIntent(context, task.id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notifyIfAllowed(context, photoTaskNotificationId, notification)
    }

    fun showCallerIdNotification(context: Context, lookup: MobileCallerIdLookup) {
        if (!canPostNotifications(context)) {
            return
        }

        val order = lookup.primaryOrder
        val title = lookup.displayName.ifBlank { "Dzwoni klient" }
        val text = callerIdNotificationText(order)

        val notification = NotificationCompat.Builder(context, callerIdChannelId)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    lookup.phone.hashCode(),
                    CallerIdActivity.createIntent(context, lookup),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()

        notifyIfAllowed(context, callerIdNotificationId, notification)
    }
}

@SuppressLint("MissingPermission")
private fun notifyIfAllowed(context: Context, notificationId: Int, notification: Notification) {
    if (!DlaFlowNotifications.canPostNotifications(context)) {
        return
    }

    runCatching {
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

internal fun callerIdNotificationText(order: MobileCallerIdOrder?): String {
    return listOfNotNull(
        order?.orderNumber,
        order?.status,
        order?.let { "${it.amount} ${it.currency}" },
    ).joinToString(" · ").ifBlank { "Dotknij, aby zobaczyć kartę DlaFlow." }
}
