package pl.dlaflow.mobile.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowBackHeader
import pl.dlaflow.mobile.core.designsystem.DlaFlowCard
import pl.dlaflow.mobile.core.designsystem.DlaFlowComposeColors
import pl.dlaflow.mobile.core.designsystem.DlaFlowFilterChip
import pl.dlaflow.mobile.core.designsystem.DlaFlowHeaderIconButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowInter
import pl.dlaflow.mobile.core.designsystem.DlaFlowPrimaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowScreenHeader
import pl.dlaflow.mobile.core.designsystem.DlaFlowSearchField
import pl.dlaflow.mobile.core.designsystem.DlaFlowSkeletonBlock
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusBadge
import pl.dlaflow.mobile.core.designsystem.DlaFlowStateCard
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

internal fun messagesFilterLabel(filter: MessagesFilter): String = when (filter) {
    MessagesFilter.ALL -> "Wszystkie"
    MessagesFilter.UNREAD -> "Nieprzeczytane"
}

internal fun messageStatusBadgeLabel(isNew: Boolean, isUnread: Boolean): String? = when {
    isNew && isUnread -> "Nowe"
    isUnread -> "Nieprzeczytane"
    else -> null
}

internal fun messagesChannelLabel(channel: MessagesChannel): String = when (channel) {
    MessagesChannel.ALL -> "Wszystkie kanały"
    MessagesChannel.MARKETPLACE -> "Allegro"
    MessagesChannel.STORE -> "Sklepy"
    MessagesChannel.EMAIL -> "E-mail"
    MessagesChannel.SOCIAL -> "Social"
}

internal fun canSendMessageReply(body: String, sending: Boolean): Boolean =
    !sending && body.trim().length in 1..2000

internal fun messageTimestampLabel(value: String, zone: ZoneId = ZoneId.systemDefault()): String = runCatching {
    OffsetDateTime.parse(value)
        .toInstant()
        .atZone(zone)
        .format(DateTimeFormatter.ofPattern("dd.MM, HH:mm", Locale("pl", "PL")))
}.getOrDefault("")

@Composable
internal fun MessagesFeatureScreen(
    colors: DlaFlowComposeColors,
    state: MessagesUiState,
    onAction: (MessagesAction) -> Unit,
) {
    when (val route = state.route) {
        MessagesRoute.List -> MessagesInboxScreen(colors, state, onAction)
        is MessagesRoute.Detail -> MessageThreadDetailScreen(colors, state, route.threadId, onAction)
    }
}

@Composable
private fun MessagesInboxScreen(
    colors: DlaFlowComposeColors,
    state: MessagesUiState,
    onAction: (MessagesAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        DlaFlowScreenHeader(colors, "Wiadomości", "Rozmowy z klientami z panelu DlaFlow")
        DlaFlowSearchField(
            colors = colors,
            value = state.query.search,
            placeholder = "Szukaj klienta, tematu lub numeru",
            onValueChange = { onAction(MessagesAction.SearchChanged(it)) },
        )
        MessagesFilterRow(colors, state, state.query, onAction)
        MessagesTransientNotice(colors, state.transientMessage)

        when (val listState = state.listState) {
            DlaFlowUiState.Loading -> MessagesListSkeleton(colors)
            DlaFlowUiState.Empty -> MessagesStateCard(colors, MessagesStateKind.EMPTY)
            DlaFlowUiState.NoAccess -> MessagesStateCard(colors, MessagesStateKind.NO_ACCESS)
            is DlaFlowUiState.Error -> MessagesStateCard(colors, MessagesStateKind.ERROR)
            is DlaFlowUiState.Offline -> {
                if (listState.lastContent == null) {
                    MessagesStateCard(colors, MessagesStateKind.OFFLINE)
                } else {
                    MessagesListContent(colors, state, onAction)
                }
            }
            is DlaFlowUiState.Content -> MessagesListContent(colors, state, onAction)
        }
    }
}

@Composable
private fun MessagesFilterRow(
    colors: DlaFlowComposeColors,
    state: MessagesUiState,
    query: MessagesQuery,
    onAction: (MessagesAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MessagesFilter.entries.forEach { filter ->
            DlaFlowFilterChip(
                colors = colors,
                label = messagesFilterLabel(filter),
                selected = query.filter == filter,
                count = state.listContentOrNull()?.countFor(filter) ?: 0,
                onClick = { onAction(MessagesAction.FilterChanged(filter)) },
            )
        }
        MessagesChannel.entries.forEach { channel ->
            DlaFlowFilterChip(
                colors = colors,
                label = messagesChannelLabel(channel),
                selected = query.channel == channel,
                onClick = { onAction(MessagesAction.ChannelChanged(channel)) },
            )
        }
    }
}

@Composable
private fun MessagesListContent(
    colors: DlaFlowComposeColors,
    state: MessagesUiState,
    onAction: (MessagesAction) -> Unit,
) {
    val items = state.visibleItems()
    if (items.isEmpty()) {
        MessagesStateCard(colors, MessagesStateKind.EMPTY_FILTER)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items.forEach { item -> MessageThreadRow(colors, item, onAction) }
        val hasMore = state.listContentOrNull()?.nextCursor != null
        if (hasMore) {
            DlaFlowPrimaryButton(
                colors = colors,
                icon = Icons.Rounded.Refresh,
                text = if (state.isLoadingMore) "Pobieram..." else "Pokaż więcej",
                enabled = !state.isLoadingMore,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAction(MessagesAction.LoadMore) },
            )
        }
    }
}

@Composable
private fun MessageThreadRow(
    colors: DlaFlowComposeColors,
    item: MessageListItem,
    onAction: (MessagesAction) -> Unit,
) {
    val statusBadge = messageStatusBadgeLabel(item.isNew, item.isUnread)
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, if (item.isUnread) colors.primarySoftBorder else colors.border, shape)
            .clickable(role = Role.Button) { onAction(MessagesAction.OpenThread(item.id)) }
            .semantics {
                contentDescription = listOfNotNull(
                    "Rozmowa ${item.customerName}, ${item.subject}",
                    statusBadge,
                ).joinToString(", ")
            }
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (item.isUnread) colors.primary else colors.surface),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (item.isUnread) colors.primarySoft else colors.surfaceSubtle)
                .testTag("message_source_slot"),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.ChatBubbleOutline,
                contentDescription = null,
                tint = if (item.isUnread) colors.primary else colors.textMuted,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Text(
                    text = item.customerName,
                    color = colors.textStrong,
                    fontFamily = DlaFlowInter,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            androidx.compose.material3.Text(
                text = item.subject,
                color = colors.text,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.preview?.let { preview ->
                androidx.compose.material3.Text(
                    text = preview.body,
                    color = colors.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Text(item.providerLabel, color = colors.primary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                item.orderNumber?.let {
                    androidx.compose.material3.Text(" · #$it", color = colors.textMuted, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .width(78.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            androidx.compose.material3.Text(
                text = messageTimestampLabel(item.lastMessageAt),
                color = colors.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            statusBadge?.let { badge ->
                DlaFlowStatusBadge(colors, badge, compact = true)
            }
        }
    }
}

private enum class MessagesStateKind { EMPTY, EMPTY_FILTER, NO_ACCESS, OFFLINE, ERROR }

@Composable
private fun MessagesTransientNotice(
    colors: DlaFlowComposeColors,
    message: DlaFlowUiMessage?,
) {
    message ?: return
    DlaFlowStateCard(
        colors = colors,
        icon = Icons.Rounded.WarningAmber,
        iconColor = colors.warning,
        title = stringResource(message.titleRes),
        description = stringResource(message.descriptionRes),
    )
}

@Composable
private fun MessagesStateCard(colors: DlaFlowComposeColors, kind: MessagesStateKind) {
    val values = when (kind) {
        MessagesStateKind.EMPTY -> Triple(Icons.Rounded.ChatBubbleOutline, "Brak wiadomości", "Gdy klient napisze, rozmowa pojawi się tutaj.")
        MessagesStateKind.EMPTY_FILTER -> Triple(Icons.Rounded.Search, "Brak wyników", "Zmień filtr lub wyszukiwanie.")
        MessagesStateKind.NO_ACCESS -> Triple(Icons.Rounded.WarningAmber, "Brak dostępu", "To konto nie ma dostępu do wiadomości.")
        MessagesStateKind.OFFLINE -> Triple(Icons.Rounded.ErrorOutline, "Brak połączenia", "Sprawdź internet i spróbuj ponownie.")
        MessagesStateKind.ERROR -> Triple(Icons.Rounded.ErrorOutline, "Nie udało się pobrać wiadomości", "Spróbuj ponownie za chwilę.")
    }
    DlaFlowStateCard(colors, values.first, if (kind == MessagesStateKind.NO_ACCESS) colors.warning else colors.primary, values.second, values.third)
}

@Composable
private fun MessagesListSkeleton(colors: DlaFlowComposeColors) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().testTag("messages_list_skeleton")) {
        repeat(5) {
            DlaFlowCard(colors) {
                Row(verticalAlignment = Alignment.Top) {
                    DlaFlowSkeletonBlock(colors, Modifier.size(38.dp), radius = 10.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth(0.55f).height(13.dp), radius = 4.dp)
                        DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth(0.8f).height(10.dp), radius = 4.dp)
                        DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth(0.95f).height(10.dp), radius = 4.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageThreadDetailScreen(
    colors: DlaFlowComposeColors,
    state: MessagesUiState,
    threadId: String,
    onAction: (MessagesAction) -> Unit,
) {
    val detail = state.detailContentOrNull()
    LaunchedEffect(threadId, detail?.readAt) {
        if (detail != null && detail.readAt == null) onAction(MessagesAction.MarkThreadRead)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        DlaFlowBackHeader(
            colors = colors,
            title = detail?.customerName ?: "Wiadomość",
            subtitle = detail?.subject.orEmpty(),
            backContentDescription = "Wróć do wiadomości",
            onBack = { onAction(MessagesAction.CloseDetail) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DlaFlowHeaderIconButton(colors, Icons.Rounded.Refresh, "Odśwież rozmowę") { onAction(MessagesAction.RefreshThread) }
            if (detail?.orderId != null || detail?.orderNumber != null) {
                androidx.compose.material3.Text(
                    text = "Zamówienie #${detail.orderNumber ?: detail.orderId}",
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
        MessagesTransientNotice(colors, state.transientMessage)
        when (val detailState = state.detailState) {
            null, DlaFlowUiState.Loading -> MessageDetailSkeleton(colors)
            DlaFlowUiState.Empty -> MessagesStateCard(colors, MessagesStateKind.EMPTY)
            DlaFlowUiState.NoAccess -> MessagesStateCard(colors, MessagesStateKind.NO_ACCESS)
            is DlaFlowUiState.Error -> MessagesStateCard(colors, MessagesStateKind.ERROR)
            is DlaFlowUiState.Offline -> detailState.lastContent?.let { MessageBubbles(colors, it.messages) } ?: MessagesStateCard(colors, MessagesStateKind.OFFLINE)
            is DlaFlowUiState.Content -> MessageBubbles(colors, detailState.data.messages)
        }
        MessageReplyComposer(colors, state, onAction)
    }
}

@Composable
private fun MessageBubbles(colors: DlaFlowComposeColors, messages: List<MessageBubble>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        messages.forEach { message ->
            val inbound = message.direction != MessageDirection.OUTBOUND
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (inbound) Arrangement.Start else Arrangement.End) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(if (inbound) 0.88f else 0.88f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (inbound) colors.surface else colors.primarySoft)
                        .border(1.dp, if (inbound) colors.border else colors.primarySoftBorder, RoundedCornerShape(8.dp))
                        .padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Text(message.author, color = colors.textStrong, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Text(messageTimestampLabel(message.messageAt), color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                    androidx.compose.material3.Text(message.body, color = colors.text, fontSize = 12.sp, lineHeight = 17.sp)
                    message.attachments.forEach { attachment ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(Icons.Rounded.AttachFile, null, tint = colors.primary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            androidx.compose.material3.Text(attachment.filename, color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (message.status.equals("queued", ignoreCase = true)) {
                        androidx.compose.material3.Text("Wysyłanie...", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageReplyComposer(
    colors: DlaFlowComposeColors,
    state: MessagesUiState,
    onAction: (MessagesAction) -> Unit,
) {
    var body by remember(state.route) { mutableStateOf("") }
    val enabled = canSendMessageReply(body, state.isSendingReply)
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = body,
            onValueChange = { body = it.take(2000) },
            enabled = !state.isSendingReply,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp, max = 120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceSubtle)
                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                .semantics { contentDescription = "Treść odpowiedzi do klienta" }
                .padding(horizontal = 12.dp, vertical = 13.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = colors.textStrong, fontSize = 12.sp, lineHeight = 17.sp),
            decorationBox = { inner ->
                if (body.isBlank()) androidx.compose.material3.Text("Napisz odpowiedź...", color = colors.textMuted, fontSize = 12.sp)
                inner()
            },
        )
        DlaFlowPrimaryButton(
            colors = colors,
            icon = if (state.isSendingReply) Icons.Rounded.Refresh else Icons.AutoMirrored.Rounded.Send,
            text = "Wyślij",
            modifier = Modifier.width(104.dp),
            enabled = enabled,
            onClick = {
                val requestId = UUID.randomUUID().toString()
                onAction(MessagesAction.SendReply(body, requestId))
                body = ""
            },
        )
    }
}

@Composable
private fun MessageDetailSkeleton(colors: DlaFlowComposeColors) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().testTag("message_detail_skeleton")) {
        repeat(4) { index ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (index % 2 == 0) Arrangement.Start else Arrangement.End) {
                DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth(0.82f).height(78.dp), radius = 8.dp)
            }
        }
    }
}
