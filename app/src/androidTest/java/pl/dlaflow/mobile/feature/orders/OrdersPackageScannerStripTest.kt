package pl.dlaflow.mobile.feature.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.dlaflow.mobile.MobilePackageScanLookupResult
import pl.dlaflow.mobile.MobilePackageScanOrder
import pl.dlaflow.mobile.MobilePackageScanShipment
import pl.dlaflow.mobile.MobilePackageScanUiState
import pl.dlaflow.mobile.core.designsystem.DlaFlowTheme

@RunWith(AndroidJUnit4::class)
class OrdersPackageScannerStripTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateRendersNoScannerSurface() {
        setStrip(MobilePackageScanUiState.Empty)
        composeRule.onNodeWithTag("orders_package_scanner_strip").assertDoesNotExist()
    }

    @Test
    fun matchedStateOpensNormalizedOrder() {
        val opened = mutableListOf<String>()
        setStrip(matchedState("ORD-1001"), onOpenOrder = opened::add)

        composeRule.onNodeWithText("#ORD-1001 · Nowe").assertIsDisplayed()
        composeRule.onNodeWithText("Otwórz zamówienie").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.runOnIdle { assertEquals(listOf("ORD-1001"), opened) }
    }

    @Test
    fun failedStateOffersRetryWithoutShowingRawCode() {
        var retries = 0
        setStrip(MobilePackageScanUiState.Failed("PRIVATE-CODE", "Sprawdź internet."), onScanAgain = { retries++ })

        composeRule.onNodeWithText("Nie udało się sprawdzić paczki").assertIsDisplayed()
        composeRule.onNodeWithText("PRIVATE-CODE").assertDoesNotExist()
        composeRule.onNodeWithText("Skanuj ponownie").performClick()
        composeRule.runOnIdle { assertEquals(1, retries) }
    }

    private fun setStrip(
        state: MobilePackageScanUiState,
        onOpenOrder: (String) -> Unit = {},
        onScanAgain: () -> Unit = {},
    ) {
        composeRule.setContent {
            DlaFlowTheme(dark = false) { colors ->
                Column(Modifier.testTag("scanner_test_viewport").fillMaxWidth()) {
                    OrdersPackageScannerStrip(colors, state, onOpenOrder, onScanAgain)
                }
            }
        }
    }

    private fun matchedState(orderNumber: String) = MobilePackageScanUiState.Resolved(
        MobilePackageScanLookupResult(
            matched = true,
            scannedCode = "TRACKING",
            matchType = "trackingNumber",
            message = "",
            order = MobilePackageScanOrder(100.0, "Allegro", "PLN", "Klient", "order-1", orderNumber, "Opłacone", "", "Produkt", "Nowe"),
            shipment = MobilePackageScanShipment("InPost", "shipment-1", true, "Gotowa", "TRACKING", ""),
        ),
    )
}
