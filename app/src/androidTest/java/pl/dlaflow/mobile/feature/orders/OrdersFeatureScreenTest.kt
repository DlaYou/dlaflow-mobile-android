package pl.dlaflow.mobile.feature.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowTheme
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnailLoader
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

@RunWith(AndroidJUnit4::class)
class OrdersFeatureScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val offlineMessage = DlaFlowUiMessage(
        titleRes = R.string.mobile_error_offline_title,
        descriptionRes = R.string.mobile_error_offline_description,
        retryable = true,
    )

    @Test
    fun noAccessHidesLeadContentListAndRetry() {
        setOrders(
            state = OrdersUiState(listState = DlaFlowUiState.NoAccess),
            actions = mutableListOf(),
        )

        composeRule.onAllNodesWithText("Brak dostępu")[0].assertIsDisplayed()
        composeRule.onNodeWithText("lead-content").assertDoesNotExist()
        composeRule.onNodeWithText("Spróbuj ponownie").assertDoesNotExist()
    }

    @Test
    fun ownsAStableFeatureLayoutRoot() {
        setOrders(contentState(), mutableListOf())

        composeRule.onNodeWithTag("orders_feature_root")
            .assertIsDisplayed()
    }

    @Test
    fun largeFontUsesTwoColumnFilterLayout() {
        setOrders(contentState(), mutableListOf(), fontScale = 1.3f)

        val problemsTop = composeRule.onNodeWithText("Problemy")
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val messagesTop = composeRule.onNodeWithText("Wiadomości")
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertTrue(messagesTop > problemsTop)
    }

    @Test
    fun filterHasRadioRoleMinimumTargetAndEmitsTypedAction() {
        val actions = mutableListOf<OrdersAction>()
        setOrders(contentState(), actions)

        val filter = composeRule.onNode(
            hasText("Nowe") and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )
        filter.assertHeightIsAtLeast(48.dp)
        filter.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        filter.performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(OrdersAction.FilterChanged(OrdersFilter.NEW)), actions)
        }
    }

    @Test
    fun orderRowIsAccessibleButtonAndEmitsOpenOrder() {
        val actions = mutableListOf<OrdersAction>()
        setOrders(contentState(), actions)

        val row = composeRule.onNode(hasText("Klient testowy") and hasClickAction())
        row.performScrollTo()
        row.assertHeightIsAtLeast(48.dp)
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        row.performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(OrdersAction.OpenOrder("ORD-1001")), actions)
        }
    }

    @Test
    fun detailLoadingStillShowsCloseAndEmitsCloseDetail() {
        val actions = mutableListOf<OrdersAction>()
        setOrders(
            state = contentState().copy(
                route = OrdersRoute.Detail("ORD-1001"),
                detailState = DlaFlowUiState.Loading,
            ),
            actions = actions,
        )

        composeRule.onNodeWithText("Wróć").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(OrdersAction.CloseDetail), actions)
        }
    }

    @Test
    fun offlineRetainsRowsAndRetryEmitsTypedAction() {
        val actions = mutableListOf<OrdersAction>()
        val content = ordersContent()
        setOrders(
            state = OrdersUiState(
                listState = DlaFlowUiState.Offline(content),
                transientMessage = offlineMessage,
            ),
            actions = actions,
        )

        composeRule.onNodeWithText("Klient testowy").assertIsDisplayed()
        composeRule.onNodeWithText("Brak połączenia").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Spróbuj ponownie").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(OrdersAction.Retry), actions)
        }
    }

    @Test
    fun nonRetryableDetailFailureDoesNotOfferRetry() {
        setOrders(
            state = contentState().copy(
                route = OrdersRoute.Detail("MISSING"),
                detailState = DlaFlowUiState.Error(
                    DlaFlowUiMessage(
                        titleRes = R.string.orders_not_found_title,
                        descriptionRes = R.string.orders_not_found_description,
                        retryable = false,
                    ),
                ),
            ),
            actions = mutableListOf(),
        )

        composeRule.onNodeWithText("Nie znaleziono zamówienia").assertIsDisplayed()
        composeRule.onNodeWithText("Spróbuj ponownie").assertDoesNotExist()
    }

    private fun setOrders(
        state: OrdersUiState,
        actions: MutableList<OrdersAction>,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            DlaFlowTheme(dark = false) { colors ->
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OrdersFeatureScreen(
                            colors = colors,
                            state = state,
                            thumbnailLoader = DlaFlowThumbnailLoader { null },
                            leadContent = { Text("lead-content") },
                            onAction = actions::add,
                        )
                    }
                }
            }
        }
    }

    private fun contentState() = OrdersUiState(
        listState = DlaFlowUiState.Content(ordersContent()),
    )

    private fun ordersContent() = OrdersListContent(
        items = listOf(
            OrdersListItem(
                id = "order-1",
                orderNumber = "ORD-1001",
                amount = 100.0,
                currency = "PLN",
                customer = "Klient testowy",
                channel = "Panel",
                createdAt = "",
                itemCount = 1,
                productSummary = "Produkt testowy",
                paymentStatus = "Opłacone",
                paymentTone = "success",
                phone = "",
                shippingMethod = "Paczkomat",
                status = "new",
                statusTone = "info",
                thumbnailUrl = "",
                badges = OrdersBadges(0, 0, 0),
            ),
        ),
        total = 1,
        nextOffset = null,
    )
}
