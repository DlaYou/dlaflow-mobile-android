package pl.dlaflow.mobile.feature.scanner

import pl.dlaflow.mobile.MobilePackageScanLookupResult

internal class InvalidScannerResultException : IllegalStateException()

internal fun MobilePackageScanLookupResult.toScannerLookupResult(): ScannerLookupResult {
    if (!matched) {
        return ScannerLookupResult(
            kind = ScannerMatchKind.NO_MATCH,
            order = null,
            shipment = null,
        )
    }

    val matchedOrder = order ?: throw InvalidScannerResultException()
    val orderNumber = matchedOrder.orderNumber.trim()
    if (orderNumber.isBlank()) throw InvalidScannerResultException()

    return ScannerLookupResult(
        kind = if (ambiguous) ScannerMatchKind.AMBIGUOUS else ScannerMatchKind.MATCH,
        order = ScannerOrder(
            orderNumber = orderNumber,
            customer = matchedOrder.customer,
            status = matchedOrder.status,
        ),
        shipment = shipment?.let { matchedShipment ->
            ScannerShipment(
                carrier = matchedShipment.carrier,
                status = matchedShipment.status,
            )
        },
    )
}
