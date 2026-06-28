package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class MobileOrdersStateTest {
    @Test
    fun `query all default has only paging parameters`() {
        val query = buildMobileOrdersQuery(
            search = "",
            filter = MobileOrderFilter.ALL,
        )

        assertEquals("limit=20&offset=0", query)
    }

    @Test
    fun `search is trimmed and URL encoded`() {
        val query = buildMobileOrdersQuery(
            search = "  Anna Kowalska #123  ",
            filter = MobileOrderFilter.ALL,
        )

        assertEquals("limit=20&offset=0&search=Anna+Kowalska+%23123", query)
    }

    @Test
    fun `filters map to mobile order query parameters`() {
        assertEquals(
            "limit=20&offset=0&filter=new",
            buildMobileOrdersQuery("", MobileOrderFilter.NEW),
        )
        assertEquals(
            "limit=20&offset=0&filter=to-ship",
            buildMobileOrdersQuery("", MobileOrderFilter.TO_SHIP),
        )
        assertEquals(
            "limit=20&offset=0&filter=problems",
            buildMobileOrdersQuery("", MobileOrderFilter.PROBLEMS),
        )
        assertEquals(
            "limit=20&offset=0&filter=messages",
            buildMobileOrdersQuery("", MobileOrderFilter.MESSAGES),
        )
    }

    @Test
    fun `offset is clamped to zero`() {
        assertEquals(
            "limit=20&offset=0",
            buildMobileOrdersQuery("", MobileOrderFilter.ALL, offset = -40),
        )
    }

    @Test
    fun `next offset normalizer rejects blank and JSON null values`() {
        assertEquals(null, normalizeMobileOrdersNextOffset(null))
        assertEquals(null, normalizeMobileOrdersNextOffset(""))
        assertEquals(null, normalizeMobileOrdersNextOffset("   "))
        assertEquals(null, normalizeMobileOrdersNextOffset("null"))
        assertEquals(null, normalizeMobileOrdersNextOffset(" NULL "))
        assertEquals(40, normalizeMobileOrdersNextOffset("40"))
    }

    @Test
    fun `status labels normalize known business statuses`() {
        assertEquals("Nowe", mobileOrderStatusLabel("new"))
        assertEquals("Do wysyłki", mobileOrderStatusLabel("do wysylki"))
        assertEquals("W realizacji", mobileOrderStatusLabel("w realizacji"))
        assertEquals("Dostarczone", mobileOrderStatusLabel("delivered"))
        assertEquals("Wstrzymane", mobileOrderStatusLabel("wstrzymane"))
    }

    @Test
    fun `tone from API maps to stable UI tone`() {
        assertEquals(MobileOrderUiTone.INFO, mobileOrderUiTone("info"))
        assertEquals(MobileOrderUiTone.WARNING, mobileOrderUiTone("warning"))
        assertEquals(MobileOrderUiTone.SUCCESS, mobileOrderUiTone("success"))
        assertEquals(MobileOrderUiTone.BRAND, mobileOrderUiTone("brand"))
        assertEquals(MobileOrderUiTone.NEUTRAL, mobileOrderUiTone(""))
    }
}
