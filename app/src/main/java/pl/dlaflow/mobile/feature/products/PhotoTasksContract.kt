package pl.dlaflow.mobile.feature.products

import java.io.InputStream
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal const val PHOTO_TASK_UPLOAD_MAX_BYTES = 5L * 1024L * 1024L

internal enum class PhotoTaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    UNKNOWN,
}

internal data class ProductPhotoTask(
    val id: String,
    val productName: String,
    val productSku: String,
    val status: PhotoTaskStatus,
    val mediaCount: Int,
    val maxPhotos: Int,
    val expiresAt: String,
)

/** A repeatable, private source prepared by the host. It is never part of Compose state. */
internal interface PhotoUploadSource {
    val sourceId: String
    val lengthBytes: Long
    val safeFileName: String
    val safeMimeType: String
    fun openStream(): InputStream
    fun dispose()
}

internal data class PhotoTasksUiState(
    val tasksState: DlaFlowUiState<List<ProductPhotoTask>> = DlaFlowUiState.Loading,
    val fallbackTask: ProductPhotoTask? = null,
    val focusedTaskId: String? = null,
    val preparingMediaTaskId: String? = null,
    val uploadingTaskId: String? = null,
    val completingTaskIds: Set<String> = emptySet(),
    val isRefreshing: Boolean = false,
    val activeRefreshRequestId: Long? = null,
    val activeUploadRequestId: Long? = null,
    val activeCompletionRequestId: Long? = null,
    val activeDispatchRequestId: Long? = null,
    val transientMessage: DlaFlowUiMessage? = null,
    val transientMessageCanRefresh: Boolean = false,
)

internal fun PhotoTasksUiState.contentOrNull(): List<ProductPhotoTask>? = when (val current = tasksState) {
    is DlaFlowUiState.Content -> current.data
    is DlaFlowUiState.Offline -> current.lastContent
    else -> null
}

internal val PhotoTasksUiState.hasMutationInFlight: Boolean
    get() = preparingMediaTaskId != null || uploadingTaskId != null || completingTaskIds.isNotEmpty()

internal val PhotoTasksUiState.hasRefreshInFlight: Boolean
    get() = activeRefreshRequestId != null

internal val PhotoTasksUiState.hasBlockingOperationInFlight: Boolean
    get() = hasMutationInFlight || hasRefreshInFlight

internal fun PhotoTasksUiState.actionEnabledTaskIds(): Set<String> =
    if (hasBlockingOperationInFlight) {
        emptySet()
    } else {
        contentOrNull().orEmpty().mapTo(mutableSetOf()) { it.id }
    }

internal fun PhotoTasksUiState.orderedTasks(): List<ProductPhotoTask> {
    val canonical = contentOrNull()
    val visible = when {
        canonical != null -> canonical
        tasksState is DlaFlowUiState.Empty || tasksState is DlaFlowUiState.NoAccess -> emptyList()
        fallbackTask != null -> listOf(fallbackTask)
        else -> emptyList()
    }.distinctBy(ProductPhotoTask::id)
    val focus = focusedTaskId ?: return visible
    return visible.sortedBy { if (it.id == focus) 0 else 1 }
}

internal data class PhotoTaskFocusDecision(
    val focusedTaskId: String?,
    val shouldRefreshTasks: Boolean,
    val statusMessage: String,
)

internal fun choosePhotoTaskFocus(
    activeTaskIds: List<String>,
    dashboardActiveTaskId: String?,
): PhotoTaskFocusDecision {
    val listTaskId = activeTaskIds.firstOrNull { it.isNotBlank() }
    if (listTaskId != null) {
        return PhotoTaskFocusDecision(
            focusedTaskId = listTaskId,
            shouldRefreshTasks = false,
            statusMessage = "Otwieram zadanie zdjęciowe produktu.",
        )
    }

    val dashboardTaskId = dashboardActiveTaskId?.takeIf { it.isNotBlank() }
    if (dashboardTaskId != null) {
        return PhotoTaskFocusDecision(
            focusedTaskId = dashboardTaskId,
            shouldRefreshTasks = false,
            statusMessage = "Otwieram aktywne zadanie zdjęciowe.",
        )
    }

    return PhotoTaskFocusDecision(
        focusedTaskId = null,
        shouldRefreshTasks = true,
        statusMessage = "Sprawdzam, czy panel wysłał zadanie zdjęciowe.",
    )
}

internal data class PhotoTasksRefreshRequest(
    val requestId: Long,
    internal val sessionKey: String,
)

internal data class PhotoTaskUploadRequest(
    val requestId: Long,
    internal val sessionKey: String,
    val taskId: String,
    internal val sourceId: String,
)

internal data class PhotoTaskCompletionRequest(
    val requestId: Long,
    internal val sessionKey: String,
    val taskId: String,
)

internal data class PhotoTaskDispatchRequest(
    val requestId: Long,
    internal val sessionKey: String,
)

internal sealed interface PhotoTasksAction {
    data object Refresh : PhotoTasksAction
    data class Focus(val taskId: String) : PhotoTasksAction
    data class RequestCamera(val taskId: String) : PhotoTasksAction
    data class RequestGallery(val taskId: String) : PhotoTasksAction
    data class Complete(val taskId: String) : PhotoTasksAction
}

internal sealed interface PhotoTasksEffect {
    data class LaunchCamera(val taskId: String) : PhotoTasksEffect
    data class LaunchGallery(val taskId: String) : PhotoTasksEffect
    data class PresentDispatch(val task: ProductPhotoTask) : PhotoTasksEffect
    data object UploadSucceeded : PhotoTasksEffect
    data object CompletionSucceeded : PhotoTasksEffect
    data object OperationFailed : PhotoTasksEffect
}
