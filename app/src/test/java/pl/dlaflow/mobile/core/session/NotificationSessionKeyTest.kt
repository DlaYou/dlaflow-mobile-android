package pl.dlaflow.mobile.core.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSessionKeyTest {
    @Test
    fun `session memory resets when token or device changes but not for the same session`() {
        val first = NotificationSessionKey.create(
            baseUrl = "https://panel.example.test/",
            deviceId = "device-a",
            token = "token-a",
        )
        val same = NotificationSessionKey.create(
            baseUrl = "https://panel.example.test",
            deviceId = "device-a",
            token = "token-a",
        )
        val changed = NotificationSessionKey.create(
            baseUrl = "https://panel.example.test",
            deviceId = "device-b",
            token = "token-b",
        )

        assertFalse(notificationSessionChanged(first, same))
        assertTrue(notificationSessionChanged(first, changed))
        assertTrue(notificationSessionChanged(null, changed))
    }
}
