package pl.dlaflow.mobile

import android.content.Context
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.time.Instant

/**
 * Receives data messages issued by the DlaFlow panel after a committed order import.
 * The server remains the source of truth; no customer address or full payload is included.
 */
class DlaFlowFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["event"] != "order.created") return

        val orderId = message.data["orderId"].orEmpty()
        val orderNumber = message.data["orderNumber"].orEmpty().ifBlank { "nowe zamówienie" }
        val notification = MobileAssistantNotification(
            id = "push-order:$orderId",
            title = "Nowe zamówienie",
            description = "Zamówienie $orderNumber oczekuje na obsługę.",
            tone = "attention",
            source = "push",
            account = "",
            occurredAt = Instant.ofEpochMilli(
                message.sentTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
            ).toString(),
            readAt = null,
            mobileAction = MobileNotificationAction(
                type = "orders",
                label = "Otwórz zamówienia",
            ),
        )
        if (shouldShowNativePanelNotification(notification, MobileSessionStore(applicationContext).readNotificationPreferences())) {
            DlaFlowNotifications.showPanelAlertNotification(applicationContext, notification)
        }
    }

    override fun onNewToken(token: String) {
        DlaFlowPushInstallation.refresh(applicationContext)
    }
}

/** Keeps the Firebase Installation ID locally until the paired Mobile API session registers it. */
object DlaFlowPushInstallation {
    private const val preferencesName = "dlaflow_push"
    private const val installationIdKey = "firebase_installation_id"

    fun refresh(context: Context) {
        FirebaseInstallations.getInstance().id.addOnSuccessListener { installationId ->
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
                .edit()
                .putString(installationIdKey, installationId)
                .apply()
        }
    }

    fun refreshAndReceive(context: Context, onReady: (String) -> Unit) {
        FirebaseInstallations.getInstance().id.addOnSuccessListener { installationId ->
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
                .edit()
                .putString(installationIdKey, installationId)
                .apply()
            onReady(installationId)
        }
    }

    fun pendingInstallationId(context: Context): String =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(installationIdKey, "")
            .orEmpty()
}
