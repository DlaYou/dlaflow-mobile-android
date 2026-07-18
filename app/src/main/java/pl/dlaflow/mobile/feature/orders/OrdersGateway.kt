package pl.dlaflow.mobile.feature.orders

import pl.dlaflow.mobile.MobileApiClient

internal interface OrdersGateway {
    fun loadPage(
        token: String,
        query: OrdersQuery,
        offset: Int,
    ): OrdersListContent

    fun loadDetail(token: String, orderNumber: String): OrderDetailContent
}

internal class MobileApiOrdersGateway(
    private val clientProvider: () -> MobileApiClient,
) : OrdersGateway {
    override fun loadPage(
        token: String,
        query: OrdersQuery,
        offset: Int,
    ): OrdersListContent = clientProvider().listOrders(
        token = token,
        search = query.search,
        filter = query.filter.toMobileOrderFilter(),
        offset = offset,
    ).toOrdersListContent()

    override fun loadDetail(token: String, orderNumber: String): OrderDetailContent =
        clientProvider().getOrder(token, orderNumber).toOrderDetailContent()
}
