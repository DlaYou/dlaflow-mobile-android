package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class CallerIdPermissionTest {
    @Test
    fun `caller id permission message explains saved contacts requirement`() {
        assertEquals(
            "Caller ID wymaga zgody na kontakty, żeby działać dla numerów zapisanych w telefonie.",
            callerIdMissingPermissionMessage(needsPhoneState = false, needsContacts = true),
        )
        assertEquals(
            "Caller ID wymaga zgody na telefon i kontakty, żeby pokazywać kartę także dla zapisanych klientów.",
            callerIdMissingPermissionMessage(needsPhoneState = true, needsContacts = true),
        )
    }
}
