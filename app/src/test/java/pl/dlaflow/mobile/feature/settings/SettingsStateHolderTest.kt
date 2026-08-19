package pl.dlaflow.mobile.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsStateHolderTest {
    @Test
    fun `select and back use one typed route`() {
        val holder = SettingsStateHolder()

        assertTrue(holder.select(SettingsKind.SECURITY))
        assertEquals(SettingsRoute.Detail(SettingsKind.SECURITY), holder.state.route)
        assertTrue(holder.back())
        assertEquals(SettingsRoute.List, holder.state.route)
        assertFalse(holder.back())
    }

    @Test
    fun `reset always removes detail from a replacement session`() {
        val holder = SettingsStateHolder()
        holder.replaceSession(1L)
        holder.select(SettingsKind.CALLER_ID)
        holder.updateCallerIdPhone("123")

        assertTrue(holder.replaceSession(2L))

        assertEquals(SettingsUiState(sessionEpoch = 2L), holder.state)
    }

    @Test
    fun `selecting current detail is an idempotent no op`() {
        val holder = SettingsStateHolder()
        assertTrue(holder.select(SettingsKind.APP))

        assertFalse(holder.select(SettingsKind.APP))
        assertEquals(SettingsRoute.Detail(SettingsKind.APP), holder.state.route)
    }

    @Test
    fun `disconnect confirmation is single flight and stale session completion is rejected`() {
        val holder = SettingsStateHolder()
        holder.replaceSession(10L)
        holder.select(SettingsKind.SECURITY)

        assertTrue(holder.requestDisconnect())
        val request = requireNotNull(holder.beginDisconnect())
        assertFalse(holder.requestDisconnect())
        assertEquals(null, holder.beginDisconnect())

        holder.replaceSession(11L)

        assertFalse(holder.acceptsDisconnectSuccess(request))
        assertFalse(holder.acceptDisconnectFailure(request))
        assertEquals(SettingsUiState(sessionEpoch = 11L), holder.state)
    }

    @Test
    fun `back closes security detail while disconnect remains in flight`() {
        val holder = SettingsStateHolder()
        holder.replaceSession(10L)
        holder.select(SettingsKind.SECURITY)
        holder.requestDisconnect()
        val request = requireNotNull(holder.beginDisconnect())

        assertTrue(holder.back())

        assertEquals(SettingsRoute.List, holder.state.route)
        assertTrue(holder.state.disconnecting)
        assertTrue(holder.acceptsDisconnectSuccess(request))
        assertTrue(holder.acceptDisconnectFailure(request))
    }

    @Test
    fun `disconnect action is disabled while request is in flight`() {
        val holder = SettingsStateHolder()
        holder.requestDisconnect()
        holder.beginDisconnect()

        assertFalse(settingsDisconnectActionEnabled(holder.state))
    }
}
