package pl.dlaflow.mobile

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URLEncoder
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

enum class MobileProductFilter(val queryValue: String, val label: String) {
    ALL("", "Wszystkie"),
    LOW_STOCK("low-stock", "Niski stan"),
    NO_IMAGE("noImage", "Bez zdjęć"),
    HAS_VARIANTS("hasVariants", "Warianty"),
}

data class MobileProductEditableFields(
    val grossPrice: Boolean,
    val stock: Boolean,
)

data class MobileProduct(
    val id: String,
    val name: String,
    val sku: String,
    val ean: String,
    val image: String,
    val thumbnailUrl: String,
    val grossPrice: Double,
    val stock: Int,
    val status: String,
    val currency: String,
    val variantCount: Int,
    val lowStock: Boolean,
    val editableFields: MobileProductEditableFields,
)

data class MobileProductVariantEditableFields(
    val price: Boolean,
    val stock: Boolean,
)

data class MobileProductVariant(
    val id: String,
    val productId: String,
    val name: String,
    val sku: String,
    val ean: String,
    val image: String,
    val thumbnailUrl: String,
    val price: Double,
    val stock: Int,
    val status: String,
    val editableFields: MobileProductVariantEditableFields,
)

data class MobileProductsPage(
    val data: List<MobileProduct>,
    val nextCursor: String?,
    val total: Int,
    val canEdit: Boolean,
)

enum class MobileProductQuickEditField {
    GROSS_PRICE,
    STOCK,
}

enum class MobileVariantQuickEditField {
    PRICE,
    STOCK,
}

data class MobileAssistantKpis(
    val newOrders: Int,
    val toShip: Int,
    val overdueOrProblems: Int,
    val messages: Int,
)

data class MobileAssistantNotification(
    val title: String,
    val description: String,
    val tone: String,
    val source: String,
    val occurredAt: String,
)

data class MobileAssistantTrendPoint(
    val date: String,
    val orders: Int,
    val revenue: Double,
)

data class MobileAssistantCallerIdStatus(
    val enabled: Boolean,
    val label: String,
)

data class MobileAssistantPhotoTask(
    val id: String,
    val productName: String,
    val productSku: String,
    val productImage: String,
    val status: String,
    val mediaCount: Int,
    val maxPhotos: Int,
    val expiresAt: String,
)

data class MobileAssistantDashboard(
    val userName: String,
    val tenantName: String,
    val todayRevenue: Double,
    val revenueChangePercent: Double,
    val kpis: MobileAssistantKpis,
    val notifications: List<MobileAssistantNotification>,
    val activePhotoTask: MobileAssistantPhotoTask?,
    val callerIdStatus: MobileAssistantCallerIdStatus,
    val trend: List<MobileAssistantTrendPoint>,
    val generatedAt: String,
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

    fun getAssistantDashboard(token: String): MobileAssistantDashboard {
        val data = getJson("/api/mobile/assistant/dashboard", token).getJSONObject("data")

        return parseAssistantDashboard(data)
    }

    fun listProducts(token: String, search: String, filter: MobileProductFilter, cursor: String? = null): MobileProductsPage {
        val response = getJson("/api/mobile/products?${buildMobileProductsQuery(search, filter, cursor)}", token)
        val data = response.getJSONArray("data")
        val products = mutableListOf<MobileProduct>()
        for (index in 0 until data.length()) {
            products.add(parseMobileProduct(data.getJSONObject(index)))
        }
        val meta = response.optJSONObject("meta") ?: JSONObject()

        return MobileProductsPage(
            data = products,
            nextCursor = normalizeMobileProductsCursor(meta.optString("nextCursor", "")),
            total = meta.optInt("total", products.size),
            canEdit = meta.optBoolean("canEdit", false),
        )
    }

    fun listProductVariants(token: String, productId: String): List<MobileProductVariant> {
        val data = getJson("/api/mobile/products/${encodePathSegment(productId)}/variants", token).getJSONArray("data")
        val variants = mutableListOf<MobileProductVariant>()
        for (index in 0 until data.length()) {
            variants.add(parseMobileProductVariant(data.getJSONObject(index)))
        }

        return variants
    }

    fun quickEditProduct(token: String, productId: String, field: MobileProductQuickEditField, value: Double): MobileProduct {
        val body = JSONObject()
        when (field) {
            MobileProductQuickEditField.GROSS_PRICE -> body.put("grossPrice", quickEditValueAsBodyNumber(fieldIsStock = false, value = value))
            MobileProductQuickEditField.STOCK -> body.put("stock", quickEditValueAsBodyNumber(fieldIsStock = true, value = value))
        }
        val response = patchJson("/api/mobile/products/${encodePathSegment(productId)}/quick-edit", body, token)

        return parseMobileProduct(response.getJSONObject("data"))
    }

    fun quickEditProductVariant(token: String, productId: String, variantId: String, field: MobileVariantQuickEditField, value: Double): MobileProductVariant {
        val body = JSONObject()
        when (field) {
            MobileVariantQuickEditField.PRICE -> body.put("price", quickEditValueAsBodyNumber(fieldIsStock = false, value = value))
            MobileVariantQuickEditField.STOCK -> body.put("stock", quickEditValueAsBodyNumber(fieldIsStock = true, value = value))
        }
        val response = patchJson(
            path = "/api/mobile/products/${encodePathSegment(productId)}/variants/${encodePathSegment(variantId)}/quick-edit",
            body = body,
            token = token,
        )

        return parseMobileProductVariant(response.getJSONObject("data"))
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

    private fun patchJson(path: String, body: JSONObject, token: String): JSONObject {
        val connection = openConnection(path)
        connection.requestMethod = "PATCH"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Authorization", "Bearer $token")
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

    private fun parseMobileProduct(item: JSONObject): MobileProduct {
        val editable = item.optJSONObject("editableFields") ?: JSONObject()

        return MobileProduct(
            id = item.getString("id"),
            name = item.optString("name", "Produkt").ifBlank { "Produkt" },
            sku = item.optString("sku", ""),
            ean = item.optString("ean", ""),
            image = item.optString("image", ""),
            thumbnailUrl = item.optString("thumbnailUrl", item.optString("image", "")),
            grossPrice = item.getDouble("grossPrice"),
            stock = item.getInt("stock"),
            status = item.optString("status", ""),
            currency = item.optString("currency", "PLN"),
            variantCount = item.optInt("variantCount", 0),
            lowStock = item.optBoolean("lowStock", false),
            editableFields = MobileProductEditableFields(
                grossPrice = editable.optBoolean("grossPrice", false),
                stock = editable.optBoolean("stock", false),
            ),
        )
    }

    private fun parseMobileProductVariant(item: JSONObject): MobileProductVariant {
        val editable = item.optJSONObject("editableFields") ?: JSONObject()

        return MobileProductVariant(
            id = item.getString("id"),
            productId = item.getString("productId"),
            name = item.optString("name", "Wariant").ifBlank { "Wariant" },
            sku = item.optString("sku", ""),
            ean = item.optString("ean", ""),
            image = item.optString("image", ""),
            thumbnailUrl = item.optString("thumbnailUrl", item.optString("image", "")),
            price = item.getDouble("price"),
            stock = item.getInt("stock"),
            status = item.optString("status", ""),
            editableFields = MobileProductVariantEditableFields(
                price = editable.optBoolean("price", false),
                stock = editable.optBoolean("stock", false),
            ),
        )
    }

    private fun parseAssistantDashboard(data: JSONObject): MobileAssistantDashboard {
        val kpis = data.optJSONObject("kpis") ?: JSONObject()
        val callerIdStatus = data.optJSONObject("callerIdStatus") ?: JSONObject()
        val notificationsJson = data.optJSONArray("notifications")
        val notifications = mutableListOf<MobileAssistantNotification>()
        if (notificationsJson != null) {
            for (index in 0 until notificationsJson.length()) {
                val item = notificationsJson.getJSONObject(index)
                notifications.add(
                    MobileAssistantNotification(
                        title = item.optString("title", ""),
                        description = item.optString("description", ""),
                        tone = item.optString("tone", "info"),
                        source = item.optString("source", ""),
                        occurredAt = item.optString("occurredAt", ""),
                    ),
                )
            }
        }

        val trendJson = data.optJSONArray("trend")
        val trend = mutableListOf<MobileAssistantTrendPoint>()
        if (trendJson != null) {
            for (index in 0 until trendJson.length()) {
                val item = trendJson.getJSONObject(index)
                trend.add(
                    MobileAssistantTrendPoint(
                        date = item.optString("date", ""),
                        orders = item.optInt("orders", 0),
                        revenue = item.optDouble("revenue", 0.0),
                    ),
                )
            }
        }

        return MobileAssistantDashboard(
            userName = data.optString("userName", ""),
            tenantName = data.optString("tenantName", ""),
            todayRevenue = data.optDouble("todayRevenue", 0.0),
            revenueChangePercent = data.optDouble("revenueChangePercent", 0.0),
            kpis = MobileAssistantKpis(
                newOrders = kpis.optInt("newOrders", 0),
                toShip = kpis.optInt("toShip", 0),
                overdueOrProblems = kpis.optInt("overdueOrProblems", 0),
                messages = kpis.optInt("messages", 0),
            ),
            notifications = notifications,
            activePhotoTask = data.optJSONObject("activePhotoTask")?.let { parseAssistantPhotoTask(it) },
            callerIdStatus = MobileAssistantCallerIdStatus(
                enabled = callerIdStatus.optBoolean("enabled", false),
                label = callerIdStatus.optString("label", ""),
            ),
            trend = trend,
            generatedAt = data.optString("generatedAt", ""),
        )
    }

    private fun parseAssistantPhotoTask(task: JSONObject): MobileAssistantPhotoTask {
        val product = task.optJSONObject("product") ?: JSONObject()

        return MobileAssistantPhotoTask(
            id = task.optString("id", ""),
            productName = product.optString("name", "Produkt").ifBlank { "Produkt" },
            productSku = product.optString("sku", ""),
            productImage = product.optString("image", ""),
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

    private fun encodePathSegment(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
    }
}
