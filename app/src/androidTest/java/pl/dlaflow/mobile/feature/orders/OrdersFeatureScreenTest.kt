package pl.dlaflow.mobile.feature.orders

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun exposesOnlyOperatorFilters() {
        setOrders(contentState(), mutableListOf())

        listOf("Wszystkie", "Nowe", "Do wysyłki").forEach { label ->
            filterNode(label).assertIsDisplayed()
        }
        composeRule.onNodeWithText("Problemy").assertDoesNotExist()
        composeRule.onNodeWithText("Wiadomości").assertDoesNotExist()
    }

    @Test
    fun largeFontUsesTwoColumnThreeFilterLayoutWithoutOverflow() {
        setOrders(
            state = contentState(),
            actions = mutableListOf(),
            screenWidthDp = 360,
            fontScale = 1.3f,
        )

        val allTop = filterNode("Wszystkie")
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val newTop = filterNode("Nowe")
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val toShipTop = filterNode("Do wysyłki")
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertEquals(allTop, newTop, 0f)
        assertTrue(toShipTop > allTop)
        val viewport = composeRule.onNodeWithTag("orders_test_viewport")
            .fetchSemanticsNode()
            .boundsInRoot
        listOf("Wszystkie", "Nowe", "Do wysyłki").forEach { label ->
            val bounds = filterNode(label).fetchSemanticsNode().boundsInRoot
            assertTrue("Filter '$label' extends left of viewport", bounds.left >= viewport.left)
            assertTrue("Filter '$label' extends right of viewport", bounds.right <= viewport.right)
        }
        composeRule.onNodeWithText("Problemy").assertDoesNotExist()
        composeRule.onNodeWithText("Wiadomości").assertDoesNotExist()
    }

    @Test
    fun largeFontWideScreenKeepsThreeColumnFilterLayout() {
        setOrders(
            state = contentState(),
            actions = mutableListOf(),
            screenWidthDp = 600,
            fontScale = 1.3f,
        )

        val newTop = composeRule.onNode(
            hasText("Nowe") and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val shippingTop = composeRule.onNode(
            hasText("Do wysyłki") and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertEquals(newTop, shippingTop, 0f)
    }

    @Test
    fun largeFontNarrowScreenStacksOrderTimingValues() {
        setOrders(
            state = contentState(),
            actions = mutableListOf(),
            screenWidthDp = 360,
            fontScale = 1.3f,
        )

        val orderedTop = composeRule.onNodeWithText("Zamówiono", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val shippingTop = composeRule.onNodeWithText("Wyślij do", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertTrue(shippingTop > orderedTop)
    }

    @Test
    fun orderCardExposesBothApiDrivenStatusesToTalkBack() {
        setOrders(contentState(), mutableListOf(), screenWidthDp = 412)

        composeRule.onNodeWithContentDescription("Realizacja, Nowe").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Płatność, Opłacone").assertIsDisplayed()
    }

    @Test
    fun statusFieldsStackAt360Dp() {
        setOrders(contentState(), mutableListOf(), screenWidthDp = 360)

        val fulfillmentTop = composeRule.onNodeWithContentDescription("Realizacja, Nowe")
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val paymentTop = composeRule.onNodeWithContentDescription("Płatność, Opłacone")
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertTrue(paymentTop > fulfillmentTop)
    }

    @Test
    fun statusFieldsStaySideBySideAt412Dp() {
        setOrders(contentState(), mutableListOf(), screenWidthDp = 412)

        val fulfillmentTop = composeRule.onNodeWithContentDescription("Realizacja, Nowe")
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val paymentTop = composeRule.onNodeWithContentDescription("Płatność, Opłacone")
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertEquals(fulfillmentTop, paymentTop, 0f)
    }

    @Test
    fun statusFieldsRespectActualViewportWithLargeFontWithoutOverflow() {
        val actualWidthDp = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.configuration.screenWidthDp
        setOrders(
            state = OrdersUiState(
                listState = DlaFlowUiState.Content(
                    ordersContent(
                        status = "Gotowe do przekazania przewoźnikowi",
                        paymentStatus = "Płatność przy odbiorze",
                    ),
                ),
            ),
            actions = mutableListOf(),
            fontScale = 1.3f,
        )

        val fulfillment = composeRule.onNodeWithContentDescription(
            "Realizacja, Gotowe do przekazania przewoźnikowi",
        ).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val payment = composeRule.onNodeWithContentDescription(
            "Płatność, Płatność przy odbiorze",
        ).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val viewport = composeRule.onNodeWithTag("orders_test_viewport")
            .fetchSemanticsNode().boundsInRoot

        if (actualWidthDp >= 600) {
            assertEquals(fulfillment.top, payment.top, 0f)
        } else {
            assertTrue(payment.top > fulfillment.top)
        }
        assertTrue(fulfillment.left >= viewport.left)
        assertTrue(payment.right <= viewport.right)
        assertTextHasNoVisualOverflow("Gotowe do przekazania przewoźnikowi")
        assertTextHasNoVisualOverflow("Płatność przy odbiorze")
    }

    @Test
    fun missingShippingDeadlineShowsSafeIntegrationFallback() {
        setOrders(
            state = OrdersUiState(
                listState = DlaFlowUiState.Content(ordersContent(shippingDeadlineAt = "")),
            ),
            actions = mutableListOf(),
        )

        composeRule.onNodeWithText("Brak daty z integracji").assertIsDisplayed()
    }

    @Test
    fun shippedOrderReplacesDeadlineWithCanonicalDispatchDate() {
        setOrders(
            state = OrdersUiState(
                listState = DlaFlowUiState.Content(ordersContent(
                    shipmentStatus = "W trasie",
                    shipmentStage = "transit",
                    shippedAt = "2026-07-18T14:00:00Z",
                )),
            ),
            actions = mutableListOf(),
        )

        composeRule.onNodeWithText("Wysłano", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Wyślij do", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun deliveredOrderWithoutDateShowsSafeIntegrationFallback() {
        setOrders(
            state = OrdersUiState(
                listState = DlaFlowUiState.Content(ordersContent(shipmentStatus = "Dostarczona", shipmentStage = "delivered")),
            ),
            actions = mutableListOf(),
        )

        composeRule.onNodeWithText("Dostarczono", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Brak daty z integracji", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun orderTimingIsVerticalAtNormalWidth() {
        setOrders(contentState(), mutableListOf(), screenWidthDp = 412)

        val orderedTop = composeRule.onNodeWithText("Zamówiono", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val shippingTop = composeRule.onNodeWithText("Wyślij do", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertTrue(shippingTop > orderedTop)
    }

    @Test
    fun orderCardKeepsDeliveryMethodButHidesPhoneNumber() {
        setOrders(
            state = OrdersUiState(
                listState = DlaFlowUiState.Content(ordersContent(phone = "+48 100 200 300")),
            ),
            actions = mutableListOf(),
        )

        composeRule.onNodeWithText("Paczkomat", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("+48 100 200 300", substring = true).assertDoesNotExist()
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
    fun detailDisplaysCanonicalApiStatusWithoutAndroidTranslation() {
        setOrders(
            state = contentState().copy(
                route = OrdersRoute.Detail("ORD-1001"),
                detailState = DlaFlowUiState.Content(orderDetail(status = "mPOS gotowe")),
            ),
            actions = mutableListOf(),
        )

        composeRule.onNodeWithText("mPOS gotowe").assertIsDisplayed()
        composeRule.onNodeWithText("MPOS gotowe").assertDoesNotExist()
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
        screenWidthDp: Int? = null,
    ) {
        composeRule.setContent {
            DlaFlowTheme(dark = false) { colors ->
                val density = LocalDensity.current
                val configuration = Configuration(LocalConfiguration.current).apply {
                    screenWidthDp?.let { this.screenWidthDp = it }
                }
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale),
                    LocalConfiguration provides configuration,
                ) {
                    Column(
                        modifier = Modifier
                            .then(screenWidthDp?.let { Modifier.width(it.dp) } ?: Modifier)
                            .testTag("orders_test_viewport")
                            .verticalScroll(rememberScrollState()),
                    ) {
                        OrdersFeatureScreen(
                            colors = colors,
                            state = state,
                            thumbnailLoader = DlaFlowThumbnailLoader { _, _ -> null },
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

    private fun assertTextHasNoVisualOverflow(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                action(layouts)
            }
        val layout = layouts.single()
        assertFalse(
            "Text '$text' has visual overflow: size=${layout.size}, lines=${layout.lineCount}, " +
                "width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}",
            layout.hasVisualOverflow,
        )
    }

    private fun filterNode(label: String) = composeRule.onNode(
        hasText(label) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
    )

    private fun orderDetail(status: String) = OrderDetailContent(
        id = "order-1",
        orderNumber = "ORD-1001",
        amount = 100.0,
        currency = "PLN",
        createdAt = "2026-07-18T10:00:00Z",
        shippingDeadlineAt = "",
        status = status,
        statusTone = "info",
        productSummary = "Produkt testowy",
        itemCount = 1,
        customer = OrderCustomer("Klient testowy", "", "", "+48 100 200 300"),
        delivery = OrderDelivery(
            address = OrderAddress("Klient testowy", "", "", "", "", "", "", "+48 100 200 300"),
            method = "Paczkomat",
        ),
        payment = OrderPayment("PLN", "", 100.0, "Opłacone", "success"),
        items = emptyList(),
        shipments = emptyList(),
        messages = emptyList(),
        documentsCount = 0,
        internalNotesCount = 0,
        statusHistoryCount = 0,
    )

    private fun ordersContent(
        shippingDeadlineAt: String = Instant.now().plus(Duration.ofHours(18)).toString(),
        shipmentStatus: String = "",
        shipmentStage: String = "",
        shippedAt: String = "",
        deliveredAt: String = "",
        phone: String = "",
        status: String = "Nowe",
        paymentStatus: String = "Opłacone",
    ) = OrdersListContent(
        items = listOf(
            OrdersListItem(
                id = "order-1",
                orderNumber = "ORD-1001",
                amount = 100.0,
                currency = "PLN",
                customer = "Klient testowy",
                channel = "Panel",
                createdAt = "2026-07-18T10:00:00Z",
                shippingDeadlineAt = shippingDeadlineAt,
                shipmentStatus = shipmentStatus,
                shipmentStage = shipmentStage,
                shippedAt = shippedAt,
                deliveredAt = deliveredAt,
                itemCount = 1,
                productSummary = "Produkt testowy",
                paymentStatus = paymentStatus,
                paymentTone = "success",
                phone = phone,
                shippingMethod = "Paczkomat",
                status = status,
                statusTone = "info",
                thumbnailUrl = "",
                badges = OrdersBadges(0, 0, 0),
            ),
        ),
        total = 1,
        nextOffset = null,
    )
}
