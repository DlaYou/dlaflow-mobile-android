package pl.dlaflow.mobile.feature.products

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.UnknownHostException
import java.util.ArrayDeque
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dlaflow.mobile.core.network.MobileApiException
import pl.dlaflow.mobile.core.state.DlaFlowUiState

class PhotoTasksCoordinatorTest {
    @Test
    fun `active tasks reach state only through main queue`() {
        val harness = PhotoTasksHarness()

        harness.coordinator.refresh("session-a")
        harness.executor.runNext()
        assertEquals(DlaFlowUiState.Loading, harness.holder.state.tasksState)
        harness.mainQueue.runNext()

        assertEquals("task-1", harness.holder.state.orderedTasks().single().id)
    }

    @Test
    fun `media selection is one blocking operation until cancellation`() {
        val harness = PhotoTasksHarness()

        harness.coordinator.handleAction(null, PhotoTasksAction.RequestCamera("task-1"))
        harness.coordinator.handleAction(null, PhotoTasksAction.RequestGallery("task-2"))
        assertFalse(harness.coordinator.complete("session-a", "task-1"))

        assertEquals(listOf(PhotoTasksEffect.LaunchCamera("task-1")), harness.effects)
        assertEquals("task-1", harness.holder.state.preparingMediaTaskId)
        harness.coordinator.mediaSelectionCancelled()
        assertEquals(null, harness.holder.state.preparingMediaTaskId)

        harness.coordinator.handleAction(null, PhotoTasksAction.RequestGallery("task-2"))
        assertEquals(PhotoTasksEffect.LaunchGallery("task-2"), harness.effects.last())
        harness.coordinator.mediaSelectionCancelled()
        assertEquals(PhotoTasksUiState(), harness.holder.state)
    }

    @Test
    fun `prepared selection transitions atomically into upload`() {
        val harness = PhotoTasksHarness(upload = { taskId, _ -> photoTask(taskId, mediaCount = 1) })
        val source = TestPhotoUploadSource()

        harness.coordinator.handleAction(null, PhotoTasksAction.RequestGallery("task-1"))
        assertTrue(harness.coordinator.submitUpload("session-a", "task-1", source))

        assertEquals(null, harness.holder.state.preparingMediaTaskId)
        assertEquals("task-1", harness.holder.state.uploadingTaskId)
        assertEquals(1, harness.executor.size)
    }

    @Test
    fun `invalid prepared source clears selection and exposes controlled error`() {
        val harness = PhotoTasksHarness()
        val source = TestPhotoUploadSource(safeMimeType = "image/heic")

        harness.coordinator.handleAction(null, PhotoTasksAction.RequestGallery("task-1"))
        assertFalse(harness.coordinator.submitUpload("session-a", "task-1", source))

        assertEquals(null, harness.holder.state.preparingMediaTaskId)
        assertEquals(1, source.disposeCount)
        assertTrue(harness.holder.state.transientMessage != null)
    }

    @Test
    fun `upload MIME allow list matches canonical API`() {
        listOf("image/avif", "image/gif", "image/jpeg", "image/png", "image/webp").forEachIndexed { index, mime ->
            val harness = PhotoTasksHarness(upload = { taskId, _ -> photoTask(taskId, mediaCount = 1) })
            val source = TestPhotoUploadSource(sourceId = "allowed-$index", safeMimeType = mime)

            assertTrue(harness.coordinator.submitUpload("session-a", "task-$index", source))
        }

        listOf("image/heic", "image/heif").forEachIndexed { index, mime ->
            val harness = PhotoTasksHarness()
            val source = TestPhotoUploadSource(sourceId = "rejected-$index", safeMimeType = mime)

            assertFalse(harness.coordinator.submitUpload("session-a", "task-$index", source))
            assertEquals(1, source.disposeCount)
        }
    }

    @Test
    fun `first upload 401 reopens repeatable source once and disposes after successful retry`() {
        val auth = MobileApiException(401, "AUTH_REQUIRED", "private")
        var attempts = 0
        val harness = PhotoTasksHarness(upload = { _, source ->
            source.openStream().use { it.read() }
            attempts += 1
            if (attempts == 1) throw auth
            photoTask("task-1", mediaCount = 1)
        })
        val source = TestPhotoUploadSource()

        assertTrue(harness.coordinator.submitUpload("session-a", "task-1", source))
        harness.executor.runNext()
        harness.mainQueue.runNext()
        assertEquals(1, harness.unauthorized.size)
        assertSame(auth, harness.unauthorized.single().error)
        assertEquals(0, source.disposeCount)

        harness.unauthorized.single().onSessionValid()
        harness.unauthorized.single().onSessionValid()
        assertEquals(1, harness.executor.size)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(2, source.openCount)
        assertEquals(1, source.disposeCount)
        assertNullUpload(harness)
    }

    @Test
    fun `second upload 401 is terminal without second confirmation and disposes source`() {
        val auth = MobileApiException(401, "AUTH_REQUIRED", "private")
        val harness = PhotoTasksHarness(upload = { _, source ->
            source.openStream().use { it.read() }
            throw auth
        })
        val source = TestPhotoUploadSource()

        harness.coordinator.submitUpload("session-a", "task-1", source)
        harness.executor.runNext()
        harness.mainQueue.runNext()
        harness.unauthorized.single().onSessionValid()
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(1, harness.unauthorized.size)
        assertEquals(2, source.openCount)
        assertEquals(1, source.disposeCount)
        assertTrue(harness.holder.state.transientMessage != null)
    }

    @Test
    fun `ordinary upload failure disposes source and preserves controlled state`() {
        val source = TestPhotoUploadSource()
        val harness = PhotoTasksHarness(upload = { _, _ -> throw UnknownHostException("private-host") })

        harness.coordinator.submitUpload("session-a", "task-1", source)
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(1, source.disposeCount)
        assertEquals(null, harness.holder.state.uploadingTaskId)
        assertTrue(harness.holder.state.transientMessage != null)
    }

    @Test
    fun `active upload rejects and disposes only the second source without replacing work`() {
        val harness = PhotoTasksHarness(upload = { taskId, _ -> photoTask(taskId, mediaCount = 1) })
        val active = TestPhotoUploadSource(sourceId = "active")
        val rejected = TestPhotoUploadSource(sourceId = "rejected")

        assertTrue(harness.coordinator.submitUpload("session-a", "task-old", active))
        assertFalse(harness.coordinator.submitUpload("session-a", "task-new", rejected))
        assertEquals(0, active.disposeCount)
        assertEquals(1, rejected.disposeCount)
        assertEquals(1, harness.executor.size)

        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(1, active.disposeCount)
        assertEquals(1, rejected.disposeCount)
        assertEquals("task-old", harness.holder.state.orderedTasks().single().id)
    }

    @Test
    fun `active completion rejects a second mutation without scheduling gateway work`() {
        val harness = PhotoTasksHarness()

        assertTrue(harness.coordinator.complete("session-a", "task-1"))
        assertFalse(harness.coordinator.complete("session-a", "task-2"))
        val rejectedUpload = TestPhotoUploadSource(sourceId = "rejected-during-completion")
        assertFalse(harness.coordinator.submitUpload("session-a", "task-2", rejectedUpload))

        assertEquals(1, rejectedUpload.disposeCount)
        assertEquals(1, harness.executor.size)
    }

    @Test
    fun `oversized source is rejected and disposed before gateway`() {
        val harness = PhotoTasksHarness()
        val source = TestPhotoUploadSource(lengthBytes = PHOTO_TASK_UPLOAD_MAX_BYTES + 1)

        assertFalse(harness.coordinator.submitUpload("session-a", "task-1", source))

        assertEquals(0, harness.executor.size)
        assertEquals(1, source.disposeCount)
        assertTrue(harness.holder.state.transientMessage != null)
    }

    @Test
    fun `discard and reset dispose a source exactly once and stale work does not start`() {
        val harness = PhotoTasksHarness()
        val discarded = TestPhotoUploadSource(sourceId = "discarded")
        harness.coordinator.discardUploadSource(discarded)
        assertEquals(1, discarded.disposeCount)

        val active = TestPhotoUploadSource(sourceId = "active")
        harness.coordinator.submitUpload("session-a", "task-1", active)
        harness.coordinator.reset()
        assertEquals(1, active.disposeCount)

        harness.executor.runNext()
        assertEquals(0, harness.mainQueue.size)
        assertEquals(0, harness.tasksChangedCount)
        assertTrue(harness.effects.isEmpty())
        assertEquals(PhotoTasksUiState(), harness.holder.state)
    }

    @Test
    fun `many completed uploads release owners and reset keeps late callback dispose idempotent`() {
        val harness = PhotoTasksHarness(upload = { taskId, _ -> photoTask(taskId, mediaCount = 1) })
        val completed = (1..32).map { index -> TestPhotoUploadSource(sourceId = "source-$index") }

        completed.forEachIndexed { index, source ->
            assertTrue(harness.coordinator.submitUpload("session-a", "task-$index", source))
            harness.executor.runNext()
            harness.mainQueue.runNext()
            assertEquals(1, source.disposeCount)
            assertEquals(0, harness.coordinator.activeUploadSourceCount())
        }

        val late = TestPhotoUploadSource(sourceId = "late-source")
        assertTrue(harness.coordinator.submitUpload("session-a", "late-task", late))
        harness.executor.runNext()
        assertEquals(1, harness.mainQueue.size)
        harness.coordinator.reset()
        assertEquals(1, late.disposeCount)
        assertEquals(0, harness.coordinator.activeUploadSourceCount())
        harness.mainQueue.runNext()

        assertEquals(1, late.disposeCount)
        completed.forEach { assertEquals(1, it.disposeCount) }
    }

    @Test
    fun `reset after active upload disposes once and stale success has no side effects`() {
        val harness = PhotoTasksHarness(upload = { taskId, _ -> photoTask(taskId, mediaCount = 1) })
        val active = TestPhotoUploadSource(sourceId = "active-success")

        harness.coordinator.submitUpload("session-a", "task-1", active)
        harness.executor.runNext()
        assertEquals(1, harness.mainQueue.size)
        assertEquals(0, active.disposeCount)

        harness.coordinator.reset()
        assertEquals(1, active.disposeCount)
        harness.mainQueue.runNext()

        assertEquals(1, active.disposeCount)
        assertEquals(0, harness.tasksChangedCount)
        assertTrue(harness.effects.isEmpty())
        assertEquals(PhotoTasksUiState(), harness.holder.state)
    }

    @Test
    fun `unconfirmed list 401 ends request and late valid callback cannot retry newer session`() {
        val auth = MobileApiException(401, "AUTH_REQUIRED", "private")
        var fail = true
        val harness = PhotoTasksHarness(active = {
            if (fail) throw auth
            listOf(photoTask("new"))
        })
        harness.coordinator.refresh("session-a")
        harness.executor.runNext()
        harness.mainQueue.runNext()
        val event = harness.unauthorized.single()

        fail = false
        harness.coordinator.refresh("session-b")
        harness.executor.runNext()
        harness.mainQueue.runNext()
        event.onSessionUnconfirmed()
        event.onSessionValid()

        assertEquals("new", harness.holder.state.orderedTasks().single().id)
        assertEquals(0, harness.executor.size)
    }

    @Test
    fun `completion removes task and dispatch emits only a new task once`() {
        val harness = PhotoTasksHarness()
        harness.coordinator.refresh("session-a")
        harness.executor.runNext()
        harness.mainQueue.runNext()

        harness.coordinator.complete("session-a", "task-1")
        harness.executor.runNext()
        harness.mainQueue.runNext()
        assertTrue(harness.holder.state.orderedTasks().isEmpty())

        assertTrue(harness.coordinator.pollDispatch("session-a"))
        harness.executor.runNext()
        harness.mainQueue.runNext()
        assertTrue(harness.coordinator.pollDispatch("session-a"))
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertEquals(listOf(PhotoTasksEffect.PresentDispatch(photoTask("dispatch-1"))), harness.effects)
    }

    @Test
    fun `offline refresh is controlled and does not expose exception text`() {
        val harness = PhotoTasksHarness(active = { throw UnknownHostException("private-host") })

        harness.coordinator.refresh("session-a")
        harness.executor.runNext()
        harness.mainQueue.runNext()

        assertTrue(harness.holder.state.tasksState is DlaFlowUiState.Offline)
    }
}

private class PhotoTasksHarness(
    active: () -> List<ProductPhotoTask> = { listOf(photoTask("task-1")) },
    upload: (String, PhotoUploadSource) -> ProductPhotoTask = { taskId, _ -> photoTask(taskId, mediaCount = 1) },
) {
    val holder = PhotoTasksStateHolder()
    val executor = PhotoTasksQueuedExecutor()
    val mainQueue = PhotoTasksMainQueue()
    val effects = mutableListOf<PhotoTasksEffect>()
    val unauthorized = mutableListOf<PhotoTasksUnauthorizedEvent>()
    var tasksChangedCount = 0
    private val gateway = object : PhotoTasksGateway {
        override fun loadActive(token: String): List<ProductPhotoTask> = active()
        override fun upload(token: String, taskId: String, source: PhotoUploadSource): ProductPhotoTask = upload(taskId, source)
        override fun complete(token: String, taskId: String): ProductPhotoTask =
            photoTask(taskId, status = PhotoTaskStatus.COMPLETED)
        override fun loadDispatch(token: String): ProductPhotoTask? = photoTask("dispatch-1")
    }
    val coordinator = PhotoTasksCoordinator(
        stateHolder = holder,
        gateway = gateway,
        executor = executor,
        postToMain = mainQueue::post,
        onEffect = effects::add,
        onUnauthorized = { error, allowRetry, onSessionValid, onSessionUnconfirmed ->
            unauthorized += PhotoTasksUnauthorizedEvent(error, allowRetry, onSessionValid, onSessionUnconfirmed)
        },
        onTasksChanged = { tasksChangedCount += 1 },
    )
}

private data class PhotoTasksUnauthorizedEvent(
    val error: Throwable,
    val allowRetry: Boolean,
    val onSessionValid: () -> Unit,
    val onSessionUnconfirmed: () -> Unit,
)

private class TestPhotoUploadSource(
    override val sourceId: String = "source-1",
    override val lengthBytes: Long = 16,
    override val safeFileName: String = "photo.jpg",
    override val safeMimeType: String = "image/jpeg",
) : PhotoUploadSource {
    var openCount = 0
    var disposeCount = 0
    override fun openStream(): InputStream {
        openCount += 1
        return ByteArrayInputStream(byteArrayOf(1, 2, 3))
    }
    override fun dispose() {
        disposeCount += 1
    }
}

private class PhotoTasksQueuedExecutor : Executor {
    private val tasks = ArrayDeque<Runnable>()
    val size: Int get() = tasks.size
    override fun execute(command: Runnable) {
        tasks.addLast(command)
    }
    fun runNext() = tasks.removeFirst().run()
}

private class PhotoTasksMainQueue {
    private val tasks = ArrayDeque<() -> Unit>()
    val size: Int get() = tasks.size
    fun post(task: () -> Unit) {
        tasks.addLast(task)
    }
    fun runNext() = tasks.removeFirst().invoke()
}

private fun assertNullUpload(harness: PhotoTasksHarness) {
    assertEquals(null, harness.holder.state.uploadingTaskId)
}
