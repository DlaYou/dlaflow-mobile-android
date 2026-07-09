package pl.dlaflow.mobile

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilePackageScannerTest {
    @Test
    fun launchPackageScanStartsLookupWhenNoSessionIsAvailable() {
        val action = resolveLaunchPackageScanAction(
            rawCode = " TRK123 ",
            hasActiveSession = false,
            hasSavedSession = false,
        )

        assertEquals(MobileLaunchPackageScanAction.StartLookup("TRK123"), action)
    }

    @Test
    fun launchPackageScanWaitsForSavedSessionVerification() {
        val action = resolveLaunchPackageScanAction(
            rawCode = "TRK123",
            hasActiveSession = false,
            hasSavedSession = true,
        )

        assertEquals(MobileLaunchPackageScanAction.WaitForSession("TRK123"), action)
    }

    @Test
    fun pendingLaunchPackageScanDoesNotConsumeBeforeSavedSessionVerification() {
        val shouldConsume = shouldConsumePendingLaunchPackageScan(
            pendingCode = "TRK123",
            hasActiveSession = false,
            hasSavedSession = true,
        )

        assertFalse(shouldConsume)
    }

    @Test
    fun packageScanUiStateShowsMatchedOrderTitle() {
        val result = MobilePackageScanLookupResult(
            matched = true,
            ambiguous = false,
            scannedCode = "TRK123",
            matchType = "trackingNumber",
            message = "",
            order = MobilePackageScanOrder(
                amount = 129.0,
                channel = "Allegro",
                currency = "PLN",
                customer = "Adam Kowalski",
                id = "order-1",
                orderNumber = "000000123",
                paymentStatus = "Opłacone",
                phone = "+48 501 234 987",
                productSummary = "Bluza Classic",
                status = "Nowe",
            ),
            shipment = MobilePackageScanShipment(
                carrier = "InPost",
                id = "shipment-1",
                labelReady = true,
                status = "Gotowa",
                trackingNumber = "TRK123",
                trackingUrl = "https://example.test/TRK123",
            ),
        )

        val state = MobilePackageScanUiState.Resolved(result)

        assertEquals("Adam Kowalski", state.result.order?.customer)
        assertEquals("InPost", state.result.shipment?.carrier)
    }

    @Test
    fun packageScanResolvedCopyShowsAmbiguousMatchWarning() {
        val result = MobilePackageScanLookupResult(
            matched = true,
            ambiguous = true,
            scannedCode = "TRK123",
            matchType = "trackingNumber",
            message = "",
            order = MobilePackageScanOrder(
                amount = 129.0,
                channel = "Allegro",
                currency = "PLN",
                customer = "Adam Kowalski",
                id = "order-1",
                orderNumber = "000000123",
                paymentStatus = "Opłacone",
                phone = "+48 501 234 987",
                productSummary = "Bluza Classic",
                status = "Nowe",
            ),
            shipment = null,
        )

        val copy = packageScannerResolvedCopy(result)

        assertEquals("Znaleziono kilka możliwych paczek", copy.title)
        assertEquals(
            "Pokazujemy najnowsze pasujące zamówienie. Sprawdź dane przed dalszą obsługą.",
            copy.supportingText,
        )
    }

    @Test
    fun parsesMatchedPackageScanResult() {
        val json = JSONObject(
            """
            {
              "matched": true,
              "scannedCode": "630123456789012345678904",
              "matchType": "trackingNumber",
              "shipment": {
                "id": "shipment-1",
                "carrier": "InPost",
                "labelReady": true,
                "status": "Gotowa",
                "trackingNumber": "630123456789012345678904",
                "trackingUrl": "https://example.test/track"
              },
              "order": {
                "amount": 110,
                "channel": "Allegro",
                "currency": "PLN",
                "customer": "Anna Nowak",
                "id": "order-1",
                "orderNumber": "000000107",
                "paymentStatus": "Opłacone",
                "phone": "+48 600 100 200",
                "productSummary": "Produkt testowy",
                "status": "Nowe"
              }
            }
            """.trimIndent()
        )

        val result = parseMobilePackageScanLookupResult(json)

        assertTrue(result.matched)
        assertEquals("trackingNumber", result.matchType)
        assertEquals("Anna Nowak", result.order?.customer)
        assertEquals("InPost", result.shipment?.carrier)
    }

    @Test
    fun parsesAmbiguousMatchedPackageScanResult() {
        val json = JSONObject(
            """
            {
              "matched": true,
              "ambiguous": true,
              "scannedCode": "630123456789012345678904",
              "matchType": "trackingNumber",
              "shipment": {
                "id": "shipment-1",
                "carrier": "InPost",
                "labelReady": true,
                "status": "Gotowa",
                "trackingNumber": "630123456789012345678904",
                "trackingUrl": "https://example.test/track"
              },
              "order": {
                "amount": 110,
                "channel": "Allegro",
                "currency": "PLN",
                "customer": "Anna Nowak",
                "id": "order-1",
                "orderNumber": "000000107",
                "paymentStatus": "Opłacone",
                "phone": "+48 600 100 200",
                "productSummary": "Produkt testowy",
                "status": "Nowe"
              }
            }
            """.trimIndent()
        )

        val result = parseMobilePackageScanLookupResult(json)

        assertTrue(result.matched)
        assertTrue(result.ambiguous)
        assertEquals("Anna Nowak", result.order?.customer)
    }

    @Test
    fun parsesNoMatchPackageScanResult() {
        val json = JSONObject(
            """
            {
              "matched": false,
              "scannedCode": "UNKNOWN",
              "message": "Nie znaleziono paczki w DlaFlow."
            }
            """.trimIndent()
        )

        val result = parseMobilePackageScanLookupResult(json)

        assertFalse(result.matched)
        assertEquals("UNKNOWN", result.scannedCode)
        assertEquals("Nie znaleziono paczki w DlaFlow.", result.message)
        assertNull(result.order)
        assertNull(result.shipment)
    }
}
