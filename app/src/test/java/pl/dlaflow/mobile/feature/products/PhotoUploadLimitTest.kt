package pl.dlaflow.mobile.feature.products

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.dlaflow.mobile.MOBILE_PHOTO_UPLOAD_MAX_BYTES

class PhotoUploadLimitTest {
    @Test
    fun `mobile product photo uploads allow the same 50 MiB limit as panel product media`() {
        assertEquals(50L * 1024L * 1024L, PHOTO_TASK_UPLOAD_MAX_BYTES)
        assertEquals(50L * 1024L * 1024L, MOBILE_PHOTO_UPLOAD_MAX_BYTES)
    }
}
