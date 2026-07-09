package pl.dlaflow.mobile

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilePackageScannerTest {
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
