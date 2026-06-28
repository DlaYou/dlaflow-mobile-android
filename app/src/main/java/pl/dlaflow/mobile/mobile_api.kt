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

data class MobileOrderBadges(
    val documents: Int,
    val messages: Int,
    val shipments: Int,
)

data class MobileOrderListItem(
    val amount: Double,
    val badges: MobileOrderBadges,
    val channel: String,
    val createdAt: String,
    val currency: String,
    val customer: String,
    val email: String,
    val externalId: String,
    val id: String,
    val itemCount: Int,
    val orderNumber: String,
    val paymentStatus: String,
    val paymentTone: String,
    val phone: String,
    val productSummary: String,
    val shippingMethod: String,
    val status: String,
    val statusTone: String,
    val thumbnailUrl: String,
    val updatedAt: String,
)

data class MobileOrdersPage(
    val data: List<MobileOrderListItem>,
    val count: Int,
    val limit: Int,
    val nextOffset: Int?,
    val offset: Int,
    val total: Int,
)

data class MobileOrderCustomer(
    val email: String,
    val name: String,
    val nick: String,
    val phone: String,
)

data class MobileOrderAddress(
    val city: String,
    val company: String,
    val country: String,
    val name: String,
    val phone: String,
    val pointName: String,
    val postalCode: String,
    val street: String,
)

data class MobileOrderDelivery(
    val address: MobileOrderAddress,
    val method: String,
)

data class MobileOrderPayment(
    val currency: String,
    val dueAmount: Double,
    val method: String,
    val paidAmount: Double,
    val status: String,
    val tone: String,
    val totalAmount: Double,
)

data class MobileOrderItem(
    val currency: String,
    val ean: String,
    val id: String,
    val image: String,
    val lineTotal: Double,
    val name: String,
    val offerId: String,
    val productId: String,
    val quantity: Int,
    val sku: String,
    val unitPrice: Double,
    val variantId: String,
)

data class MobileOrderShipment(
    val carrier: String,
    val createdAt: String,
    val id: String,
    val labelReady: Boolean,
    val status: String,
    val trackingNumber: String,
    val trackingUrl: String,
)

data class MobileOrderDocument(
    val id: String,
    val issuedAt: String,
    val number: String,
    val status: String,
    val type: String,
)

data class MobileOrderMessage(
    val author: String,
    val body: String,
    val direction: String,
    val id: String,
    val messageAt: String,
    val source: String,
    val status: String,
)

data class MobileOrderStatusHistory(
    val changedAt: String,
    val source: String,
    val status: String,
)

data class MobileOrderDetail(
    val amount: Double,
    val billingAddress: MobileOrderAddress,
    val channel: String,
    val createdAt: String,
    val currency: String,
    val customer: MobileOrderCustomer,
    val delivery: MobileOrderDelivery,
    val documents: List<MobileOrderDocument>,
    val externalId: String,
    val id: String,
    val itemCount: Int,
    val items: List<MobileOrderItem>,
    val messages: List<MobileOrderMessage>,
    val internalNotes: List<MobileOrderMessage>,
    val orderNumber: String,
    val payment: MobileOrderPayment,
    val productSummary: String,
    val shipments: List<MobileOrderShipment>,
    val status: String,
    val statusHistory: List<MobileOrderStatusHistory>,
    val statusTone: String,
    val updatedAt: String,
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

    fun checkAppUpdate(token: String, currentVersionCode: Int, currentVersionName: String): MobileAppUpdate? {
        val response = getJson(
            "/api/mobile/app-release/check?versionCode=$currentVersionCode&versionName=${encodeQueryValue(currentVersionName)}",
            token,
        )
        val data = response.getJSONObject("data")
        if (!data.optBoolean("available", false)) {
            return null
        }

        return parseAppUpdate(data)
    }

    fun listOrders(token: String, search: String, filter: MobileOrderFilter, offset: Int = 0): MobileOrdersPage {
        val response = getJson("/api/mobile/orders?${buildMobileOrdersQuery(search, filter, offset)}", token)
        val data = response.getJSONArray("data")
        val orders = mutableListOf<MobileOrderListItem>()
        for (index in 0 until data.length()) {
            orders.add(parseMobileOrderListItem(data.getJSONObject(index)))
        }
        val meta = response.optJSONObject("meta") ?: JSONObject()

        return MobileOrdersPage(
            data = orders,
            count = meta.optInt("count", orders.size),
            limit = meta.optInt("limit", 20),
            nextOffset = normalizeMobileOrdersNextOffset(if (meta.has("nextOffset") && !meta.isNull("nextOffset")) meta.optString("nextOffset") else null),
            offset = meta.optInt("offset", offset.coerceAtLeast(0)),
            total = meta.optInt("total", orders.size),
        )
    }

    fun getOrder(token: String, orderId: String): MobileOrderDetail {
        val response = getJson("/api/mobile/orders/${encodePathSegment(orderId)}", token)

        return parseMobileOrderDetail(response.getJSONObject("data"))
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

    private fun parseMobileOrderListItem(item: JSONObject): MobileOrderListItem {
        val badges = item.optJSONObject("badges") ?: JSONObject()

        return MobileOrderListItem(
            amount = item.optDouble("amount", 0.0),
            badges = MobileOrderBadges(
                documents = badges.optInt("documents", 0),
                messages = badges.optInt("messages", 0),
                shipments = badges.optInt("shipments", 0),
            ),
            channel = item.optString("channel", ""),
            createdAt = item.optString("createdAt", ""),
            currency = item.optString("currency", "PLN"),
            customer = item.optString("customer", "Klient").ifBlank { "Klient" },
            email = item.optString("email", ""),
            externalId = item.optString("externalId", ""),
            id = item.optString("id", item.optString("orderNumber", "")),
            itemCount = item.optInt("itemCount", 0),
            orderNumber = item.optString("orderNumber", ""),
            paymentStatus = item.optString("paymentStatus", ""),
            paymentTone = item.optString("paymentTone", "neutral"),
            phone = item.optString("phone", ""),
            productSummary = item.optString("productSummary", ""),
            shippingMethod = item.optString("shippingMethod", ""),
            status = item.optString("status", ""),
            statusTone = item.optString("statusTone", "neutral"),
            thumbnailUrl = item.optString("thumbnailUrl", ""),
            updatedAt = item.optString("updatedAt", ""),
        )
    }

    private fun parseMobileOrderDetail(item: JSONObject): MobileOrderDetail {
        val customer = item.optJSONObject("customer") ?: JSONObject()
        val delivery = item.optJSONObject("delivery") ?: JSONObject()
        val payment = item.optJSONObject("payment") ?: JSONObject()

        return MobileOrderDetail(
            amount = item.optDouble("amount", 0.0),
            billingAddress = parseMobileOrderAddress(item.optJSONObject("billingAddress") ?: JSONObject()),
            channel = item.optString("channel", ""),
            createdAt = item.optString("createdAt", ""),
            currency = item.optString("currency", "PLN"),
            customer = MobileOrderCustomer(
                email = customer.optString("email", ""),
                name = customer.optString("name", "Klient").ifBlank { "Klient" },
                nick = customer.optString("nick", ""),
                phone = customer.optString("phone", ""),
            ),
            delivery = MobileOrderDelivery(
                address = parseMobileOrderAddress(delivery.optJSONObject("address") ?: JSONObject()),
                method = delivery.optString("method", ""),
            ),
            documents = parseMobileOrderDocuments(item.optJSONArray("documents")),
            externalId = item.optString("externalId", ""),
            id = item.optString("id", item.optString("orderNumber", "")),
            itemCount = item.optInt("itemCount", 0),
            items = parseMobileOrderItems(item.optJSONArray("items")),
            messages = parseMobileOrderMessages(item.optJSONArray("messages")),
            internalNotes = parseMobileOrderMessages(item.optJSONArray("internalNotes")),
            orderNumber = item.optString("orderNumber", ""),
            payment = MobileOrderPayment(
                currency = payment.optString("currency", item.optString("currency", "PLN")),
                dueAmount = payment.optDouble("dueAmount", 0.0),
                method = payment.optString("method", ""),
                paidAmount = payment.optDouble("paidAmount", 0.0),
                status = payment.optString("status", ""),
                tone = payment.optString("tone", "neutral"),
                totalAmount = payment.optDouble("totalAmount", item.optDouble("amount", 0.0)),
            ),
            productSummary = item.optString("productSummary", ""),
            shipments = parseMobileOrderShipments(item.optJSONArray("shipments")),
            status = item.optString("status", ""),
            statusHistory = parseMobileOrderStatusHistory(item.optJSONArray("statusHistory")),
            statusTone = item.optString("statusTone", "neutral"),
            updatedAt = item.optString("updatedAt", ""),
        )
    }

    private fun parseMobileOrderAddress(item: JSONObject): MobileOrderAddress {
        return MobileOrderAddress(
            city = item.optString("city", ""),
            company = item.optString("company", ""),
            country = item.optString("country", ""),
            name = item.optString("name", ""),
            phone = item.optString("phone", ""),
            pointName = item.optString("pointName", ""),
            postalCode = item.optString("postalCode", ""),
            street = item.optString("street", ""),
        )
    }

    private fun parseMobileOrderItems(itemsJson: org.json.JSONArray?): List<MobileOrderItem> {
        val items = mutableListOf<MobileOrderItem>()
        if (itemsJson == null) {
            return items
        }

        for (index in 0 until itemsJson.length()) {
            val item = itemsJson.getJSONObject(index)
            items.add(
                MobileOrderItem(
                    currency = item.optString("currency", "PLN"),
                    ean = item.optString("ean", ""),
                    id = item.optString("id", ""),
                    image = item.optString("image", ""),
                    lineTotal = item.optDouble("lineTotal", 0.0),
                    name = item.optString("name", "Produkt").ifBlank { "Produkt" },
                    offerId = item.optString("offerId", ""),
                    productId = item.optString("productId", ""),
                    quantity = item.optInt("quantity", 0),
                    sku = item.optString("sku", ""),
                    unitPrice = item.optDouble("unitPrice", 0.0),
                    variantId = item.optString("variantId", ""),
                ),
            )
        }

        return items
    }

    private fun parseMobileOrderShipments(shipmentsJson: org.json.JSONArray?): List<MobileOrderShipment> {
        val shipments = mutableListOf<MobileOrderShipment>()
        if (shipmentsJson == null) {
            return shipments
        }

        for (index in 0 until shipmentsJson.length()) {
            val item = shipmentsJson.getJSONObject(index)
            shipments.add(
                MobileOrderShipment(
                    carrier = item.optString("carrier", ""),
                    createdAt = item.optString("createdAt", ""),
                    id = item.optString("id", ""),
                    labelReady = item.optBoolean("labelReady", false),
                    status = item.optString("status", ""),
                    trackingNumber = item.optString("trackingNumber", ""),
                    trackingUrl = item.optString("trackingUrl", ""),
                ),
            )
        }

        return shipments
    }

    private fun parseMobileOrderDocuments(documentsJson: org.json.JSONArray?): List<MobileOrderDocument> {
        val documents = mutableListOf<MobileOrderDocument>()
        if (documentsJson == null) {
            return documents
        }

        for (index in 0 until documentsJson.length()) {
            val item = documentsJson.getJSONObject(index)
            documents.add(
                MobileOrderDocument(
                    id = item.optString("id", ""),
                    issuedAt = item.optString("issuedAt", ""),
                    number = item.optString("number", ""),
                    status = item.optString("status", ""),
                    type = item.optString("type", ""),
                ),
            )
        }

        return documents
    }

    private fun parseMobileOrderMessages(messagesJson: org.json.JSONArray?): List<MobileOrderMessage> {
        val messages = mutableListOf<MobileOrderMessage>()
        if (messagesJson == null) {
            return messages
        }

        for (index in 0 until messagesJson.length()) {
            val item = messagesJson.getJSONObject(index)
            messages.add(
                MobileOrderMessage(
                    author = item.optString("author", ""),
                    body = item.optString("body", ""),
                    direction = item.optString("direction", ""),
                    id = item.optString("id", ""),
                    messageAt = item.optString("messageAt", ""),
                    source = item.optString("source", ""),
                    status = item.optString("status", ""),
                ),
            )
        }

        return messages
    }

    private fun parseMobileOrderStatusHistory(historyJson: org.json.JSONArray?): List<MobileOrderStatusHistory> {
        val history = mutableListOf<MobileOrderStatusHistory>()
        if (historyJson == null) {
            return history
        }

        for (index in 0 until historyJson.length()) {
            val item = historyJson.getJSONObject(index)
            history.add(
                MobileOrderStatusHistory(
                    changedAt = item.optString("changedAt", ""),
                    source = item.optString("source", ""),
                    status = item.optString("status", ""),
                ),
            )
        }

        return history
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

    private fun parseAppUpdate(data: JSONObject): MobileAppUpdate {
        val notesJson = data.optJSONArray("releaseNotes")
        val notes = mutableListOf<String>()
        if (notesJson != null) {
            for (index in 0 until notesJson.length()) {
                notes.add(notesJson.optString(index, "").trim())
            }
        }
        val download = data.optJSONObject("download") ?: JSONObject()

        return MobileAppUpdate(
            currentVersionCode = data.optInt("currentVersionCode", 0),
            currentVersionName = data.optString("currentVersionName", ""),
            downloadUrl = download.optString("url", ""),
            expiresAt = download.optString("expiresAt", ""),
            latestVersionCode = data.optInt("latestVersionCode", 0),
            latestVersionName = data.optString("latestVersionName", ""),
            minSupportedVersionCode = data.optInt("minSupportedVersionCode", 1),
            releaseNotes = notes.filter { it.isNotBlank() },
            releaseTitle = data.optString("releaseTitle", "Nowa wersja DlaFlow").ifBlank { "Nowa wersja DlaFlow" },
            required = data.optBoolean("required", false),
            sha256 = data.optString("sha256", ""),
            sizeBytes = data.optInt("sizeBytes", 0),
            status = MobileAppUpdateStatus.fromApi(data.optString("status", "")),
            updatePriority = data.optString("updatePriority", "normal").ifBlank { "normal" },
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

    private fun encodeQueryValue(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }
}
