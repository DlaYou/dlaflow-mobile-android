package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.dlaflow.mobile.app.navigation.MobileAssistantBackAction
import pl.dlaflow.mobile.app.navigation.MobileAssistantOverlayScreen
import pl.dlaflow.mobile.app.navigation.MobileAssistantTab
import pl.dlaflow.mobile.app.navigation.MobileRoute
import pl.dlaflow.mobile.app.navigation.mobileAssistantBackAction

class MobileBackNavigationTest {
    @Test
    fun `android back closes pairing help when no phone session exists`() {
        val action = mobileAssistantBackAction(MobileRoute.Pairing(helpVisible = true))

        assertEquals(MobileAssistantBackAction.CLOSE_PAIRING_HELP, action)
    }

    @Test
    fun `android back closes mandatory pairing name before leaving pairing`() {
        val action = mobileAssistantBackAction(
            MobileRoute.Pairing(helpVisible = false, nameVisible = true),
        )

        assertEquals(MobileAssistantBackAction.CLOSE_PAIRING_NAME, action)
    }

    @Test
    fun `android back closes order detail and keeps orders list visible`() {
        val action = mobileAssistantBackAction(
            MobileRoute.Assistant(
                selectedTab = MobileAssistantTab.ORDERS,
                orderDetailVisible = true,
            ),
        )

        assertEquals(MobileAssistantBackAction.CLOSE_ORDER_DETAIL, action)
    }

    @Test
    fun `android back closes notification overlay before tab detail`() {
        val action = mobileAssistantBackAction(
            MobileRoute.Assistant(
                selectedTab = MobileAssistantTab.ORDERS,
                overlayScreen = MobileAssistantOverlayScreen.NOTIFICATIONS,
                orderDetailVisible = true,
            ),
        )

        assertEquals(MobileAssistantBackAction.CLOSE_OVERLAY, action)
    }

    @Test
    fun `android back falls through when there is no internal screen to close`() {
        val action = mobileAssistantBackAction(
            MobileRoute.Assistant(selectedTab = MobileAssistantTab.ORDERS),
        )

        assertEquals(MobileAssistantBackAction.NONE, action)
    }
}
