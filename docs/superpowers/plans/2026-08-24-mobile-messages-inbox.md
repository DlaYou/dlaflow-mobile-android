# Mobile Messages Inbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a native Android inbox and conversation detail flow backed by a signed `/api/mobile/messages` contract, including read, refresh, reply and FCM deep-link behavior.

**Architecture:** Keep the Panel as the source of truth and add a focused `feature/messages` boundary that follows the existing Orders/Notifications coordinator and `DlaFlowUiState` patterns. Extend the existing signed `MobileApiClient` with bounded DTOs and operations, then replace the legacy Messages tab with Compose list/detail screens. The Panel endpoint is a prerequisite handoff and is not implemented in this Android repository.

**Tech Stack:** Kotlin, Jetpack Compose, existing Android HTTP/signature transport, existing DlaFlow design system, JVM unit tests, Gradle lint/debug build.

---

## Dependency Contract

Before Android integration smoke, the Panel agent must provide the reviewed `/api/mobile/messages` endpoints described in `docs/superpowers/specs/2026-08-24-mobile-messages-inbox-design.md`, with tenant isolation, `messages.view`, `messages.reply` and `integrations.runJobs` enforcement. Android unit tests use bounded JSON fixtures so they do not require a live Panel endpoint.

### Task 1: Add mobile message DTOs and signed client methods

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/mobile_api.kt`
- Modify: `app/src/test/java/pl/dlaflow/mobile/MobileApiClientTest.kt`

- [ ] **Step 1: Write failing JSON parsing tests**

Add fixtures for a list page, detail page with inbound/outbound messages and attachments, read response, refresh operation and reply response. Assert that `MobileApiClient.listMessages`, `getMessageThread`, `markMessageRead`, `refreshMessageThread` and `replyToMessageThread` expose only bounded fields and preserve opaque cursors.

```kotlin
@Test
fun `message list parses provider customer preview and cursor`() {
    val page = MobileApiClient(testServerReturning("/api/mobile/messages" to messageListJson))
        .listMessages("token", search = "Anna", channel = "all", unreadOnly = true, cursor = null, limit = 20)

    assertEquals("thread-1", page.items.single().id)
    assertEquals("allegro", page.items.single().providerId)
    assertEquals("cursor-next", page.nextCursor)
    assertEquals("inbound", page.items.single().lastMessage?.direction)
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run `./gradlew :app:testDebugUnitTest --tests pl.dlaflow.mobile.MobileApiClientTest --no-daemon`.

Expected result: compilation fails because message DTOs and client methods do not exist.

- [ ] **Step 3: Implement bounded DTOs and request methods**

Add `MobileMessageBuyer`, `MobileMessagePreview`, `MobileMessageOrderLink`, `MobileMessageThread`, `MobileMessagesPage`, `MobileMessageAttachment`, `MobileMessage`, `MobileMessageCustomerContext`, `MobileMessageThreadDetail`, and `MobileMessageOperation` beside the existing mobile DTOs. Add these signed methods to `MobileApiClient`:

```kotlin
fun listMessages(token: String, search: String, channel: String, unreadOnly: Boolean, cursor: String?, limit: Int): MobileMessagesPage
fun getMessageThread(token: String, threadId: String, cursor: String?, limit: Int): MobileMessageThreadDetail
fun markMessageRead(token: String, threadId: String): MobileMessageOperation
fun refreshMessageThread(token: String, threadId: String): MobileMessageOperation
fun replyToMessageThread(token: String, threadId: String, body: String, requestId: String): MobileMessageOperation
```

Use the same `getJson`, `postJson`, path-segment encoding, request signing and JSON parser helpers already used by orders and notifications. Clamp list/detail limits to `1..20` and `1..100`, reject blank IDs, reject reply bodies outside `1..2000` trimmed characters, and never log response bodies.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the same Gradle command. Expected result: all existing and new `MobileApiClientTest` cases pass.

- [ ] **Step 5: Commit the transport contract**

```text
git add app/src/main/java/pl/dlaflow/mobile/mobile_api.kt app/src/test/java/pl/dlaflow/mobile/MobileApiClientTest.kt
git commit -m "feat: add signed mobile messages client contract"
```

### Task 2: Define messages feature state, mapper and failure policy

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesContract.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesMapper.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesFailure.kt`
- Create: `app/src/test/java/pl/dlaflow/mobile/feature/messages/MessagesMapperTest.kt`
- Create: `app/src/test/java/pl/dlaflow/mobile/feature/messages/MessagesContractTest.kt`

- [ ] **Step 1: Write RED mapper and state tests**

Cover provider labels (`allegro`, `gmail`, `woocommerce`), blank buyer/order fallbacks, direction mapping, safe attachment metadata, `ALL`/`UNREAD`/channel filters and unsupported deep-link destinations.

```kotlin
@Test
fun `blank buyer and order values use safe presentation fallbacks`() {
    val item = fixtureThread(buyerName = "", buyerLogin = "", orderNumber = "").toMessageItem()
    assertEquals("Nieznany klient", item.customerName)
    assertNull(item.orderNumber)
}
```

- [ ] **Step 2: Run the focused tests and verify RED**

Run `./gradlew :app:testDebugUnitTest --tests 'pl.dlaflow.mobile.feature.messages.*' --no-daemon`.

Expected result: compilation fails because the messages feature types are absent.

- [ ] **Step 3: Implement the feature contract**

Define `MessagesFilter`, `MessagesChannel`, `MessageListItem`, `MessagePreview`, `MessageAttachment`, `MessageBubble`, `MessageThreadDetail`, `MessagesContent`, `MessagesUiState`, `MessagesRoute`, `MessagesAction` and `MessagesEffect`. Include request IDs and internal session keys in load/mutation requests. Use `DlaFlowUiState` for list and detail and keep optimistic replies marked `queued` until the Panel operation confirms them.

- [ ] **Step 4: Implement mapper and failure mapping**

Map only the DTO fields in the design spec. Normalize provider labels for display, preserve raw provider IDs only for icons/accessibility, clamp text preview length, map 401 to the existing unauthorized path, 403 to no-access, network failures to offline and all other failures to retryable safe messages.

- [ ] **Step 5: Run the focused tests and verify GREEN**

Run the focused Gradle command again. Expected result: all mapper, filter, fallback and failure tests pass.

- [ ] **Step 6: Commit the feature contract**

```text
git add app/src/main/java/pl/dlaflow/mobile/feature/messages app/src/test/java/pl/dlaflow/mobile/feature/messages
git commit -m "feat: define mobile messages feature contract"
```

### Task 3: Add gateway, state holder and coordinator

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesGateway.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesStateHolder.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesCoordinator.kt`
- Create: `app/src/test/java/pl/dlaflow/mobile/feature/messages/MessagesStateHolderTest.kt`
- Create: `app/src/test/java/pl/dlaflow/mobile/feature/messages/MessagesCoordinatorTest.kt`

- [ ] **Step 1: Write RED state/coordinator tests**

Test stale request rejection, session-key rejection, list refresh preserving content, cursor pagination, opening a detail, read mutation, refresh operation, optimistic queued reply and duplicate `requestId` handling.

```kotlin
@Test
fun `reply is accepted once and duplicate request remains one visible bubble`() {
    val harness = messagesHarness()
    harness.coordinator.reply("token", "thread-1", "Dziękuję", requestId = "reply-123456")
    harness.coordinator.reply("token", "thread-1", "Dziękuję", requestId = "reply-123456")
    assertEquals(1, harness.stateHolder.detailContentOrNull()!!.messages.count { it.requestId == "reply-123456" })
}
```

- [ ] **Step 2: Run focused tests and verify RED**

Run `./gradlew :app:testDebugUnitTest --tests 'pl.dlaflow.mobile.feature.messages.Messages*Test' --no-daemon`.

Expected result: compilation fails because gateway, holder and coordinator are absent.

- [ ] **Step 3: Implement gateway and request guards**

Implement `MessagesGateway` with the five client calls from Task 1. Follow `NotificationsCoordinator` and `OrdersCoordinator` for executor dispatch, `postToMain`, request IDs, unauthorized confirmation and no overlapping requests. Never clear the session from a single auxiliary 401.

- [ ] **Step 4: Implement state transitions**

Implement `MessagesStateHolder` transitions for Loading, Content, Empty, Offline, Error and NoAccess. Preserve last content on refresh/offline, replace list pages only for matching session/request, and merge optimistic reply bubbles by `requestId`.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run the focused Gradle command again. Expected result: state and coordinator tests pass, including stale-session and duplicate-reply cases.

- [ ] **Step 6: Commit runtime coordination**

```text
git add app/src/main/java/pl/dlaflow/mobile/feature/messages app/src/test/java/pl/dlaflow/mobile/feature/messages
git commit -m "feat: coordinate mobile message threads"
```

### Task 4: Build Compose inbox and conversation detail UI

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesScreen.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessageThreadDetailScreen.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesSkeleton.kt`
- Modify: existing shared resource file under `app/src/main/res/values/strings.xml`
- Create: `app/src/test/java/pl/dlaflow/mobile/feature/messages/MessagesUiTextTest.kt`

- [ ] **Step 1: Add UI contract tests before Compose code**

Assert filter labels, safe empty/no-access/offline copy, TalkBack content descriptions and that the composer is disabled for blank input or an in-flight send.

- [ ] **Step 2: Implement the inbox screen**

Use existing `DlaFlowScreenHeader`, `DlaFlowCard`, `DlaFlowSkeletonBlock`, `DlaFlowEmptyState` and shared colors. Render search, `Wszystkie`, `Nieprzeczytane`, provider filters, thread rows, unread marker, provider avatar, subject, bounded preview and time. Add `Modifier.testTag` values for list, filters and rows. Keep every interactive target at least 48 dp.

- [ ] **Step 3: Implement detail and composer**

Render back action, provider/customer header, linked order action, message bubbles from `direction`, timestamps, safe attachment rows, refresh icon and reply composer. Show queued/sending/failure states without exposing operation IDs. Make the list/detail split conditional on available width rather than a fixed screen-size assumption.

- [ ] **Step 4: Run unit tests and inspect Compose compilation**

Run `./gradlew :app:testDebugUnitTest --tests 'pl.dlaflow.mobile.feature.messages.*' --no-daemon`. Expected result: UI helper tests pass and Compose sources compile.

- [ ] **Step 5: Commit the screens**

```text
git add app/src/main/java/pl/dlaflow/mobile/feature/messages app/src/main/res/values/strings.xml app/src/test/java/pl/dlaflow/mobile/feature/messages
git commit -m "feat: add mobile messages inbox and detail screens"
```

### Task 5: Wire navigation, refresh and FCM deep links

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/DlaFlowFirebaseMessagingService.kt`
- Create: `app/src/test/java/pl/dlaflow/mobile/feature/messages/MessagesNavigationTest.kt`

- [ ] **Step 1: Write RED integration tests**

Assert that selecting `MobileAssistantTab.MESSAGES` opens the new inbox, tapping a row opens the matching thread, `OPEN_MESSAGES` without a thread opens the inbox, and `message.created` with `threadId` opens the detail after session verification.

- [ ] **Step 2: Replace the legacy Messages tab body**

Connect `MessagesCoordinator` state to the new screens, preserve the existing bottom navigation and notification-center route, and call list refresh on tab entry, foreground refresh and pull-to-refresh.

- [ ] **Step 3: Wire detail effects**

Route open-order effects to the existing Orders route, back to the inbox, read/refresh/reply mutations to the coordinator and safe unsupported effects to the existing explanation surface.

- [ ] **Step 4: Wire FCM thread deep link**

Extend the existing bounded `message.created` notification intent with only `threadId` and route it to the new feature. Keep missing/unknown IDs safe and retain current notification preference checks.

- [ ] **Step 5: Run integration tests and verify GREEN**

Run `./gradlew :app:testDebugUnitTest --tests '*Messages*Test' --tests '*DeepLink*Test' --no-daemon`.

- [ ] **Step 6: Commit navigation integration**

```text
git add app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt app/src/main/java/pl/dlaflow/mobile/MainActivity.kt app/src/main/java/pl/dlaflow/mobile/DlaFlowFirebaseMessagingService.kt app/src/test/java/pl/dlaflow/mobile
git commit -m "feat: connect messages inbox to mobile navigation"
```

### Task 6: Full verification and handoff

**Files:**
- Modify: `docs/superpowers/specs/2026-08-24-mobile-messages-inbox-design.md` only if verified contract details change
- No release files or version metadata

- [ ] **Step 1: Run the required Android gate**

Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon`.

- [ ] **Step 2: Run static and diff checks**

Run `git diff --check`, inspect `git status --short --branch`, and confirm no secrets, raw payloads, version changes or new network libraries were introduced.

- [ ] **Step 3: Perform UI verification**

Run `./gradlew :app:connectedDebugAndroidTest --no-daemon` on the connected emulator and inspect light/dark, 360/412/600 dp, larger font, TalkBack labels, Back, offline/no-access and permission states. Use a physical phone for FCM delivery and native notification behavior; if no physical phone is connected, record that hardware check as outstanding rather than substituting a claim.

- [ ] **Step 4: Prepare Panel handoff**

Record the exact mobile contract, required permissions and API test expectations for the Agent Ogólny. Do not claim end-to-end completion until the Panel endpoint is merged, deployed and smoke-tested against a tenant-scoped seeded thread.

- [ ] **Step 5: Commit and report handoff state**

Run `git status --short --branch`, `git worktree list`, `git diff --check`, and `git log -1 --oneline`. Report the exact Android commit and checks directly to Agent Ogólny; the Panel agent separately runs `npm run graphify:code`, integrates the mobile branch to `main`, and coordinates the Panel contract deployment.

The branch remains unmerged until Agent Ogólny reviews the commit, integrates it to mobile `main`, runs emulator/phone smoke and coordinates the Panel contract deployment.
