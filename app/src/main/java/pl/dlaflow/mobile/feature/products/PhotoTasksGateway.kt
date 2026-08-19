package pl.dlaflow.mobile.feature.products

import pl.dlaflow.mobile.MobileApiClient
import pl.dlaflow.mobile.MobilePhotoUploadSource

internal interface PhotoTasksGateway {
    fun loadActive(token: String): List<ProductPhotoTask>
    fun upload(token: String, taskId: String, source: PhotoUploadSource): ProductPhotoTask
    fun complete(token: String, taskId: String): ProductPhotoTask
    fun loadDispatch(token: String): ProductPhotoTask?
}

internal class MobileApiPhotoTasksGateway(
    private val clientProvider: () -> MobileApiClient,
) : PhotoTasksGateway {
    override fun loadActive(token: String): List<ProductPhotoTask> =
        clientProvider().listActivePhotoTasks(token).toActiveProductPhotoTasks()

    override fun upload(token: String, taskId: String, source: PhotoUploadSource): ProductPhotoTask =
        clientProvider().uploadPhotoTaskMedia(
            token = token,
            taskId = taskId,
            source = MobilePhotoUploadSource(
                byteCount = source.lengthBytes,
                openStream = source::openStream,
            ),
            fileName = source.safeFileName,
            mimeType = source.safeMimeType,
        ).toProductPhotoTask(expectedTaskId = taskId)

    override fun complete(token: String, taskId: String): ProductPhotoTask =
        clientProvider().completePhotoTask(token, taskId).toProductPhotoTask(expectedTaskId = taskId)

    override fun loadDispatch(token: String): ProductPhotoTask? =
        clientProvider().getPhotoTaskDispatch(token).toDispatchedPhotoTask()
}
