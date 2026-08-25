package pl.dlaflow.mobile.feature.messages

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.dlaflow.mobile.core.designsystem.DlaFlowTheme
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
}
