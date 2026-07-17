package pl.dlaflow.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MobileImageDecodePlanTest {
    @Test
    fun `decode plan keeps small thumbnails at native size`() {
        assertEquals(1, mobileImageDecodePlan(width = 512, height = 384)?.inSampleSize)
    }

    @Test
    fun `decode plan samples large but safe image to bounded bitmap`() {
        assertEquals(2, mobileImageDecodePlan(width = 2_000, height = 1_000)?.inSampleSize)
        assertEquals(4, mobileImageDecodePlan(width = 4_096, height = 4_096)?.inSampleSize)
    }

    @Test
    fun `decode plan rejects invalid dimensions and decompression bombs`() {
        assertNull(mobileImageDecodePlan(width = 0, height = 100))
        assertNull(mobileImageDecodePlan(width = 100, height = -1))
        assertNull(mobileImageDecodePlan(width = 20_000, height = 100))
        assertNull(mobileImageDecodePlan(width = 10_000, height = 10_000))
    }
}
