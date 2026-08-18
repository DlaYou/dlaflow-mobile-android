package pl.dlaflow.mobile.feature.orders

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.dlaflow.mobile.core.designsystem.DlaFlowTheme
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnailLoader
import pl.dlaflow.mobile.core.state.DlaFlowUiState

@RunWith(AndroidJUnit4::class)
class OrdersVisualSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capturesSyntheticListInLight() {
        capture("orders-list-light", dark = false, state = listState(), expectedText = "Anna Testowa")
    }

    @Test
    fun capturesSyntheticListInDark() {
        capture("orders-list-dark", dark = true, state = listState(), expectedText = "Anna Testowa")
    }

    @Test
    fun capturesSyntheticDetailInDark() {
        capture(
            name = "orders-detail-dark",
            dark = true,
            expectedText = "Szczegóły zamówienia",
            state = listState().copy(
                route = OrdersRoute.Detail("ORD-1001"),
                detailState = DlaFlowUiState.Content(detail()),
            ),
        )
    }

    private fun capture(name: String, dark: Boolean, state: OrdersUiState, expectedText: String) {
        composeRule.setContent {
            DlaFlowTheme(dark = dark) { colors ->
                Surface(color = colors.appBg) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OrdersFeatureScreen(
                            colors = colors,
                            state = state,
                            thumbnailLoader = DlaFlowThumbnailLoader { null },
                            leadContent = {},
                            onAction = {},
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithText(expectedText).assertIsDisplayed()
        composeRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = context.resources.configuration
        val fontPercent = (configuration.fontScale * 100).toInt()
        val outputDir = requireNotNull(context.getExternalFilesDir("qa"))
        val output = File(outputDir, "$name-${configuration.screenWidthDp}dp-font$fontPercent.png")
        output.outputStream().use { stream ->
            assertTrue(composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        assertTrue(output.length() > 0)
    }

    private fun listState() = OrdersUiState(
        listState = DlaFlowUiState.Content(
            OrdersListContent(
                items = listOf(
                    row("order-1", "ORD-1001", "Anna Testowa", "Koszulka testowa · 2 szt.", "warning"),
                    row("order-2", "ORD-1002", "Jan Testowy", "Buty testowe", "info"),
                    row("order-3", "ORD-1003", "Firma Przykładowa", "Akcesoria magazynowe", "success"),
                ),
                total = 24,
                nextOffset = 20,
            ),
        ),
    )

    private fun row(id: String, number: String, customer: String, product: String, tone: String) = OrdersListItem(
        id = id,
        orderNumber = number,
        amount = 149.99,
        currency = "PLN",
        customer = customer,
        channel = "Panel",
        createdAt = "2026-07-18T10:00:00Z",
        shippingDeadlineAt = Instant.now().plus(Duration.ofHours(18)).toString(),
        itemCount = 2,
        productSummary = product,
        paymentStatus = "Opłacone",
        paymentTone = "success",
        phone = "+48 100 200 300",
        shippingMethod = "Paczkomat",
        status = "processing",
        statusTone = tone,
        thumbnailUrl = "",
        badges = OrdersBadges(documents = 1, messages = 1, shipments = 1),
    )

    private fun detail() = OrderDetailContent(
        id = "order-1",
        orderNumber = "ORD-1001",
        amount = 149.99,
        currency = "PLN",
        createdAt = "2026-07-18T10:00:00Z",
        shippingDeadlineAt = Instant.now().plus(Duration.ofHours(18)).toString(),
        status = "processing",
        statusTone = "info",
        productSummary = "Koszulka testowa",
        itemCount = 2,
        customer = OrderCustomer("Anna Testowa", "anna@example.invalid", "anna-test", "+48 100 200 300"),
        delivery = OrderDelivery(
            address = OrderAddress("Anna Testowa", "", "WAW01", "Testowa 1", "00-001", "Warszawa", "PL", "+48 100 200 300"),
            method = "Paczkomat",
        ),
        payment = OrderPayment("PLN", "Przelew", 149.99, "Opłacone", "success"),
        items = listOf(OrderItem("item-1", "Koszulka testowa", "SKU-TEST", 2, 149.99, 74.995)),
        shipments = listOf(OrderShipment("shipment-1", "InPost", true, "Nadana", "TEST-TRACK")),
        messages = listOf(OrderMessage("message-1", "Klient", "Proszę o szybką wysyłkę.", "2026-07-18T10:00:00Z")),
        documentsCount = 1,
        internalNotesCount = 0,
        statusHistoryCount = 2,
    )
}
