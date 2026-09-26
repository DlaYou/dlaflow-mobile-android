package pl.dlaflow.mobile

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import pl.dlaflow.mobile.core.session.AppNotificationSessionSynchronization
import pl.dlaflow.mobile.core.session.NotificationSessionKey
import pl.dlaflow.mobile.core.session.notificationSessionChanged

class MobileSessionStore(context: Context) {
    private val keyAlias = "dlaflow_mobile_session_token"
    private val preferences = context.getSharedPreferences("dlaflow_mobile_session", Context.MODE_PRIVATE)

    fun readBaseUrl(): String {
        return preferences.getString("base_url", defaultBaseUrl) ?: defaultBaseUrl
    }

    fun readToken(): String {
        val cipherText = preferences.getString("token_cipher", "") ?: ""
        val iv = preferences.getString("token_iv", "") ?: ""

        if (cipherText.isNotBlank() && iv.isNotBlank()) {
            return runCatching {
                decryptToken(cipherText, iv)
            }.getOrDefault("")
        }

        val legacyPlainToken = preferences.getString("token", "") ?: ""
        if (legacyPlainToken.isBlank()) {
            return ""
        }

        val encryptedToken = runCatching {
            encryptToken(legacyPlainToken)
        }.getOrNull()

        if (encryptedToken == null) {
            preferences.edit().remove("token").apply()
            return ""
        }

        preferences.edit()
            .putString("token_cipher", encryptedToken.cipherText)
            .putString("token_iv", encryptedToken.iv)
            .remove("token")
            .apply()

        return legacyPlainToken
    }

    fun readDeviceId(): String {
        return preferences.getString("device_id", "") ?: ""
    }

    fun readSavedSessionOrNull(): MobileSession? {
        val token = readToken().trim()
        val deviceId = readDeviceId().trim()
        if (token.isBlank() || deviceId.isBlank()) {
            return null
        }

        return MobileSession(
            deviceId = deviceId,
            deviceName = preferences.getString("device_name", "Telefon").orEmpty().ifBlank { "Telefon" },
            tenantName = preferences.getString("tenant_name", "").orEmpty(),
            token = token,
            userEmail = preferences.getString("user_email", "").orEmpty(),
        )
    }

    fun saveBaseUrl(baseUrl: String) {
        preferences.edit().putString("base_url", baseUrl.trim()).apply()
    }

    fun saveSession(baseUrl: String, session: MobileSession) {
        AppNotificationSessionSynchronization.instance.withLock {
            val previousSessionKey = NotificationSessionKey.create(
                baseUrl = readBaseUrl(),
                deviceId = readDeviceId(),
                token = readToken(),
            )
            val nextSessionKey = NotificationSessionKey.create(
                baseUrl = baseUrl,
                deviceId = session.deviceId,
                token = session.token,
            )
            val sessionChanged = notificationSessionChanged(previousSessionKey, nextSessionKey)
            val encryptedToken = encryptToken(session.token)
            val editor = preferences.edit()
            if (sessionChanged) {
                editor
                    .remove(shownPanelNotificationIdsKey)
                    .remove(lastBackgroundPhotoTaskIdKey)
                    .remove(notificationPreferencesKey)
            }
            editor
                .putString("base_url", baseUrl.trim())
                .putString("device_id", session.deviceId)
                .putString("device_name", session.deviceName)
                .putString("tenant_name", session.tenantName)
                .putString("token_cipher", encryptedToken.cipherText)
                .putString("token_iv", encryptedToken.iv)
                .remove("token")
                .putString("user_email", session.userEmail)
                .apply()
        }
    }

    fun readUpdateDismissalState(): MobileAppUpdateDismissalState {
        return MobileAppUpdateDismissalState(
            versionCode = preferences.getInt("app_update_dismissed_version_code", 0),
            count = preferences.getInt("app_update_dismiss_count", 0),
        )
    }

    fun saveUpdateDismissalState(state: MobileAppUpdateDismissalState) {
        preferences.edit()
            .putInt("app_update_dismissed_version_code", state.versionCode)
            .putInt("app_update_dismiss_count", state.count)
            .apply()
    }

    fun readLastBackgroundPhotoTaskId(): String {
        return preferences.getString(lastBackgroundPhotoTaskIdKey, "") ?: ""
    }

    fun saveLastBackgroundPhotoTaskId(taskId: String) {
        preferences.edit().putString(lastBackgroundPhotoTaskIdKey, taskId).apply()
    }

    fun readShownPanelNotificationIds(): String {
        return preferences.getString(shownPanelNotificationIdsKey, "") ?: ""
    }

    fun saveShownPanelNotificationIds(value: String) {
        preferences.edit().putString(shownPanelNotificationIdsKey, value).apply()
    }

    fun readNotificationPreferences(): MobileNotificationPreferences {
        return AppNotificationSessionSynchronization.instance.withLock {
            parseMobileNotificationPreferences(
                preferences.getString(notificationPreferencesKey, "").orEmpty(),
            )
        }
    }

    fun saveNotificationPreferences(value: MobileNotificationPreferences) {
        AppNotificationSessionSynchronization.instance.withLock {
            preferences.edit()
                .putString(notificationPreferencesKey, serializeMobileNotificationPreferences(value))
                .apply()
        }
    }

    fun clear() {
        AppNotificationSessionSynchronization.instance.withLock {
            preferences.edit().clear().apply()
        }
    }

    fun clearSession() {
        AppNotificationSessionSynchronization.instance.withLock {
            val baseUrl = readBaseUrl()
            val updateDismissalState = readUpdateDismissalState()
            preferences.edit().clear().putString("base_url", baseUrl).apply()
            preserveUpdateDismissalState(updateDismissalState)
        }
    }

    private fun preserveUpdateDismissalState(updateDismissalState: MobileAppUpdateDismissalState) {
        preferences.edit()
            .putInt("app_update_dismissed_version_code", updateDismissalState.versionCode)
            .putInt("app_update_dismiss_count", updateDismissalState.count)
            .apply()
    }

    private fun encryptToken(token: String): EncryptedToken {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        return EncryptedToken(
            cipherText = Base64.encodeToString(cipher.doFinal(token.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP),
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
        )
    }

    private fun decryptToken(cipherText: String, iv: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
        )

        return String(cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        val existingKey = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val parameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(parameterSpec)

        return keyGenerator.generateKey()
    }
}

private const val defaultBaseUrl = "https://panel.dlayou.pl"
private const val lastBackgroundPhotoTaskIdKey = "last_background_photo_task_id"
private const val shownPanelNotificationIdsKey = "shown_panel_notification_ids"
private const val notificationPreferencesKey = "notification_preferences"

private data class EncryptedToken(
    val cipherText: String,
    val iv: String,
)

internal fun hasShownNotificationId(serialized: String, id: String): Boolean {
    if (id.isBlank()) {
        return false
    }

    return serialized
        .split("|")
        .filter { it.isNotBlank() }
        .contains(id)
}

internal fun rememberShownNotificationId(serialized: String, id: String, maxIds: Int = 50): String {
    if (id.isBlank()) {
        return serialized
    }

    val ids = serialized
        .split("|")
        .filter { it.isNotBlank() && it != id }
        .toMutableList()
    ids.add(0, id)

    return ids.take(maxIds.coerceAtLeast(1)).joinToString("|")
}
