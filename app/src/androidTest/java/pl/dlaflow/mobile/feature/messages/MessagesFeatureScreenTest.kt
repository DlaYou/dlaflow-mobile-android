package pl.dlaflow.mobile.feature.messages

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.dlaflow.mobile.R
import pl.dlaflow.mobile.core.designsystem.DlaFlowTheme
import pl.dlaflow.mobile.core.designsystem.DlaFlowThumbnailLoader
import pl.dlaflow.mobile.core.state.DlaFlowUiMessage
import pl.dlaflow.mobile.core.state.DlaFlowUiState

@RunWith(AndroidJUnit4::class)
class MessagesFeatureScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listShowsServerCountsStatusBadgesAndReservedSourceSlots() {
        composeRule.setContent {
            DlaFlowTheme(dark = false) { colors ->
                MessagesFeatureScreen(
                    colors = colors,
                    state = MessagesUiState(
                        listState = DlaFlowUiState.Content(
                            MessagesContent(
                                items = listOf(
                                    item("new", "new"),
                                    item("unread", "unread", providerId = "social", providerLabel = "Social"),
                                ),
                                total = 23,
                                nextCursor = null,
                                unreadCount = 6,
                            ),
                        ),
                    ),
                    thumbnailLoader = DlaFlowThumbnailLoader { _, _ -> null },
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("Wszystkie").assertIsDisplayed()
        composeRule.onNodeWithText("23").assertIsDisplayed()
        composeRule.onAllNodesWithText("Nieprzeczytane").assertCountEquals(2)
        composeRule.onNodeWithText("6").assertIsDisplayed()
        composeRule.onNodeWithText("Nowe").assertIsDisplayed()
        composeRule.onAllNodesWithTag("message_source_slot", useUnmergedTree = true).assertCountEquals(2)
        composeRule.onNodeWithContentDescription("Źródło: Allegro", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Źródło: Social", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun detailLoadingShowsFullSkeletonInsteadOfStaleConversationChrome() {
        composeRule.setContent {
            DlaFlowTheme(dark = false) { colors ->
                MessagesFeatureScreen(
                    colors = colors,
                    state = MessagesUiState(
                        route = MessagesRoute.Detail("thread-1"),
                        detailState = DlaFlowUiState.Loading,
                    ),
                    thumbnailLoader = DlaFlowThumbnailLoader { _, _ -> null },
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithTag("message_detail_loading_skeleton", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onAllNodesWithTag("message_detail_skeleton", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithText("Napisz wiadomość...", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("Zamówienie", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun detailRefreshHidesRetainedContentUntilRefreshCompletes() {
        composeRule.setContent {
            DlaFlowTheme(dark = false) { colors ->
                MessagesFeatureScreen(
                    colors = colors,
                    state = MessagesUiState(
                        route = MessagesRoute.Detail("thread-1"),
                        detailState = DlaFlowUiState.Content(detail()),
                        isRefreshingThread = true,
                    ),
                    thumbnailLoader = DlaFlowThumbnailLoader { _, _ -> null },
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithTag("message_detail_loading_skeleton", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("Stara wiadomość", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun detailPaginationFailureOffersRetryAction() {
        val actions = mutableListOf<MessagesAction>()
        composeRule.setContent {
            DlaFlowTheme(dark = false) { colors ->
                MessagesFeatureScreen(
                    colors = colors,
                    state = MessagesUiState(
                        route = MessagesRoute.Detail("thread-1"),
                        detailState = DlaFlowUiState.Content(detail()),
                        transientMessage = DlaFlowUiMessage(
                            titleRes = R.string.mobile_error_server_title,
                            descriptionRes = R.string.mobile_error_server_description,
                            retryable = true,
                        ),
                        retryOperation = MessagesOperation.Detail("thread-1", MessagesDetailLoadMode.LOAD_MORE),
                    ),
                    thumbnailLoader = DlaFlowThumbnailLoader { _, _ -> null },
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithText("Spróbuj ponownie", useUnmergedTree = true).performClick()

        assertEquals(listOf(MessagesAction.Retry), actions)
    }

    private fun item(
        id: String,
        status: String,
        providerId: String = "allegro",
        providerLabel: String = "Allegro",
    ) = MessageListItem(
        id = id,
        providerId = providerId,
        integrationId = "integration",
        providerLabel = providerLabel,
        customerName = "Anna Kowalska",
        customerLogin = "anna",
        subject = "Pytanie o wysyłkę",
        preview = MessagePreview("Czy paczka wyjdzie?", MessageDirection.INBOUND, "2026-08-24T10:00:00Z"),
        lastMessageAt = "2026-08-24T10:00:00Z",
        messageCount = 1,
        orderId = null,
        orderNumber = null,
        readAt = null,
        status = status,
        channel = if (providerId == "social") MessagesChannel.SOCIAL else MessagesChannel.MARKETPLACE,
    )

    private fun detail() = MessageThreadDetail(
        id = "thread-1",
        providerId = "allegro",
        integrationId = "integration",
        providerLabel = "Allegro",
        customerName = "Anna Kowalska",
        customerLogin = "anna",
        customerEmail = null,
        subject = "Pytanie",
        lastMessageAt = "2026-08-24T10:00:00Z",
        readAt = "2026-08-24T10:01:00Z",
        status = "read",
        orderId = null,
        orderNumber = null,
        messages = listOf(MessageBubble("m1", "Anna", MessageDirection.INBOUND, "Stara wiadomość", "2026-08-24T10:00:00Z", "read", emptyList())),
        nextCursor = null,
        customerContext = null,
    )
}
