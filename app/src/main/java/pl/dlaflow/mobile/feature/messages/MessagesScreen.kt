package pl.dlaflow.mobile.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.layout.wrapContentWidth
import java.time.OffsetDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
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
import pl.dlaflow.mobile.core.designsystem.DlaFlowSecondaryButton
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnail
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnailLoader
import pl.dlaflow.mobile.core.designsystem.dlaFlowHexColor
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

internal fun messageTimestampLabel(
    value: String,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Instant = Instant.now(),
): String = runCatching {
    val timestamp = OffsetDateTime.parse(value).toInstant().atZone(zone)
    val today = now.atZone(zone).toLocalDate()
    val label = when (timestamp.toLocalDate()) {
        today -> "Dzisiaj"
        today.minusDays(1) -> "Wczoraj"
        else -> timestamp.format(DateTimeFormatter.ofPattern("dd.MM", Locale("pl", "PL")))
    }
    "$label, ${timestamp.format(DateTimeFormatter.ofPattern("HH:mm", Locale("pl", "PL")))}"
}.getOrDefault("")

@Composable
internal fun MessagesFeatureScreen(
    colors: DlaFlowComposeColors,
    state: MessagesUiState,
    thumbnailLoader: DlaFlowThumbnailLoader,
    onAction: (MessagesAction) -> Unit,
) {
    when (val route = state.route) {
        MessagesRoute.List -> MessagesInboxScreen(colors, state, onAction)
        is MessagesRoute.Detail -> MessageThreadDetailScreen(colors, state, route.threadId, thumbnailLoader, onAction)
    }
}

@Composable
private fun MessagesInboxScreen(
    colors: DlaFlowComposeColors,
    state: MessagesUiState,
    onAction: (MessagesAction) -> Unit,
) {
    var filtersDialogOpen by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        DlaFlowScreenHeader(colors, "Wiadomości", "Rozmowy z klientami z panelu DlaFlow")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DlaFlowSearchField(
                colors = colors,
                value = state.query.search,
                placeholder = "Szukaj wiadomości...",
                modifier = Modifier.weight(1f).height(56.dp),
                singleLine = true,
                placeholderFontSize = 11.sp,
                onValueChange = { onAction(MessagesAction.SearchChanged(it)) },
            )
            Row(
                modifier = Modifier
                    .width(76.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { filtersDialogOpen = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.Tune, contentDescription = "Filtry", tint = colors.textMuted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                androidx.compose.material3.Text("Filtry", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        MessagesFilterRow(colors, state, state.query, onAction)
        if (filtersDialogOpen) {
            MessagesChannelDialog(
                colors = colors,
                selectedChannel = state.query.channel,
                onSelect = { channel ->
                    filtersDialogOpen = false
                    onAction(MessagesAction.ChannelChanged(channel))
                },
                onDismiss = { filtersDialogOpen = false },
            )
        }
        when (val listState = state.listState) {
            DlaFlowUiState.Loading -> MessagesListSkeleton(colors)
            DlaFlowUiState.Empty -> MessagesStateCard(colors, MessagesStateKind.EMPTY)
            DlaFlowUiState.NoAccess -> MessagesStateCard(colors, MessagesStateKind.NO_ACCESS)
            is DlaFlowUiState.Error -> MessagesStateCard(
                colors = colors,
                kind = MessagesStateKind.ERROR,
                message = listState.message,
                onRetry = state.retryOperation?.let { { onAction(MessagesAction.Retry) } },
            )
            is DlaFlowUiState.Offline -> {
                if (listState.lastContent == null) {
                    MessagesStateCard(
                        colors = colors,
                        kind = MessagesStateKind.OFFLINE,
                        message = state.transientMessage,
                        onRetry = state.retryOperation?.let { { onAction(MessagesAction.Retry) } },
                    )
                } else {
                    MessagesTransientNotice(
                        colors = colors,
                        message = state.transientMessage,
                        onRetry = state.retryOperation?.let { { onAction(MessagesAction.Retry) } },
                    )
                    MessagesListContent(colors, state, onAction)
                }
            }
            is DlaFlowUiState.Content -> {
                MessagesTransientNotice(
                    colors = colors,
                    message = state.transientMessage,
                    onRetry = state.retryOperation?.let { { onAction(MessagesAction.Retry) } },
                )
                MessagesListContent(colors, state, onAction)
            }
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
                compact = true,
                count = state.listContentOrNull()?.countFor(filter) ?: 0,
                onClick = { onAction(MessagesAction.FilterChanged(filter)) },
            )
        }
    }
}

@Composable
private fun MessagesChannelDialog(
    colors: DlaFlowComposeColors,
    selectedChannel: MessagesChannel,
    onSelect: (MessagesChannel) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.widthIn(max = 360.dp),
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = AlertDialogDefaults.TonalElevation,
        titleContentColor = colors.textStrong,
        textContentColor = colors.text,
        title = {
            androidx.compose.material3.Text(
                "Filtry wiadomości",
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                MessagesChannel.entries.forEach { channel ->
                    DlaFlowFilterChip(
                        colors = colors,
                        label = messagesChannelLabel(channel),
                        selected = selectedChannel == channel,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(channel) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("Gotowe", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
    )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp)),
    ) {
        items.forEachIndexed { index, item ->
            MessageThreadRow(colors, item, index % 2 == 1, onAction)
            if (index < items.lastIndex) {
                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
            }
        }
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
    alternate: Boolean,
    onAction: (MessagesAction) -> Unit,
) {
    val statusBadge = messageStatusBadgeLabel(item.isNew, item.isUnread)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (alternate) colors.surfaceSubtle else colors.surface)
            .clickable(role = Role.Button) { onAction(MessagesAction.OpenThread(item.id)) }
            .semantics {
                contentDescription = listOfNotNull(
                    "Rozmowa ${item.customerName}, ${item.subject}",
                    statusBadge,
                ).joinToString(", ")
            }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (item.isUnread) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.CenterVertically)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.primary),
            )
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(colors.surfaceSubtle)
                .border(1.dp, if (item.isUnread) colors.primarySoftBorder else colors.borderSubtle, RoundedCornerShape(17.dp))
                .align(Alignment.CenterVertically)
                .testTag("message_source_slot"),
            contentAlignment = Alignment.Center,
        ) {
            MessageSourceMarkVisual(
                colors = colors,
                providerId = item.providerId,
                fallbackLabel = item.providerLabel,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Text(
                    text = item.customerName,
                    color = colors.textStrong,
                    fontFamily = DlaFlowInter,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 15.sp,
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
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.preview?.let { preview ->
                androidx.compose.material3.Text(
                    text = preview.body,
                    color = colors.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .width(94.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            androidx.compose.material3.Text(
                text = messageTimestampLabel(item.lastMessageAt),
                color = colors.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
            statusBadge?.let { badge ->
                androidx.compose.material3.Text(
                    text = badge,
                    color = colors.primary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.primarySoft)
                        .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private enum class MessagesStateKind { EMPTY, EMPTY_FILTER, NO_ACCESS, OFFLINE, ERROR }

@Composable
private fun MessagesTransientNotice(
    colors: DlaFlowComposeColors,
    message: DlaFlowUiMessage?,
    onRetry: (() -> Unit)? = null,
) {
    message ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DlaFlowStateCard(
            colors = colors,
            icon = Icons.Rounded.WarningAmber,
            iconColor = colors.warning,
            title = stringResource(message.titleRes),
            description = stringResource(message.descriptionRes),
        )
        if (message.retryable && onRetry != null) {
            MessagesRetryButton(colors, onRetry)
        }
    }
}

@Composable
private fun MessagesStateCard(
    colors: DlaFlowComposeColors,
    kind: MessagesStateKind,
    message: DlaFlowUiMessage? = null,
    onRetry: (() -> Unit)? = null,
) {
    val values = when (kind) {
        MessagesStateKind.EMPTY -> Triple(Icons.Rounded.ChatBubbleOutline, "Brak wiadomości", "Gdy klient napisze, rozmowa pojawi się tutaj.")
        MessagesStateKind.EMPTY_FILTER -> Triple(Icons.Rounded.Search, "Brak wyników", "Zmień filtr lub wyszukiwanie.")
        MessagesStateKind.NO_ACCESS -> Triple(Icons.Rounded.WarningAmber, "Brak dostępu", "To konto nie ma dostępu do wiadomości.")
        MessagesStateKind.OFFLINE -> Triple(Icons.Rounded.ErrorOutline, "Brak połączenia", "Sprawdź internet i spróbuj ponownie.")
        MessagesStateKind.ERROR -> Triple(Icons.Rounded.ErrorOutline, "Nie udało się pobrać wiadomości", "Spróbuj ponownie za chwilę.")
    }
    DlaFlowStateCard(
        colors = colors,
        icon = values.first,
        iconColor = if (kind == MessagesStateKind.NO_ACCESS) colors.warning else colors.primary,
        title = message?.let { stringResource(it.titleRes) } ?: values.second,
        description = message?.let { stringResource(it.descriptionRes) } ?: values.third,
    )
    if (message?.retryable == true && onRetry != null) {
        MessagesRetryButton(colors, onRetry)
    }
}

@Composable
private fun MessagesRetryButton(colors: DlaFlowComposeColors, onRetry: () -> Unit) {
    DlaFlowSecondaryButton(
        colors = colors,
        icon = Icons.Rounded.Refresh,
        text = stringResource(R.string.messages_retry),
        modifier = Modifier.fillMaxWidth(),
        onClick = onRetry,
    )
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
    thumbnailLoader: DlaFlowThumbnailLoader,
    onAction: (MessagesAction) -> Unit,
) {
    val detail = state.detailContentOrNull()
    LaunchedEffect(threadId, detail?.readAt) {
        if (detail != null && detail.readAt == null) onAction(MessagesAction.MarkThreadRead)
    }
    if (state.isRefreshingThread) {
        MessageDetailLoadingSkeleton(colors)
        return
    }
    when (val detailState = state.detailState) {
        null, DlaFlowUiState.Loading -> MessageDetailLoadingSkeleton(colors)
        DlaFlowUiState.Empty, DlaFlowUiState.NoAccess, is DlaFlowUiState.Error -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MessageDetailHero(colors, detail, onBack = { onAction(MessagesAction.CloseDetail) })
                MessageDetailMeta(colors, detail)
                when (detailState) {
                    DlaFlowUiState.Empty -> MessagesStateCard(colors, MessagesStateKind.EMPTY)
                    DlaFlowUiState.NoAccess -> MessagesStateCard(colors, MessagesStateKind.NO_ACCESS)
                    is DlaFlowUiState.Error -> MessagesStateCard(
                        colors = colors,
                        kind = MessagesStateKind.ERROR,
                        message = detailState.message,
                        onRetry = state.retryOperation?.let { { onAction(MessagesAction.Retry) } },
                    )
                    else -> Unit
                }
            }
        }
        is DlaFlowUiState.Offline -> MessageDetailContent(
            colors = colors,
            state = state,
            detail = detailState.lastContent,
            thumbnailLoader = thumbnailLoader,
            onAction = onAction,
            offline = true,
        )
        is DlaFlowUiState.Content -> MessageDetailContent(
            colors = colors,
            state = state,
            detail = detailState.data,
            thumbnailLoader = thumbnailLoader,
            onAction = onAction,
            offline = false,
        )
    }
}

@Composable
private fun MessageDetailContent(
    colors: DlaFlowComposeColors,
    state: MessagesUiState,
    detail: MessageThreadDetail?,
    thumbnailLoader: DlaFlowThumbnailLoader,
    onAction: (MessagesAction) -> Unit,
    offline: Boolean,
) {
    val retryAction: (() -> Unit)? = state.retryOperation?.let { { onAction(MessagesAction.Retry) } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MessageDetailHero(colors, detail, onBack = { onAction(MessagesAction.CloseDetail) })
        MessageDetailMeta(colors, detail)
        if (offline && detail == null) {
            MessagesStateCard(
                colors = colors,
                kind = MessagesStateKind.OFFLINE,
                message = state.transientMessage,
                onRetry = retryAction,
            )
        } else {
            MessagesTransientNotice(colors, state.transientMessage, retryAction)
            if (state.isLoadingMore) {
                MessageDetailLoadMoreSkeleton(colors)
            }
            MessageBubbles(colors, detail?.messages.orEmpty())
            detail?.relatedOrder?.let { relatedOrder ->
                MessageRelatedOrderCard(colors, relatedOrder, thumbnailLoader) {
                    onAction(MessagesAction.OpenRelatedOrder(relatedOrder.orderNumber.ifBlank { relatedOrder.id }))
                }
            }
        }
    }
}

@Composable
private fun MessageDetailHero(colors: DlaFlowComposeColors, detail: MessageThreadDetail?, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colors.surfaceSubtle)
                .border(1.dp, colors.border, RoundedCornerShape(9.dp))
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Wróć do wiadomości", tint = colors.text, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(23.dp)).background(colors.primarySoft),
            contentAlignment = Alignment.Center,
        ) {
            MessageSourceMarkVisual(
                colors = colors,
                providerId = detail?.providerId.orEmpty(),
                fallbackLabel = detail?.providerLabel.orEmpty(),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            androidx.compose.material3.Text(
                text = "${detail?.providerLabel ?: "Kanał"} – ${detail?.orderNumber?.let { "Zamówienie #$it" } ?: "Rozmowa"}",
                color = colors.textStrong,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DlaFlowHeaderIconButton(colors, Icons.Rounded.StarBorder, "Dodaj do ulubionych") {}
    }
}

@Composable
private fun MessageDetailMeta(colors: DlaFlowComposeColors, detail: MessageThreadDetail?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            Triple(Icons.Outlined.ShoppingBag, "Zamówienie", detail?.orderNumber?.let { "#$it" } ?: "Brak"),
            Triple(Icons.Outlined.Person, "Klient", detail?.customerName ?: "Brak"),
            Triple(Icons.Outlined.CalendarToday, "Data", messageTimestampLabel(detail?.lastMessageAt.orEmpty())),
        ).forEachIndexed { index, (icon, label, value) ->
            val columnWeight = if (index == 1) 1.33f else 1f
            MessageMetaCell(colors, icon, label, value, Modifier.weight(columnWeight))
            if (index < 2) {
                Box(Modifier.width(1.dp).height(50.dp).background(colors.borderSubtle))
            }
        }
    }
}

@Composable
private fun MessageMetaCell(
    colors: DlaFlowComposeColors,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(
                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(colors.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(icon, null, tint = colors.primary, modifier = Modifier.size(13.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                androidx.compose.material3.Text(label, color = colors.textMuted, fontSize = 8.sp, lineHeight = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                androidx.compose.material3.Text(value, color = colors.textStrong, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MessageSubjectCard(colors: DlaFlowComposeColors, detail: MessageThreadDetail?) {
    DlaFlowCard(colors) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.material3.Text(detail?.subject ?: "Temat rozmowy", color = colors.textStrong, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                androidx.compose.material3.Text(detail?.messages?.lastOrNull()?.body.orEmpty(), color = colors.textMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            DemoLabel(colors)
        }
    }
}

@Composable
private fun DemoLabel(colors: DlaFlowComposeColors) {
    androidx.compose.material3.Text(
        text = "DEMO",
        color = colors.danger,
        fontSize = 8.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(colors.danger.copy(alpha = 0.12f)).border(1.dp, colors.danger.copy(alpha = 0.45f), RoundedCornerShape(999.dp)).padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

@Composable
private fun MessageRelatedOrderCard(
    colors: DlaFlowComposeColors,
    order: MessageRelatedOrder,
    thumbnailLoader: DlaFlowThumbnailLoader,
    onOpenOrder: () -> Unit,
) {
    val item = order.items.firstOrNull()
    val statusColor = messageOrderStatusColor(colors, order.statusTone, order.statusColor)
    val price = item?.let {
        relatedOrderMoney(if (it.lineTotal > 0.0) it.lineTotal else it.unitPrice, order.currency)
    } ?: relatedOrderMoney(order.amount, order.currency)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.primarySoft.copy(alpha = 0.45f))
            .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Powiązane z zamówieniem", color = colors.textStrong, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                DlaFlowThumbnail(
                    colors = colors,
                    url = item?.image.orEmpty(),
                    loader = thumbnailLoader,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentDescription = item?.name,
                )
                if (item != null && item.quantity > 0) {
                    Text(
                        text = "${item.quantity} szt.",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(5.dp))
                            .background(colors.primary)
                            .padding(horizontal = 5.dp, vertical = 3.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item?.name ?: "Zamówienie #${order.orderNumber}", color = colors.textStrong, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("SKU: ${item?.sku?.ifBlank { "brak" } ?: "brak"}", color = colors.textMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(price, color = colors.textStrong, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = order.status.ifBlank { "Brak statusu" },
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .border(1.dp, colors.primarySoftBorder, RoundedCornerShape(7.dp))
                        .clickable(role = Role.Button, onClick = onOpenOrder)
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Zobacz zamówienie", color = colors.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.primary, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

private fun relatedOrderMoney(value: Double, currency: String): String =
    NumberFormat.getCurrencyInstance(Locale("pl", "PL")).apply {
        currency.takeIf { it.isNotBlank() }?.let { runCatching { java.util.Currency.getInstance(it) }.getOrNull()?.let(::setCurrency) }
    }.format(value)

private fun messageOrderStatusColor(colors: DlaFlowComposeColors, tone: String, customColor: String): Color {
    return dlaFlowHexColor(customColor) ?: when (tone.trim().lowercase(Locale.ROOT)) {
        "success" -> colors.success
        "info" -> colors.info
        "warning" -> colors.orange
        "danger" -> colors.danger
        "brand" -> colors.primary
        else -> colors.textMuted
    }
}

@Composable
private fun MessageBubbles(colors: DlaFlowComposeColors, messages: List<MessageBubble>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        messages.forEachIndexed { index, message ->
            val inbound = message.direction != MessageDirection.OUTBOUND
            val previous = messages.getOrNull(index - 1)
            val showInboundAvatar = inbound && (
                previous == null ||
                    previous.direction == MessageDirection.OUTBOUND ||
                    previous.author != message.author
                )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (inbound) Arrangement.Start else Arrangement.End, verticalAlignment = Alignment.Top) {
                if (showInboundAvatar) {
                    Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(19.dp)).background(colors.primarySoft), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text("a.", color = colors.primary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(8.dp))
                } else if (inbound) {
                    Spacer(Modifier.width(46.dp))
                }
                Column(modifier = Modifier.fillMaxWidth(0.88f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (inbound) colors.surface else colors.primarySoft)
                                .border(1.dp, if (inbound) colors.border else colors.primarySoftBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                androidx.compose.material3.Text(
                                    text = if (inbound) message.author else "Ty • DlaFlow",
                                    color = if (inbound) colors.textStrong else colors.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                androidx.compose.material3.Text(
                                    messageTimestampLabel(message.messageAt),
                                    color = if (inbound) colors.textMuted else colors.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            androidx.compose.material3.Text(message.body, color = colors.text, fontSize = 12.sp, lineHeight = 18.sp)
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
    }
}

@Composable
internal fun MessageReplyComposer(
    colors: DlaFlowComposeColors,
    state: MessagesUiState,
    onAction: (MessagesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var body by remember(state.route) { mutableStateOf("") }
    val enabled = canSendMessageReply(body, state.isSendingReply)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = body,
            onValueChange = { body = it.take(2000) },
            enabled = !state.isSendingReply,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp, max = 120.dp)
                .semantics { contentDescription = "Treść odpowiedzi do klienta" }
                .padding(vertical = 0.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = colors.textStrong, fontSize = 12.sp, lineHeight = 17.sp),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (body.isBlank()) {
                        androidx.compose.material3.Text("Napisz wiadomość...", color = colors.textMuted, fontSize = 12.sp)
                    }
                    inner()
                }
            },
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceSubtle),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.AttachFile,
                contentDescription = "Dodaj załącznik",
                tint = colors.textMuted,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (enabled) colors.primary else colors.borderSubtle)
                .clickable(enabled = enabled, role = Role.Button) {
                    val requestId = UUID.randomUUID().toString()
                    onAction(MessagesAction.SendReply(body, requestId))
                    body = ""
                },
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                imageVector = if (state.isSendingReply) Icons.Rounded.Refresh else Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Wyślij wiadomość",
                tint = if (enabled) Color.White else colors.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
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

@Composable
private fun MessageDetailLoadingSkeleton(colors: DlaFlowComposeColors) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("message_detail_loading_skeleton"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            DlaFlowSkeletonBlock(colors, Modifier.size(34.dp), radius = 9.dp)
            Spacer(Modifier.width(10.dp))
            DlaFlowSkeletonBlock(colors, Modifier.size(46.dp), radius = 23.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth(0.78f).height(14.dp), radius = 4.dp)
                DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth(0.45f).height(10.dp), radius = 4.dp)
            }
            DlaFlowSkeletonBlock(colors, Modifier.size(34.dp), radius = 9.dp)
        }
        DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth().height(72.dp), radius = 8.dp)
        MessageDetailSkeleton(colors)
        DlaFlowSkeletonBlock(colors, Modifier.fillMaxWidth().height(56.dp), radius = 8.dp)
    }
}

@Composable
private fun MessageDetailLoadMoreSkeleton(colors: DlaFlowComposeColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("message_detail_load_more_skeleton"),
        horizontalArrangement = Arrangement.Center,
    ) {
        DlaFlowSkeletonBlock(colors, Modifier.width(180.dp).height(12.dp), radius = 4.dp)
    }
}
