package pl.dlaflow.mobile

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import pl.dlaflow.mobile.core.session.AppNotificationSessionSynchronization

/**
 * Receives bounded data messages issued by the DlaFlow panel after committed changes.
 * The server remains the source of truth; no customer address, message body or full payload is included.
 */
class DlaFlowFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val event = message.data["event"]?.trim().orEmpty()
        if (event.isBlank()) return
        val sessionStore = MobileSessionStore(applicationContext)
        AppNotificationSessionSynchronization.instance.withLock {
            if (sessionStore.readToken().isBlank()) return@withLock
            // Newer panel payloads are bound to the device that was selected for
            // delivery. Missing targetDeviceId remains backwards compatible with
            // older panel workers during the contract rollout.
            if (!pushTargetMatchesDevice(message.data["targetDeviceId"], sessionStore.readDeviceId())) return@withLock

            val notification = when (event) {
                "order.created" -> newOrderNotification(message)
                "message.created" -> newCustomerMessageNotification(message)
                else -> null
            } ?: return@withLock
            if (!shouldShowNativePanelNotification(notification, sessionStore.readNotificationPreferences())) return@withLock

            val memory = object : PanelNotificationDeliveryMemory {
                override fun readShownPanelAlertIds(): String = sessionStore.readShownPanelNotificationIds()
                override fun saveShownPanelAlertIds(ids: String) = sessionStore.saveShownPanelNotificationIds(ids)
            }
            deliverPanelNotificationOnce(memory, notification) {
                DlaFlowNotifications.showPanelAlertNotification(
                    applicationContext,
                    notification,
                    messageThreadId = message.data["threadId"],
                )
            }
        }
    }

    private fun newOrderNotification(message: RemoteMessage): MobileAssistantNotification? {
        val orderId = message.data["orderId"].orEmpty().trim()
        if (orderId.isBlank()) return null
        val orderNumber = message.data["orderNumber"].orEmpty().ifBlank { "nowe zamówienie" }
        return MobileAssistantNotification(
            id = requireNotNull(
                pushNotificationDeliveryId(
                    event = "order.created",
                    eventId = orderId,
                    canonicalNotificationId = message.data["notificationId"],
                ),
            ),
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
    }

    private fun newCustomerMessageNotification(message: RemoteMessage): MobileAssistantNotification? {
        val messageId = message.data["messageId"].orEmpty().trim()
        if (messageId.isBlank()) return null

        val orderNumber = message.data["orderNumber"].orEmpty()
        val description = if (orderNumber.isNotBlank()) {
            "Klient napisał w sprawie zamówienia $orderNumber."
        } else {
            "Klient napisał nową wiadomość."
        }

        return MobileAssistantNotification(
            id = requireNotNull(
                pushNotificationDeliveryId(
                    event = "message.created",
                    eventId = messageId,
                    canonicalNotificationId = message.data["notificationId"],
                ),
            ),
            title = "Nowa wiadomość od klienta",
            description = description,
            tone = "attention",
            source = "push",
            account = "",
            occurredAt = Instant.ofEpochMilli(
                message.sentTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
            ).toString(),
            readAt = null,
            mobileAction = MobileNotificationAction(
                type = "OPEN_MESSAGES",
                label = "Otwórz wiadomości",
            ),
        )
    }

    override fun onNewToken(token: String) {
        DlaFlowPushInstallation.save(applicationContext, token)
    }
}

internal fun pushNotificationDeliveryId(
    event: String,
    eventId: String,
    canonicalNotificationId: String? = null,
): String? {
    val normalizedEvent = event.trim()
    val id = normalizePushIdentifier(eventId) ?: return null
    val canonicalId = normalizePushIdentifier(canonicalNotificationId)
    return when (normalizedEvent) {
        "order.created" -> canonicalId ?: "order:$id"
        // The panel's notificationFocusId uses the first 24 URL-safe SHA-256 characters.
        "message.created" -> canonicalId ?: "message:" + notificationFocusDigest(id)
        else -> null
    }
}

internal fun pushTargetMatchesDevice(targetDeviceId: String?, currentDeviceId: String): Boolean {
    val target = targetDeviceId?.trim().orEmpty()
    if (target.isBlank()) return true
    if (target.length > maxPushDeviceIdLength) return false

    val current = currentDeviceId.trim()
    return current.isNotBlank() && current == target
}

private const val maxPushIdentifierLength = 200
private const val maxPushDeviceIdLength = 80

private fun normalizePushIdentifier(value: String?): String? {
    val normalized = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (normalized.length > maxPushIdentifierLength || '|' in normalized) return null
    return normalized
}

private fun notificationFocusDigest(rawId: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(rawId.toByteArray(Charsets.UTF_8))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).take(24)
}

/** Keeps the Firebase registration token locally until the paired Mobile API session registers it. */
object DlaFlowPushInstallation {
    private const val preferencesName = "dlaflow_push"
    private const val registrationTokenKey = "firebase_registration_token"

    fun refresh(context: Context) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            save(context, token)
        }
    }

    fun refreshAndReceive(context: Context, onReady: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            save(context, token)
            onReady(token)
        }
    }

    fun save(context: Context, token: String) {
        if (token.isBlank()) return
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(registrationTokenKey, token)
            .apply()
    }

    fun pendingRegistrationToken(context: Context): String =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(registrationTokenKey, "")
            .orEmpty()
}
