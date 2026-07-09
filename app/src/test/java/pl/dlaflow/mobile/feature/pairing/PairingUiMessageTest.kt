package pl.dlaflow.mobile.feature.pairing

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.dlaflow.mobile.R

class PairingUiMessageTest {
    @Test
    fun `every local feedback has controlled business copy`() {
        assertEquals(R.string.pairing_error_code_invalid, PairingFeedback.INVALID_CODE.messageRes())
        assertEquals(R.string.pairing_error_name_required, PairingFeedback.DEVICE_NAME_REQUIRED.messageRes())
        assertEquals(R.string.pairing_error_code_expired, PairingFeedback.CODE_EXPIRED.messageRes())
    }
}
