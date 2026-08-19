package pl.dlaflow.mobile.feature.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class PhotoTasksStateHolderTest {
    private val message = DlaFlowUiMessage(1, 2, retryable = true)

    @Test
    fun `fallback is visible only until canonical list succeeds`() {
        val holder = PhotoTasksStateHolder()
        holder.setFallback(photoTask("fallback"))

        assertEquals(listOf("fallback"), holder.state.orderedTasks().map { it.id })

        val request = holder.beginRefresh("session-a")!!
        assertTrue(holder.acceptRefreshSuccess(request, emptyList()))
        assertEquals(DlaFlowUiState.Empty, holder.state.tasksState)
        assertTrue(holder.state.orderedTasks().isEmpty())
    }

    @Test
    fun `dashboard fallback never enables mutations before canonical confirmation`() {
        val fallback = photoTask("fallback")
        val fallbackOnly = PhotoTasksUiState(
            tasksState = DlaFlowUiState.Loading,
            fallbackTask = fallback,
        )
        val canonical = fallbackOnly.copy(tasksState = DlaFlowUiState.Content(listOf(fallback)))

        assertTrue(fallbackOnly.orderedTasks().isNotEmpty())
        assertTrue(fallbackOnly.actionEnabledTaskIds().isEmpty())
        assertEquals(setOf(fallback.id), canonical.actionEnabledTaskIds())
        assertTrue(canonical.copy(preparingMediaTaskId = fallback.id).actionEnabledTaskIds().isEmpty())
    }

    @Test
    fun `focused task is ordered first without losing canonical order`() {
        val holder = PhotoTasksStateHolder()
        holder.focus("task-2")
        val request = holder.beginRefresh("session-a")!!

        holder.acceptRefreshSuccess(request, listOf(photoTask("task-1"), photoTask("task-2"), photoTask("task-3")))

        assertEquals(listOf("task-2", "task-1", "task-3"), holder.state.orderedTasks().map { it.id })
    }

    @Test
    fun `media preparation blocks every operation and can be restored or cancelled`() {
        val holder = holderWithTasks("task-1")

        assertTrue(holder.beginMediaSelection("task-1"))
        assertEquals("task-1", holder.state.preparingMediaTaskId)
        assertFalse(holder.beginMediaSelection("task-2"))
        assertNull(holder.beginRefresh("session-a"))
        assertNull(holder.beginCompletion("session-a", "task-1"))
        assertNull(holder.beginUpload("session-a", "task-1", "source-1"))
        assertTrue(holder.restoreMediaSelection("task-1"))
        assertFalse(holder.restoreMediaSelection("task-2"))

        holder.cancelMediaSelection()
        assertNull(holder.state.preparingMediaTaskId)
        assertNotNull(holder.beginCompletion("session-a", "task-1"))
    }

    @Test
    fun `prepared upload clears preparation and starts the same mutation lane`() {
        val holder = holderWithTasks("task-1", "task-2")
        assertTrue(holder.beginMediaSelection("task-1"))

        assertNull(holder.beginPreparedUpload("session-a", "task-2", "source-wrong"))
        val upload = holder.beginPreparedUpload("session-a", "task-1", "source-1")

        assertNotNull(upload)
        assertNull(holder.state.preparingMediaTaskId)
        assertEquals("task-1", holder.state.uploadingTaskId)
        assertNull(holder.beginCompletion("session-a", "task-2"))
    }

    @Test
    fun `new refresh and matching session reject stale callback`() {
        val holder = PhotoTasksStateHolder()
        val stale = holder.beginRefresh("session-a")!!
        val current = holder.beginRefresh("session-a")!!

        assertFalse(holder.acceptRefreshSuccess(stale, listOf(photoTask("old"))))
        assertFalse(holder.acceptRefreshFailure(current.copy(sessionKey = "session-b"), message))
        assertTrue(holder.acceptRefreshSuccess(current, listOf(photoTask("new"))))
        assertEquals("new", holder.state.orderedTasks().single().id)
    }

    @Test
    fun `retained refresh failure exposes refresh retry but mutation failure does not`() {
        val holder = holderWithTasks("task-1")
        val refresh = holder.beginRefresh("session-a")!!

        assertTrue(holder.acceptRefreshFailure(refresh, message))
        assertEquals(message, holder.state.transientMessage)
        assertTrue(holder.state.transientMessageCanRefresh)

        val completion = holder.beginCompletion("session-a", "task-1")!!
        assertTrue(holder.acceptCompletionFailure(completion, message))
        assertEquals(message, holder.state.transientMessage)
        assertFalse(holder.state.transientMessageCanRefresh)
    }

    @Test
    fun `upload and completion share one mutation lane without replacing active work`() {
        val holder = holderWithTasks("task-1", "task-2")
        val upload = holder.beginUpload("session-a", "task-1", "source-old")

        assertNotNull(upload)
        assertNull(holder.beginUpload("session-a", "task-2", "source-new"))
        assertNull(holder.beginCompletion("session-a", "task-2"))
        assertTrue(holder.acceptUploadSuccess(upload!!, photoTask("task-1", mediaCount = 1)))

        val completion = holder.beginCompletion("session-a", "task-2")
        assertNotNull(completion)
        assertNull(holder.beginUpload("session-a", "task-1", "source-after"))
        assertNull(holder.beginCompletion("session-a", "task-1"))
        assertTrue(holder.acceptCompletionSuccess(completion!!, photoTask("task-2", status = PhotoTaskStatus.COMPLETED)))

        assertEquals(listOf("task-1"), holder.state.orderedTasks().map { it.id })
        assertNull(holder.state.uploadingTaskId)
        assertTrue(holder.state.completingTaskIds.isEmpty())
    }

    @Test
    fun `foreign upload and completion responses cannot mutate canonical tasks`() {
        val holder = holderWithTasks("task-1")
        val upload = holder.beginUpload("session-a", "task-1", "source-1")!!
        assertFalse(holder.acceptUploadSuccess(upload, photoTask("foreign", mediaCount = 1)))
        assertTrue(holder.acceptUploadFailure(upload, message))
        val completion = holder.beginCompletion("session-a", "task-1")!!

        assertFalse(holder.acceptCompletionSuccess(completion, photoTask("foreign", status = PhotoTaskStatus.COMPLETED)))
        assertEquals(listOf("task-1"), holder.state.orderedTasks().map { it.id })
    }

    @Test
    fun `mutation invalidates older refresh so stale payload cannot restore product state`() {
        val uploadHolder = holderWithTasks("task-1")
        val staleBeforeUpload = uploadHolder.beginRefresh("session-a")!!
        val upload = uploadHolder.beginUpload("session-a", "task-1", "source-1")!!
        assertTrue(uploadHolder.acceptUploadSuccess(upload, photoTask("task-1", mediaCount = 2)))
        assertFalse(uploadHolder.acceptRefreshSuccess(staleBeforeUpload, listOf(photoTask("task-1", mediaCount = 0))))
        assertEquals(2, uploadHolder.state.orderedTasks().single().mediaCount)

        val completionHolder = holderWithTasks("task-1")
        val staleBeforeCompletion = completionHolder.beginRefresh("session-a")!!
        val completion = completionHolder.beginCompletion("session-a", "task-1")!!
        assertTrue(
            completionHolder.acceptCompletionSuccess(
                completion,
                photoTask("task-1", status = PhotoTaskStatus.COMPLETED),
            ),
        )
        assertFalse(completionHolder.acceptRefreshSuccess(staleBeforeCompletion, listOf(photoTask("task-1"))))
        assertTrue(completionHolder.state.orderedTasks().isEmpty())
    }

    @Test
    fun `mutation blocks refresh retry and invalidates an older dispatch tick`() {
        val holder = holderWithTasks("task-1")
        val staleDispatch = holder.beginDispatch("session-a")!!
        val upload = holder.beginUpload("session-a", "task-1", "source-1")!!

        assertNull(holder.beginRefresh("session-a"))
        assertNull(holder.beginDispatch("session-a"))
        assertTrue(holder.acceptUploadSuccess(upload, photoTask("task-1", mediaCount = 2)))
        assertNull(holder.acceptDispatchSuccess(staleDispatch, photoTask("task-1", mediaCount = 0)))
        assertEquals(2, holder.state.orderedTasks().single().mediaCount)
        assertNotNull(holder.beginRefresh("session-a"))
    }

    @Test
    fun `dispatch is deduplicated and reset starts a new session epoch`() {
        val holder = PhotoTasksStateHolder()
        val first = holder.beginDispatch("session-a")!!
        assertEquals("task-1", holder.acceptDispatchSuccess(first, photoTask("task-1"))?.id)

        val duplicate = holder.beginDispatch("session-a")!!
        assertNull(holder.acceptDispatchSuccess(duplicate, photoTask("task-1")))

        holder.reset()
        val afterReset = holder.beginDispatch("session-b")!!
        assertTrue(afterReset.requestId > duplicate.requestId)
        assertEquals("task-1", holder.acceptDispatchSuccess(afterReset, photoTask("task-1"))?.id)
    }

    @Test
    fun `accepted unauthorized starts one request bound retry and late callback is ignored`() {
        val holder = PhotoTasksStateHolder()
        val unauthorized = holder.beginRefresh("session-a")!!
        assertTrue(holder.acceptRefreshUnauthorized(unauthorized))

        val retry = holder.beginRefreshUnauthorizedRetry(unauthorized)
        assertTrue(retry != null)
        assertTrue(retry!!.requestId > unauthorized.requestId)
        assertNull(holder.beginRefreshUnauthorizedRetry(unauthorized))

        assertTrue(holder.acceptRefreshSuccess(retry, listOf(photoTask("new"))))
        assertFalse(holder.acceptRefreshSessionUnconfirmed(unauthorized, message))
        assertEquals("new", holder.state.orderedTasks().single().id)
    }

    @Test
    fun `reset invalidates every chain without reusing ids`() {
        val holder = PhotoTasksStateHolder()
        val refresh = holder.beginRefresh("session-a")!!
        holder.reset()
        assertFalse(holder.acceptRefreshSuccess(refresh, listOf(photoTask("old"))))
        assertTrue(holder.beginRefresh("session-b")!!.requestId > refresh.requestId)

        holder.reset()
        val upload = holder.beginUpload("session-a", "task-1", "source-1")!!
        holder.reset()
        assertFalse(holder.acceptUploadFailure(upload, message))
        assertTrue(holder.beginUpload("session-b", "task-2", "source-2")!!.requestId > upload.requestId)

        holder.reset()
        val complete = holder.beginCompletion("session-a", "task-1")!!
        holder.reset()
        assertFalse(holder.acceptCompletionFailure(complete, message))
        assertTrue(holder.beginCompletion("session-b", "task-2")!!.requestId > complete.requestId)

        holder.reset()
        val dispatch = holder.beginDispatch("session-a")!!
        holder.reset()
        assertNull(holder.acceptDispatchSuccess(dispatch, photoTask("old")))
        assertTrue(holder.beginDispatch("session-b")!!.requestId > dispatch.requestId)
    }

    private fun holderWithTasks(vararg ids: String) = PhotoTasksStateHolder().also { holder ->
        val request = holder.beginRefresh("session-a")!!
        holder.acceptRefreshSuccess(request, ids.map(::photoTask))
    }
}

internal fun photoTask(
    id: String,
    status: PhotoTaskStatus = PhotoTaskStatus.PENDING,
    mediaCount: Int = 0,
) = ProductPhotoTask(
    id = id,
    productName = "Produkt $id",
    productSku = "SKU-$id",
    status = status,
    mediaCount = mediaCount,
    maxPhotos = 3,
    expiresAt = "2030-01-01T00:00:00Z",
)
