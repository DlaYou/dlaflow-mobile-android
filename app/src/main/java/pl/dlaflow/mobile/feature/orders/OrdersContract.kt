package pl.dlaflow.mobile.feature.orders

import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal enum class OrdersFilter {
    ALL,
    NEW,
    TO_SHIP,
    OVERDUE,
    PROBLEMS,
    MESSAGES,
}

internal val visibleOrdersFilters = listOf(
    OrdersFilter.ALL,
    OrdersFilter.NEW,
    OrdersFilter.TO_SHIP,
)

internal data class OrdersQuery(
    val search: String = "",
    val filter: OrdersFilter = OrdersFilter.ALL,
)

internal data class OrdersBadges(
    val documents: Int,
    val messages: Int,
    val shipments: Int,
)

internal data class OrdersListItem(
    val id: String,
    val orderNumber: String,
    val amount: Double,
    val currency: String,
    val customer: String,
    val channel: String,
    val createdAt: String = "",
    val shippingDeadlineAt: String = "",
    val shipmentStatus: String = "",
    val shipmentStage: String = "",
    val shippedAt: String = "",
    val deliveredAt: String = "",
    val itemCount: Int,
    val productSummary: String,
    val paymentStatus: String,
    val paymentTone: String,
    val phone: String,
    val shippingMethod: String,
    val status: String,
    val statusTone: String,
    val statusColor: String = "",
    val thumbnailUrl: String,
    val badges: OrdersBadges,
)

internal data class OrdersListContent(
    val items: List<OrdersListItem>,
    val total: Int,
    val nextOffset: Int?,
)

internal data class OrderCustomer(
    val name: String,
    val email: String,
    val nick: String,
    val phone: String,
)

internal data class OrderAddress(
    val name: String,
    val company: String,
    val pointName: String,
    val street: String,
    val postalCode: String,
    val city: String,
    val country: String,
    val phone: String,
)

internal data class OrderDelivery(
    val address: OrderAddress,
    val method: String,
)

internal data class OrderPayment(
    val currency: String,
    val method: String,
    val paidAmount: Double,
    val status: String,
    val tone: String,
)

internal data class OrderItem(
    val id: String,
    val name: String,
    val sku: String,
    val quantity: Int,
    val lineTotal: Double,
    val unitPrice: Double,
)

internal data class OrderShipment(
    val id: String,
    val carrier: String,
    val labelReady: Boolean,
    val status: String,
    val trackingNumber: String,
    val shippedAt: String = "",
    val deliveredAt: String = "",
    val stage: String = "",
)

internal data class OrderMessage(
    val id: String,
    val author: String,
    val body: String,
    val messageAt: String,
)

internal data class OrderDetailContent(
    val id: String,
    val orderNumber: String,
    val amount: Double,
    val currency: String,
    val createdAt: String = "",
    val shippingDeadlineAt: String = "",
    val status: String,
    val statusTone: String,
    val statusColor: String = "",
    val productSummary: String,
    val itemCount: Int,
    val customer: OrderCustomer,
    val delivery: OrderDelivery,
    val payment: OrderPayment,
    val items: List<OrderItem>,
    val shipments: List<OrderShipment>,
    val messages: List<OrderMessage>,
    val documentsCount: Int,
    val internalNotesCount: Int,
    val statusHistoryCount: Int,
)

internal sealed interface OrdersRoute {
    data object List : OrdersRoute
    data class Detail(val orderNumber: String) : OrdersRoute
}

internal data class OrdersUiState(
    val query: OrdersQuery = OrdersQuery(),
    val listState: DlaFlowUiState<OrdersListContent> = DlaFlowUiState.Loading,
    val route: OrdersRoute = OrdersRoute.List,
    val detailState: DlaFlowUiState<OrderDetailContent>? = null,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val activeListRequestId: Long? = null,
    val activeDetailRequestId: Long? = null,
    val transientMessage: DlaFlowUiMessage? = null,
)

internal fun OrdersUiState.listContentOrNull(): OrdersListContent? = when (val current = listState) {
    is DlaFlowUiState.Content -> current.data
    is DlaFlowUiState.Offline -> current.lastContent
    else -> null
}

internal fun OrdersUiState.detailContentOrNull(): OrderDetailContent? = when (val current = detailState) {
    is DlaFlowUiState.Content -> current.data
    is DlaFlowUiState.Offline -> current.lastContent
    else -> null
}

internal enum class OrdersListLoadMode {
    RESET,
    REFRESH,
    LOAD_MORE,
}

internal data class OrdersListRequest(
    val requestId: Long,
    internal val sessionKey: String,
    val query: OrdersQuery,
    val offset: Int,
    val mode: OrdersListLoadMode,
)

internal data class OrdersDetailRequest(
    val requestId: Long,
    internal val sessionKey: String,
    val orderNumber: String,
)

internal sealed interface OrdersPackageScannerState {
    data object Empty : OrdersPackageScannerState

    data object Loading : OrdersPackageScannerState

    data class Resolved(
        val title: String,
        val supportingText: String,
        val orderStatus: String? = null,
        val orderNumber: String? = null,
        val retryable: Boolean = false,
    ) : OrdersPackageScannerState

    data class Failed(
        val message: String,
        val retryable: Boolean = true,
    ) : OrdersPackageScannerState
}

internal sealed interface OrdersAction {
    data class SearchChanged(val search: String) : OrdersAction
    data class FilterChanged(val filter: OrdersFilter) : OrdersAction
    data object Refresh : OrdersAction
    data object LoadMore : OrdersAction
    data class OpenOrder(val orderNumber: String) : OrdersAction
    data object CloseDetail : OrdersAction
    data object Retry : OrdersAction
}
