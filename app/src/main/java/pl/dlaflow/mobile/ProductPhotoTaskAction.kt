package pl.dlaflow.mobile

data class ProductPhotoTaskActionDecision(
    val focusedTaskId: String?,
    val shouldRefreshTasks: Boolean,
    val statusMessage: String,
)

fun chooseProductPhotoTaskAction(
    activeTaskIds: List<String>,
    dashboardActiveTaskId: String?,
): ProductPhotoTaskActionDecision {
    val listTaskId = activeTaskIds.firstOrNull { it.isNotBlank() }
    if (listTaskId != null) {
        return ProductPhotoTaskActionDecision(
            focusedTaskId = listTaskId,
            shouldRefreshTasks = false,
            statusMessage = "Otwieram zadanie zdjęciowe produktu.",
        )
    }

    val dashboardTaskId = dashboardActiveTaskId?.takeIf { it.isNotBlank() }
    if (dashboardTaskId != null) {
        return ProductPhotoTaskActionDecision(
            focusedTaskId = dashboardTaskId,
            shouldRefreshTasks = false,
            statusMessage = "Otwieram aktywne zadanie zdjęciowe.",
        )
    }

    return ProductPhotoTaskActionDecision(
        focusedTaskId = null,
        shouldRefreshTasks = true,
        statusMessage = "Sprawdzam, czy panel wysłał zadanie zdjęciowe.",
    )
}
