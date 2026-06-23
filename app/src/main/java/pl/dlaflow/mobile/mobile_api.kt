package pl.dlaflow.mobile

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class MobileSession(
    val deviceId: String,
    val deviceName: String,
    val tenantName: String,
    val token: String,
    val userEmail: String,
)

data class MobilePhotoTask(
    val id: String,
    val productName: String,
    val productSku: String,
    val status: String,
    val mediaCount: Int,
    val maxPhotos: Int,
    val expiresAt: String,
)

data class MobilePhotoTaskDispatch(
    val pendingOpenTask: MobilePhotoTask?,
)

data class MobileCallerIdOrder(
    val amount: Double,
    val currency: String,
    val delivery: String,
    val lastEventAt: String,
    val orderNumber: String,
    val paymentStatus: String,
    val productSummary: String,
    val sourceCreatedAt: String,
    val status: String,
)

data class MobileCallerIdLookup(
    val displayName: String,
    val orderCount: Int,
    val phone: String,
    val primaryOrder: MobileCallerIdOrder?,
)

class MobileApiException(
    val statusCode: Int,
    val code: String,
    message: String,
) : IllegalStateException(message)

class MobileApiClient(private val baseUrl: String) {
    fun completePairing(pairingCode: String, deviceName: String): MobileSession {
        val response = postJson(
            path = "/api/mobile/devices/pair/complete",
            body = JSONObject()
                .put("deviceName", deviceName)
                .put("pairingCode", pairingCode)
                .put("platform", "ANDROID"),
            token = null,
        )
        val data = response.getJSONObject("data")
        val device = data.getJSONObject("device")
        val token = data.getString("token")
        val me = getJson("/api/mobile/me", token).getJSONObject("data")

        return MobileSession(
            deviceId = device.getString("id"),
            deviceName = device.optString("name", "Telefon"),
            tenantName = me.getJSONObject("tenant").optString("name", ""),
            token = token,
            userEmail = me.getJSONObject("user").optString("email", ""),
        )
    }

    fun verifySession(token: String): MobileSession {
        val data = getJson("/api/mobile/me", token).getJSONObject("data")
        val device = data.getJSONObject("device")

        return MobileSession(
            deviceId = device.getString("id"),
            deviceName = device.optString("name", "Telefon"),
            tenantName = data.getJSONObject("tenant").optString("name", ""),
            token = token,
            userEmail = data.getJSONObject("user").optString("email", ""),
        )
    }

    fun listActivePhotoTasks(token: String): List<MobilePhotoTask> {
        val data = getJson("/api/mobile/photo-tasks/active", token).getJSONArray("data")
        val tasks = mutableListOf<MobilePhotoTask>()

        for (index in 0 until data.length()) {
            tasks.add(parsePhotoTask(data.getJSONObject(index)))
        }

        return tasks
    }

    fun getPhotoTaskDispatch(token: String): MobilePhotoTaskDispatch {
        val data = getJson("/api/mobile/photo-tasks/dispatch", token).getJSONObject("data")
        val pendingTask = data.optJSONObject("pendingOpenTask")?.let { parsePhotoTask(it) }

        return MobilePhotoTaskDispatch(pendingOpenTask = pendingTask)
    }

    fun uploadPhotoTaskMedia(token: String, taskId: String, imageBytes: ByteArray, fileName: String, mimeType: String): MobilePhotoTask {
        val response = postMultipart(
            path = "/api/mobile/photo-tasks/$taskId/media",
            token = token,
            imageBytes = imageBytes,
            fileName = fileName,
            mimeType = mimeType,
        )
        val data = response.getJSONObject("data")

        return parsePhotoTask(data.getJSONObject("task"))
    }

    fun completePhotoTask(token: String, taskId: String): MobilePhotoTask {
        val response = postJson(
            path = "/api/mobile/photo-tasks/$taskId/complete",
            body = JSONObject(),
            token = token,
        )

        return parsePhotoTask(response.getJSONObject("data"))
    }

    fun lookupCallerId(token: String, phone: String): MobileCallerIdLookup {
        val response = postJson(
            path = "/api/mobile/caller-id/lookup",
            body = JSONObject().put("phone", phone),
            token = token,
        )

        return parseCallerIdLookup(response.getJSONObject("data"))
    }

    private fun getJson(path: String, token: String): JSONObject {
        val connection = openConnection(path)
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $token")

        return readJsonResponse(connection)
    }

    private fun postJson(path: String, body: JSONObject, token: String?): JSONObject {
        val connection = openConnection(path)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        connection.outputStream.use { stream ->
            stream.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        return readJsonResponse(connection)
    }

    private fun postMultipart(path: String, token: String, imageBytes: ByteArray, fileName: String, mimeType: String): JSONObject {
        val boundary = "dlaflow-mobile-${UUID.randomUUID()}"
        val connection = openConnection(path)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        connection.outputStream.use { stream ->
            val head = buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n")
                append("Content-Type: ").append(mimeType).append("\r\n\r\n")
            }.toByteArray(Charsets.UTF_8)
            stream.write(head)
            stream.write(imageBytes)
            stream.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        }

        return readJsonResponse(connection)
    }

    private fun openConnection(path: String): HttpURLConnection {
        val normalizedBaseUrl = baseUrl.trim().removeSuffix("/")
        val url = URL("$normalizedBaseUrl$path")

        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun readJsonResponse(connection: HttpURLConnection): JSONObject {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        }

        if (status !in 200..299) {
            val error = runCatching {
                JSONObject(text).optJSONObject("error")
            }.getOrNull()
            val code = error?.optString("code", "") ?: ""
            val message = runCatching {
                error?.optString("message")
            }.getOrNull().orEmpty()

            throw MobileApiException(status, code, message.ifBlank { "API zwróciło błąd $status." })
        }

        return JSONObject(text)
    }

    private fun parsePhotoTask(task: JSONObject): MobilePhotoTask {
        val product = task.optJSONObject("product")

        return MobilePhotoTask(
            id = task.getString("id"),
            productName = product?.optString("name", "Produkt")?.ifBlank { "Produkt" } ?: "Produkt",
            productSku = product?.optString("sku", "") ?: "",
            status = task.optString("status", ""),
            mediaCount = task.optInt("mediaCount", 0),
            maxPhotos = task.optInt("maxPhotos", 0),
            expiresAt = task.optString("expiresAt", ""),
        )
    }

    private fun parseCallerIdLookup(data: JSONObject): MobileCallerIdLookup {
        val primaryOrder = data.optJSONObject("primaryOrder")?.let { order ->
            MobileCallerIdOrder(
                amount = order.optDouble("amount", 0.0),
                currency = order.optString("currency", "PLN"),
                delivery = order.optString("delivery", ""),
                lastEventAt = order.optString("lastEventAt", ""),
                orderNumber = order.optString("orderNumber", ""),
                paymentStatus = order.optString("paymentStatus", ""),
                productSummary = order.optString("productSummary", ""),
                sourceCreatedAt = order.optString("sourceCreatedAt", ""),
                status = order.optString("status", ""),
            )
        }

        return MobileCallerIdLookup(
            displayName = data.optString("displayName", ""),
            orderCount = data.optInt("orderCount", 0),
            phone = data.optString("phone", ""),
            primaryOrder = primaryOrder,
        )
    }
}
