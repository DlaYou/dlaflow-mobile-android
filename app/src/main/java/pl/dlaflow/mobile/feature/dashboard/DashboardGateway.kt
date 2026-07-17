package pl.dlaflow.mobile.feature.dashboard

import pl.dlaflow.mobile.MobileAssistantDashboard

internal fun interface DashboardGateway {
    fun load(token: String): MobileAssistantDashboard
}
