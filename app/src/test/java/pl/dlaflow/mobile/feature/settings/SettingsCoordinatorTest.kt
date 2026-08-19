package pl.dlaflow.mobile.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCoordinatorTest {
    @Test
    fun `route actions stay in holder while platform actions emit one closed effect`() {
        val holder = SettingsStateHolder()
        val effects = mutableListOf<SettingsEffect>()
        val coordinator = SettingsCoordinator(holder, effects::add)
        val content = content()

        coordinator.onAction(SettingsAction.Select(SettingsKind.NOTIFICATIONS), content)
        assertEquals(SettingsRoute.Detail(SettingsKind.NOTIFICATIONS), holder.state.route)
        coordinator.onAction(SettingsAction.OpenNotificationSettings, content)
        coordinator.onAction(SettingsAction.Back, content)
        assertEquals(SettingsRoute.List, holder.state.route)

        coordinator.onAction(SettingsAction.Select(SettingsKind.CALLER_ID), content)
        coordinator.onAction(SettingsAction.CallerIdPhoneChanged(" 123 "), content)
        coordinator.onAction(SettingsAction.EnableCallerId, content)
        coordinator.onAction(SettingsAction.TestCallerId, content)
        coordinator.onAction(SettingsAction.ShowCallerIdPreview, content)
        coordinator.onAction(SettingsAction.Back, content)
        coordinator.onAction(SettingsAction.Select(SettingsKind.APP), content)
        coordinator.onAction(SettingsAction.CheckAppUpdate, content)
        coordinator.onAction(SettingsAction.OpenAppSystemSettings, content)
        coordinator.onAction(SettingsAction.Back, content)
        coordinator.onAction(SettingsAction.Select(SettingsKind.PREFERENCES), content)
        coordinator.onAction(SettingsAction.OpenOverlaySettings, content)

        assertEquals(
            listOf(
                SettingsEffect.OpenNotificationSettings,
                SettingsEffect.CallerIdPhoneChanged(" 123 "),
                SettingsEffect.EnableCallerId,
                SettingsEffect.TestCallerId("123"),
                SettingsEffect.ShowCallerIdPreview,
                SettingsEffect.CheckAppUpdate,
                SettingsEffect.OpenAppSystemSettings,
                SettingsEffect.OpenOverlaySettings,
            ),
            effects,
        )
    }

    @Test
    fun `caller id phone change emits accepted value so host can invalidate stale preview`() {
        val holder = SettingsStateHolder()
        val effects = mutableListOf<SettingsEffect>()
        val coordinator = SettingsCoordinator(holder, effects::add)
        coordinator.onAction(SettingsAction.Select(SettingsKind.CALLER_ID), content())

        coordinator.onAction(SettingsAction.CallerIdPhoneChanged(" +48 123 "), content())

        assertEquals(listOf(SettingsEffect.CallerIdPhoneChanged(" +48 123 ")), effects)
        assertEquals(" +48 123 ", holder.state.callerIdPhone)
    }

    @Test
    fun `notification preference change emits one typed host effect only from notifications detail`() {
        val holder = SettingsStateHolder()
        val effects = mutableListOf<SettingsEffect>()
        val coordinator = SettingsCoordinator(holder, effects::add)
        val content = content()

        coordinator.onAction(SettingsAction.NotificationPreferenceChanged("new_orders", false), content)
        coordinator.onAction(SettingsAction.Select(SettingsKind.NOTIFICATIONS), content)
        coordinator.onAction(SettingsAction.NotificationPreferenceChanged("new_orders", false), content)

        assertEquals(
            listOf(SettingsEffect.NotificationPreferenceChanged("new_orders", false)),
            effects,
        )
    }

    @Test
    fun `reset delegates to route holder without platform effect`() {
        val holder = SettingsStateHolder()
        val effects = mutableListOf<SettingsEffect>()
        val coordinator = SettingsCoordinator(holder, effects::add)
        coordinator.onAction(SettingsAction.Select(SettingsKind.SECURITY), content())

        coordinator.reset()

        assertEquals(SettingsRoute.List, holder.state.route)
        assertEquals(emptyList<SettingsEffect>(), effects)
    }

    @Test
    fun `commands fail closed outside their detail and disconnect emits once after confirmation`() {
        val holder = SettingsStateHolder()
        val effects = mutableListOf<SettingsEffect>()
        val coordinator = SettingsCoordinator(holder, effects::add)
        val content = content()
        coordinator.replaceSession(7L)

        coordinator.onAction(SettingsAction.TestCallerId, content)
        coordinator.onAction(SettingsAction.InstallAppUpdate, content)
        coordinator.onAction(SettingsAction.DisconnectConfirmed, content)
        coordinator.onAction(SettingsAction.DisconnectRequested, content)
        coordinator.onAction(SettingsAction.DisconnectConfirmed, content)
        coordinator.onAction(SettingsAction.DisconnectConfirmed, content)

        val disconnect = effects.single() as SettingsEffect.Disconnect
        assertEquals(SettingsDisconnectRequest(1L, 7L), disconnect.request)
    }

    private fun content() = buildSettingsContent(
        SettingsInput(
            displayName = "Operator",
            userEmail = "operator@example.test",
            tenantName = "DlaFlow",
            deviceName = "Telefon",
            phoneStatusMessage = "Telefon działa normalnie.",
            callerIdLabel = "Do włączenia",
            callerIdPreview = SettingsCallerIdPreview(
                displayName = "Klient",
                phone = "123",
                primaryOrder = SettingsCallerIdOrder("1", "Nowe"),
            ),
            callerIdAvailable = true,
            callerIdOperational = false,
            canAutoOpenTasks = false,
            notificationAllowed = false,
            appVersionName = "0.4.4",
            update = null,
            updateChecking = false,
            updateDownloading = false,
            updateDownloadProgress = 0,
            updateError = "",
            textResolver = testSettingsTextResolver,
        ),
    )
}
