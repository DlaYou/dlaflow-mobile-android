package pl.dlaflow.mobile.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTextNormalizerTest {
    @Test
    fun `decodes named decimal and hexadecimal html entities`() {
        assertEquals(
            "Zażółć & odbiór <3",
            normalizeMessageBodyText("Za&#380;&#243;&#322;&#263; &amp; odbi&oacute;r &lt;3"),
        )
    }

    @Test
    fun `removes arbitrary wrappers and declarations without leaking tags`() {
        val normalized = normalizeMessageBodyText("<!DOCTYPE html><FOO>Treść</FOO><bar/> po")

        assertEquals("Treść po", normalized)
        assertFalse(normalized.contains('<'))
        assertFalse(normalized.contains('>'))
    }

    @Test
    fun `does not leak malformed active content`() {
        assertEquals("Przed", normalizeMessageBodyText("Przed<script>alert(1)"))
    }

    @Test
    fun `drops active content subtrees but keeps surrounding message`() {
        assertEquals(
            "Przed\nPo",
            normalizeMessageBodyText("<p>Przed</p><script>ukryte</script><style>.x{}</style><p>Po</p>"),
        )
        assertEquals("Przed\nPo", normalizeMessageBodyText("<p>Przed</p><embed><p>Po</p>"))
    }

    @Test
    fun `repairs common windows1252 mojibake without changing valid unicode`() {
        assertEquals("Dzień dobry, zażółć", normalizeMessageBodyText("DzieÅ„ dobry, zaÅ¼Ã³Å‚Ä‡"))
        assertEquals("Åland i Łódź", normalizeMessageBodyText("Åland i Łódź"))
        assertEquals("Äpfel, Ñandú, São Paulo", normalizeMessageBodyText("Äpfel, Ñandú, São Paulo"))
    }

    @Test
    fun `preview is compact while body retains meaningful line breaks`() {
        val html = "<div>Pierwsza linia</div><br><div>Druga linia</div>"

        assertEquals("Pierwsza linia\nDruga linia", normalizeMessageBodyText(html))
        assertEquals("Pierwsza linia Druga linia", normalizeMessagePreviewText(html))
    }

    @Test
    fun `self closing line breaks and image alt text remain readable`() {
        assertEquals(
            "Pierwsza\nDruga\nZdjęcie produktu",
            normalizeMessageBodyText("Pierwsza<br/><div>Druga</div><br/><img src=\"image\" alt=\"Zdjęcie produktu\" />"),
        )
    }

    @Test
    fun `unknown html boolean attribute does not leak into the message`() {
        assertEquals("Treść", normalizeMessageBodyText("<widget hidden>Treść</widget>"))
    }

    @Test
    fun `plain comparison text remains readable`() {
        val normalized = normalizeMessageBodyText("Warunek: 2 &lt; 3 i 4 &gt; 1")

        assertTrue(normalized.contains("2 < 3"))
        assertTrue(normalized.contains("4 > 1"))
    }
}
