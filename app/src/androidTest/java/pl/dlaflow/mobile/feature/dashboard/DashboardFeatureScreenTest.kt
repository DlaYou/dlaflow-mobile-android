package pl.dlaflow.mobile.feature.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowTheme
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

@RunWith(AndroidJUnit4::class)
class DashboardFeatureScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val retryableMessage = DlaFlowUiMessage(
        titleRes = R.string.mobile_error_offline_title,
        descriptionRes = R.string.mobile_error_offline_description,
        retryable = true,
    )

    @Test
    fun errorRetryEmitsRefresh() {
        val actions = mutableListOf<DashboardAction>()
        setDashboard(
            state = DashboardUiState(contentState = DlaFlowUiState.Error(retryableMessage)),
            actions = actions,
        )

        composeRule.onNodeWithText("Ponów").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(DashboardAction.Refresh), actions)
        }
    }

    @Test
    fun offlineRetainsContentShowsNoticeAndRetry() {
        val actions = mutableListOf<DashboardAction>()
        setDashboard(
            state = DashboardUiState(
                contentState = DlaFlowUiState.Offline(dashboardContent()),
                transientMessage = retryableMessage,
            ),
            actions = actions,
        )

        composeRule.onNodeWithText("Brak połączenia").assertIsDisplayed()
        composeRule.onNodeWithText("Przychód dzisiaj").assertIsDisplayed()
        composeRule.onNodeWithText("Ponów").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(DashboardAction.Refresh), actions)
        }
    }

    @Test
    fun noAccessHidesQuickActionsAndRetry() {
        setDashboard(
            state = DashboardUiState(contentState = DlaFlowUiState.NoAccess),
            actions = mutableListOf(),
        )

        composeRule.onNodeWithText("Brak dostępu").assertIsDisplayed()
        composeRule.onNodeWithText("Ponów").assertDoesNotExist()
        composeRule.onNodeWithText("Skanuj paczkę").assertDoesNotExist()
    }

    @Test
    fun refreshingShowsProgressAndRetainsContent() {
        setDashboard(
            state = DashboardUiState(
                contentState = DlaFlowUiState.Content(dashboardContent()),
                isRefreshing = true,
            ),
            actions = mutableListOf(),
        )

        composeRule.onNodeWithText("Odświeżamy dane").assertIsDisplayed()
        composeRule.onNodeWithText("Przychód dzisiaj").assertIsDisplayed()
        composeRule.onNodeWithText("Ponów").assertDoesNotExist()
    }

    @Test
    fun notificationOpenAllIsAccessibleButtonAndEmitsAction() {
        val actions = mutableListOf<DashboardAction>()
        setDashboard(
            state = DashboardUiState(contentState = DlaFlowUiState.Content(dashboardContent())),
            actions = actions,
        )

        val openAll = composeRule.onNode(hasText("Zobacz wszystkie") and hasClickAction())
        openAll.performScrollTo()
        openAll.assertHeightIsAtLeast(48.dp)
        openAll.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        openAll.performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(DashboardAction.OpenNotifications), actions)
        }
    }

    private fun setDashboard(state: DashboardUiState, actions: MutableList<DashboardAction>) {
        composeRule.setContent {
            DlaFlowTheme(dark = false) { colors ->
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DashboardFeatureScreen(
                        colors = colors,
                        sessionUserName = "Operator",
                        state = state,
                        fallbackPhotoTask = null,
                        onAction = actions::add,
                    )
                }
            }
        }
    }

    private fun dashboardContent() = DashboardContent(
        userName = "Operator",
        tenantName = "DlaFlow",
        todayRevenue = 120.0,
        revenueChangePercent = 5.0,
        kpis = DashboardKpis(newOrders = 1, toShip = 2, overdueOrProblems = 0, messages = 3),
        notificationSummary = DashboardNotificationSummary(unreadCount = 1, unreadAttentionCount = 0),
        notifications = emptyList(),
        activePhotoTask = null,
        callerIdStatus = DashboardCallerIdStatus(enabled = true, label = "Aktywny"),
        trend = emptyList(),
        generatedAt = "2026-07-17T10:00:00Z",
    )
}
