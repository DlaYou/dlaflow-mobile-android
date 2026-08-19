package pl.dlaflow.mobile.feature.scanner

import pl.dlaflow.mobile.MobileApiClient

internal interface ScannerGateway {
    fun lookup(token: String, code: String): ScannerLookupResult
}

internal class MobileApiScannerGateway(
    private val clientProvider: () -> MobileApiClient,
) : ScannerGateway {
    override fun lookup(token: String, code: String): ScannerLookupResult =
        clientProvider()
            .scanPackage(token = token, code = code, format = "")
            .toScannerLookupResult()
}
