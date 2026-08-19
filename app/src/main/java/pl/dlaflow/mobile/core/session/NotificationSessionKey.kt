package pl.dlaflow.mobile.core.session

import java.security.MessageDigest

internal class NotificationSessionKey private constructor(
    private val normalizedBaseUrl: String,
    private val normalizedDeviceId: String,
    private val tokenDigest: ByteArray,
) {
    override fun equals(other: Any?): Boolean = other is NotificationSessionKey &&
        normalizedBaseUrl == other.normalizedBaseUrl &&
        normalizedDeviceId == other.normalizedDeviceId &&
        tokenDigest.contentEquals(other.tokenDigest)

    override fun hashCode(): Int = 31 * (31 * normalizedBaseUrl.hashCode() + normalizedDeviceId.hashCode()) + tokenDigest.contentHashCode()
    override fun toString(): String = "NotificationSessionKey(redacted)"

    companion object {
        fun create(baseUrl: String, deviceId: String, token: String): NotificationSessionKey? {
            val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
            val normalizedDeviceId = deviceId.trim()
            if (normalizedBaseUrl.isBlank() || normalizedDeviceId.isBlank() || token.isBlank()) return null
            return NotificationSessionKey(
                normalizedBaseUrl,
                normalizedDeviceId,
                MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)),
            )
        }
    }
}

internal fun notificationSessionChanged(previous: NotificationSessionKey?, next: NotificationSessionKey?): Boolean = previous != next

internal class NotificationSessionSynchronization {
    private val monitor = Any()
    fun <T> withLock(block: () -> T): T = synchronized(monitor) { block() }
}

internal object AppNotificationSessionSynchronization {
    val instance = NotificationSessionSynchronization()
}
