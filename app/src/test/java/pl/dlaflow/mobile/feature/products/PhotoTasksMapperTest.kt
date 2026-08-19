package pl.dlaflow.mobile.feature.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import pl.dlaflow.mobile.MobilePhotoTask
import pl.dlaflow.mobile.MobilePhotoTaskDispatch

class PhotoTasksMapperTest {
    @Test
    fun `transport task maps valid presentation progress`() {
        val mapped = mobileTask(id = "task-1", mediaCount = 2, maxPhotos = 3).toProductPhotoTask()

        assertEquals("task-1", mapped.id)
        assertEquals("Produkt testowy", mapped.productName)
        assertEquals("SKU-TEST", mapped.productSku)
        assertEquals(2, mapped.mediaCount)
        assertEquals(3, mapped.maxPhotos)
    }

    @Test
    fun `unknown status is normalized without exposing transport value`() {
        val mapped = mobileTask(status = "private-backend-value").toProductPhotoTask()

        assertEquals(PhotoTaskStatus.UNKNOWN, mapped.status)
    }

    @Test
    fun `blank identity invalid progress duplicate list and foreign response fail closed`() {
        assertThrows(InvalidPhotoTaskPayloadException::class.java) { mobileTask(id = " ").toProductPhotoTask() }
        assertThrows(InvalidPhotoTaskPayloadException::class.java) { mobileTask(maxPhotos = 0).toProductPhotoTask() }
        assertThrows(InvalidPhotoTaskPayloadException::class.java) { mobileTask(mediaCount = -1).toProductPhotoTask() }
        assertThrows(InvalidPhotoTaskPayloadException::class.java) { mobileTask(mediaCount = 4, maxPhotos = 3).toProductPhotoTask() }
        assertThrows(InvalidPhotoTaskPayloadException::class.java) {
            listOf(mobileTask(id = "duplicate"), mobileTask(id = "duplicate")).toActiveProductPhotoTasks()
        }
        assertThrows(InvalidPhotoTaskPayloadException::class.java) {
            mobileTask(id = "foreign").toProductPhotoTask(expectedTaskId = "expected")
        }
    }

    @Test
    fun `dispatch mapper returns only presentation task`() {
        assertEquals(
            "task-1",
            MobilePhotoTaskDispatch(mobileTask()).toDispatchedPhotoTask()?.id,
        )
        assertNull(MobilePhotoTaskDispatch(null).toDispatchedPhotoTask())
        assertThrows(InvalidPhotoTaskPayloadException::class.java) {
            MobilePhotoTaskDispatch(mobileTask(id = " ")).toDispatchedPhotoTask()
        }
    }
}

internal fun mobileTask(
    id: String = "task-1",
    status: String = "pending",
    mediaCount: Int = 0,
    maxPhotos: Int = 3,
) = MobilePhotoTask(
    id = id,
    productName = "Produkt testowy",
    productSku = "SKU-TEST",
    status = status,
    mediaCount = mediaCount,
    maxPhotos = maxPhotos,
    expiresAt = "2030-01-01T00:00:00Z",
)
