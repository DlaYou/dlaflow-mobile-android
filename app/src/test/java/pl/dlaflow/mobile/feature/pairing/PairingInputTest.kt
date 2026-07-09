package pl.dlaflow.mobile.feature.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingInputTest {
    @Test
    fun `manual code is formatted and canonicalized`() {
        assertEquals("ABC-123", formatPairingCodeInput("ab c123"))
        assertEquals("ABC-123", canonicalPairingCodeOrNull(" abc 123 "))
        assertNull(canonicalPairingCodeOrNull("AB!-123"))
    }

    @Test
    fun `qr accepts only the existing DlaFlow payload or plain code`() {
        assertEquals("ABC-123", pairingCodeFromQrOrNull("dlaflow-pair:v1:abc-123"))
        assertEquals("ABC-123", pairingCodeFromQrOrNull("ABC123"))
        assertNull(pairingCodeFromQrOrNull("https://example.test/ABC-123"))
    }

    @Test
    fun `device name is trimmed and mandatory`() {
        assertEquals("Magazyn", normalizePairingDeviceName("  Magazyn  "))
        assertEquals(PairingDeviceNameError.REQUIRED, pairingDeviceNameError("   "))
        assertEquals(PairingDeviceNameError.TOO_SHORT, pairingDeviceNameError("A"))
        assertNull(pairingDeviceNameError("Telefon Maćka"))
    }

    @Test
    fun `device name rejects control characters and more than eighty characters`() {
        assertEquals(PairingDeviceNameError.CONTROL_CHARACTER, pairingDeviceNameError("Magazyn\n1"))
        assertEquals(PairingDeviceNameError.TOO_LONG, pairingDeviceNameError("A".repeat(81)))
        assertNull(pairingDeviceNameError("Łódź – pakowanie 1"))
    }

    @Test
    fun `device name length counts Unicode code points without splitting emoji`() {
        assertEquals(PairingDeviceNameError.TOO_SHORT, pairingDeviceNameError("😀"))
        assertNull(pairingDeviceNameError("😀😀"))
        assertNull(pairingDeviceNameError("😀".repeat(80)))
        assertEquals(PairingDeviceNameError.TOO_LONG, pairingDeviceNameError("😀".repeat(81)))

        val limited = limitPairingDeviceNameInput("😀".repeat(82))

        assertEquals(81, limited.codePointCount(0, limited.length))
        assertEquals("😀".repeat(81), limited)
    }
}
