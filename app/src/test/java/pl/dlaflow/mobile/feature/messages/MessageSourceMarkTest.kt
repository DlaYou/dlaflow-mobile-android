package pl.dlaflow.mobile.feature.messages

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageSourceMarkTest {
    @Test
    fun `known marketplace uses canonical Allegro asset`() {
        val mark = resolveMessageSourceMark("allegro")

        assertEquals(MessageSourceAsset.ALLEGRO, mark.lightAsset)
        assertEquals("Allegro", mark.label)
    }

    @Test
    fun `provider matching is case insensitive and trims whitespace`() {
        val mark = resolveMessageSourceMark("  GMAIL ")

        assertEquals(MessageSourceAsset.GMAIL, mark.lightAsset)
        assertEquals("Gmail", mark.label)
    }

    @Test
    fun `WooCommerce uses its canonical store asset`() {
        val mark = resolveMessageSourceMark("woocommerce")

        assertEquals(MessageSourceAsset.WOOCOMMERCE, mark.lightAsset)
        assertEquals(MessageSourceAsset.WOOCOMMERCE, mark.darkAsset)
    }

    @Test
    fun `unknown provider keeps generic fallback and safe label`() {
        val mark = resolveMessageSourceMark("provider-not-supported")

        assertEquals(null, mark.lightAsset)
        assertEquals(null, mark.darkAsset)
        assertEquals("Nieznane źródło", mark.label)
    }
}
