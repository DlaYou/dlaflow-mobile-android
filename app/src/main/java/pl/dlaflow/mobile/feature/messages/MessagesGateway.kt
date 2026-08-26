package pl.dlaflow.mobile.feature.messages

import pl.dlaflow.mobile.MobileApiClient

internal interface MessagesGateway {
    fun loadPage(token: String, query: MessagesQuery, cursor: String?): MessagesContent
    fun loadDetail(token: String, threadId: String, cursor: String?): MessageThreadDetail
    fun markRead(token: String, threadId: String): MessageOperation
    fun refreshThread(token: String, threadId: String): MessageOperation
    fun reply(token: String, threadId: String, body: String, requestId: String): MessageOperation
}

internal class MobileApiMessagesGateway(
    private val clientProvider: () -> MobileApiClient,
) : MessagesGateway {
    override fun loadPage(token: String, query: MessagesQuery, cursor: String?): MessagesContent =
        clientProvider().listMessages(
            token = token,
            search = query.search,
            channel = query.channel.queryValue,
            unreadOnly = query.filter == MessagesFilter.UNREAD,
            cursor = cursor,
            limit = MESSAGES_PAGE_LIMIT,
        ).toMessagesContent()

    override fun loadDetail(token: String, threadId: String, cursor: String?): MessageThreadDetail {
        val client = clientProvider()
        val detail = client.getMessageThread(
            token = token,
            threadId = threadId,
            cursor = cursor,
            limit = MESSAGES_DETAIL_PAGE_LIMIT,
        ).toMessageThreadDetail()
        val relatedOrder = detail.relatedOrder?.let { fallback ->
            runCatching { client.getOrder(token, fallback.id).toMessageRelatedOrder() }.getOrDefault(fallback)
        }
        return detail.copy(relatedOrder = relatedOrder)
    }

    override fun markRead(token: String, threadId: String): MessageOperation =
        clientProvider().markMessageRead(token, threadId).toMessageOperation()

    override fun refreshThread(token: String, threadId: String): MessageOperation =
        clientProvider().refreshMessageThread(token, threadId).toMessageOperation()

    override fun reply(token: String, threadId: String, body: String, requestId: String): MessageOperation =
        clientProvider().replyToMessageThread(token, threadId, body, requestId).toMessageOperation()
}
