package pl.dlaflow.mobile.feature.pairing

import pl.dlaflow.mobile.MobileSession

internal fun interface PairingGateway {
    fun completePairing(baseUrl: String, submission: PairingSubmission): MobileSession
}
