package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.app.navigation.MobileAssistantOverlayScreen
import pl.dlaflow.mobile.app.navigation.MobileAssistantTab
import pl.dlaflow.mobile.core.state.DlaFlowUiState
import pl.dlaflow.mobile.feature.scanner.ScannerLookupResult
import pl.dlaflow.mobile.feature.scanner.ScannerMatchKind
import pl.dlaflow.mobile.feature.scanner.ScannerOrder
import pl.dlaflow.mobile.feature.scanner.ScannerShipment
import pl.dlaflow.mobile.feature.scanner.ScannerUiState

class MobilePackageScannerTest {
    @Test
    fun packageScannerHeaderActionIsLimitedToVisibleOrdersContext() {
        assertTrue(shouldShowPackageScannerHeaderAction(MobileAssistantTab.ORDERS, MobileAssistantOverlayScreen.NONE))
        assertFalse(shouldShowPackageScannerHeaderAction(MobileAssistantTab.DASHBOARD, MobileAssistantOverlayScreen.NONE))
        assertFalse(shouldShowPackageScannerHeaderAction(MobileAssistantTab.ORDERS, MobileAssistantOverlayScreen.NOTIFICATIONS))
    }

    @Test
    fun scannerPresentationKeepsMatchedOrderProjectionAndAmbiguousCopy() {
        val result = ScannerLookupResult(
            kind = ScannerMatchKind.AMBIGUOUS,
            order = ScannerOrder("000000123", "Adam Kowalski", "Nowe"),
            shipment = ScannerShipment("InPost", "Gotowa"),
        )

        val copy = packageScannerResolvedCopy(result)
        val state = ScannerUiState(lookupState = DlaFlowUiState.Content(result))

        assertEquals("Znaleziono kilka możliwych paczek", copy.title)
        assertEquals("Adam Kowalski", result.order?.customer)
        assertEquals("InPost", result.shipment?.carrier)
        assertEquals("#000000123 · Nowe", state.toOrdersPackageScannerState().let { resolved ->
            (resolved as pl.dlaflow.mobile.feature.orders.OrdersPackageScannerState.Resolved).orderStatus
        })
    }

    @Test
    fun scannerNoMatchAndLoadingRemainCompactStripStates() {
        val noMatch = ScannerUiState(
            lookupState = DlaFlowUiState.Content(
                ScannerLookupResult(ScannerMatchKind.NO_MATCH, order = null, shipment = null),
            ),
        )

        assertTrue(noMatch.toOrdersPackageScannerState() is pl.dlaflow.mobile.feature.orders.OrdersPackageScannerState.Resolved)
        assertEquals(
            pl.dlaflow.mobile.feature.orders.OrdersPackageScannerState.Loading,
            ScannerUiState(lookupState = DlaFlowUiState.Loading).toOrdersPackageScannerState(),
        )
    }
}
