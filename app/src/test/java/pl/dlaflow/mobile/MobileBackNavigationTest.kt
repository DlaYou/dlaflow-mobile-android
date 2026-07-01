package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class MobileBackNavigationTest {
    @Test
    fun `android back closes pairing help when no phone session exists`() {
        val action = mobileAssistantBackAction(
            sessionConnected = false,
            pairingHelpVisible = true,
            selectedTab = MobileAssistantTab.DASHBOARD,
            orderDetailVisible = false,
        )

        assertEquals(MobileAssistantBackAction.CLOSE_PAIRING_HELP, action)
    }

    @Test
    fun `android back closes order detail and keeps orders list visible`() {
        val action = mobileAssistantBackAction(
            sessionConnected = true,
            pairingHelpVisible = false,
            selectedTab = MobileAssistantTab.ORDERS,
            orderDetailVisible = true,
        )

        assertEquals(MobileAssistantBackAction.CLOSE_ORDER_DETAIL, action)
    }

    @Test
    fun `android back falls through when there is no internal screen to close`() {
        val action = mobileAssistantBackAction(
            sessionConnected = true,
            pairingHelpVisible = false,
            selectedTab = MobileAssistantTab.ORDERS,
            orderDetailVisible = false,
        )

        assertEquals(MobileAssistantBackAction.NONE, action)
    }
}
