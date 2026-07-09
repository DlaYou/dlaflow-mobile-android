package pl.dlaflow.mobile.feature.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingSmokeIntentStateTest {
    @Test
    fun `blank url or code does not create smoke seed`() {
        assertNull(pairingSmokeSeed("", "ABC-123", "Magazyn testowy"))
        assertNull(pairingSmokeSeed("http://10.0.2.2:4000", "", "Magazyn testowy"))
        assertNull(pairingSmokeSeed("http://10.0.2.2:4000", "invalid", "Magazyn testowy"))
    }

    @Test
    fun `missing name opens required name step without auto submit`() {
        val seed = pairingSmokeSeed("http://10.0.2.2:4000", "ABC-123", null)
        assertEquals("ABC-123", seed?.pairingCode)
        assertNull(seed?.deviceName)
    }

    @Test
    fun `valid synthetic name permits controlled auto submit`() {
        val seed = pairingSmokeSeed("http://10.0.2.2:4000", "ABC-123", "  Magazyn testowy  ")
        assertEquals("Magazyn testowy", seed?.deviceName)
    }
}
