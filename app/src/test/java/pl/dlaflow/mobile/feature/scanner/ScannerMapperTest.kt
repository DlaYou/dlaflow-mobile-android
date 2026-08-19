package pl.dlaflow.mobile.feature.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import pl.dlaflow.mobile.MobilePackageScanLookupResult
import pl.dlaflow.mobile.MobilePackageScanOrder
import pl.dlaflow.mobile.MobilePackageScanShipment

class ScannerMapperTest {
    @Test
    fun `presentation state and result expose neither scanned code nor transport dto`() {
        val presentationFieldTypes = (
            ScannerUiState::class.java.declaredFields.toList() +
                ScannerLookupResult::class.java.declaredFields.toList()
            ).map { it.type.name }
        val presentationFieldNames = (
            ScannerUiState::class.java.declaredFields.toList() +
                ScannerLookupResult::class.java.declaredFields.toList()
            ).map { it.name.lowercase() }

        assertFalse(presentationFieldNames.any { it.contains("code") })
        assertFalse(presentationFieldTypes.any { it.contains("MobilePackageScan") })
        assertFalse(ScannerLookupResult::class.java.declaredFields.any { it.name == "message" })
    }

    @Test
    fun `matched transport result maps only rendered scanner projections`() {
        val rawTransportMessage = "RAW-SCANNER-CODE-84721"
        val result = transportResult(message = rawTransportMessage).toScannerLookupResult()

        assertEquals(ScannerMatchKind.MATCH, result.kind)
        assertEquals("Zamówienie testowe", result.order?.customer)
        assertEquals("ORDER-1", result.order?.orderNumber)
        assertEquals("Nowe", result.order?.status)
        assertEquals(ScannerShipment("InPost", "Gotowa"), result.shipment)
        assertFalse(result.toString().contains(rawTransportMessage))
    }

    @Test
    fun `ambiguous result stays distinct and retains newest returned projection`() {
        val result = transportResult(ambiguous = true).toScannerLookupResult()

        assertEquals(ScannerMatchKind.AMBIGUOUS, result.kind)
        assertEquals("ORDER-1", result.order?.orderNumber)
    }

    @Test
    fun `no match is successful presentation content with controlled fallback`() {
        val result = MobilePackageScanLookupResult(
            matched = false,
            scannedCode = "SYNTHETIC-NO-MATCH",
            matchType = "",
            message = "",
            order = null,
            shipment = null,
        ).toScannerLookupResult()

        assertEquals(ScannerMatchKind.NO_MATCH, result.kind)
        assertNull(result.order)
        assertNull(result.shipment)
    }

    @Test
    fun `matched flag without a valid order number fails closed`() {
        val missingOrder = runCatching { transportResult(order = null).toScannerLookupResult() }
        val blankOrderNumber = runCatching {
            transportResult(
                order = transportResult().order?.copy(orderNumber = " "),
            ).toScannerLookupResult()
        }

        assertEquals(InvalidScannerResultException::class, missingOrder.exceptionOrNull()!!::class)
        assertEquals(InvalidScannerResultException::class, blankOrderNumber.exceptionOrNull()!!::class)
    }
}

internal fun transportResult(
    ambiguous: Boolean = false,
    message: String = "",
    order: MobilePackageScanOrder? = MobilePackageScanOrder(
        amount = 100.0,
        channel = "Panel",
        currency = "PLN",
        customer = "Zamówienie testowe",
        id = "order-id-1",
        orderNumber = "ORDER-1",
        paymentStatus = "Opłacone",
        phone = "",
        productSummary = "Produkt testowy",
        status = "Nowe",
    ),
    shipment: MobilePackageScanShipment? = MobilePackageScanShipment(
        carrier = "InPost",
        id = "shipment-id-1",
        labelReady = true,
        status = "Gotowa",
        trackingNumber = "SYNTHETIC-CODE",
        trackingUrl = "https://example.invalid/track",
    ),
) = MobilePackageScanLookupResult(
    matched = true,
    ambiguous = ambiguous,
    scannedCode = "SYNTHETIC-CODE",
    matchType = "trackingNumber",
    message = message,
    order = order,
    shipment = shipment,
)
