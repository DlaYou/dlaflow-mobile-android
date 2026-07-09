package pl.dlaflow.mobile.app.navigation

enum class MobileAssistantTab(val label: String, val symbol: String) {
    DASHBOARD("Pulpit", "P"),
    ORDERS("Zamówienia", "Z"),
    PRODUCTS("Produkty", "PR"),
    MESSAGES("Wiadomości", "W"),
    MORE("Więcej", "..."),
}

enum class MobileAssistantOverlayScreen {
    NONE,
    NOTIFICATIONS,
}

enum class MobileAssistantBackAction {
    CLOSE_PAIRING_HELP,
    CLOSE_PAIRING_NAME,
    CLOSE_ORDER_DETAIL,
    CLOSE_OVERLAY,
    NONE,
}

sealed interface MobileRoute {
    data class Pairing(
        val helpVisible: Boolean,
        val nameVisible: Boolean = false,
    ) : MobileRoute

    data class Assistant(
        val selectedTab: MobileAssistantTab,
        val overlayScreen: MobileAssistantOverlayScreen = MobileAssistantOverlayScreen.NONE,
        val orderDetailVisible: Boolean = false,
    ) : MobileRoute
}

fun mobileAssistantBackAction(route: MobileRoute): MobileAssistantBackAction {
    return when (route) {
        is MobileRoute.Pairing -> when {
            route.helpVisible -> MobileAssistantBackAction.CLOSE_PAIRING_HELP
            route.nameVisible -> MobileAssistantBackAction.CLOSE_PAIRING_NAME
            else -> MobileAssistantBackAction.NONE
        }

        is MobileRoute.Assistant -> when {
            route.overlayScreen != MobileAssistantOverlayScreen.NONE -> MobileAssistantBackAction.CLOSE_OVERLAY
            route.selectedTab == MobileAssistantTab.ORDERS && route.orderDetailVisible -> MobileAssistantBackAction.CLOSE_ORDER_DETAIL
            else -> MobileAssistantBackAction.NONE
        }
    }
}
