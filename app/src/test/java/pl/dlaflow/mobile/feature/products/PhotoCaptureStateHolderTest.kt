package pl.dlaflow.mobile.feature.products

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoCaptureStateHolderTest {
    @Test
    fun `stale session or source callback cannot consume pending capture`() {
        val holder = PhotoCaptureStateHolder()
        val first = holder.begin("session-a", "task-1", PhotoCaptureKind.GALLERY, "source-a")
        assertNotNull(first)
        assertTrue(holder.clear("source-a"))

        val current = holder.begin("session-b", "task-1", PhotoCaptureKind.GALLERY, "source-b")!!

        assertFalse(holder.matches(first!!))
        assertFalse(holder.matches(current.copy(sessionKey = "session-a")))
        assertFalse(holder.matches(current.copy(sourceId = "source-a")))
        assertTrue(holder.matches(current))
    }

    @Test
    fun `only current source can advance and clear capture`() {
        val holder = PhotoCaptureStateHolder()
        val capture = holder.begin("session-a", "task-1", PhotoCaptureKind.CAMERA, "source-a")!!

        assertFalse(holder.advance(capture.copy(sourceId = "stale"), PhotoCapturePhase.PREPARING))
        assertTrue(holder.advance(capture, PhotoCapturePhase.PREPARING))
        assertTrue(holder.matches(capture.copy(phase = PhotoCapturePhase.PREPARING)))
        assertFalse(holder.clear("stale"))
        assertTrue(holder.clear("source-a"))
        assertNull(holder.pending)
    }
}
