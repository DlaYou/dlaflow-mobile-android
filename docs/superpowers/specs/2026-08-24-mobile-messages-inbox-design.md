# Mobile Messages Inbox Design

## Goal

Provide a native DlaFlow Android inbox that mirrors the Panel's customer-message threads, lets an operator open a conversation, mark it read, refresh the provider-backed thread, and send a reply without creating a second business model on Android.

## Scope

The first release contains:

- an inbox list with search, all/unread filters, provider identity, customer/order context, preview and relative time;
- a conversation detail view with inbound/outbound bubbles, timestamps, safe attachment metadata and linked order;
- read, refresh and reply actions routed through the Panel;
- foreground refresh, pull-to-refresh, loading skeletons, empty/offline/error/no-access states and FCM deep-link entry to a thread;
- a responsive single-column layout at 360/412 dp and a list/detail layout at 600 dp when space permits.

The first release does not contain new-thread creation, local message editing, provider-specific parsing, or local-only important/archive flags. The reference screen's "Nowa wiadomość" action remains out of the native UI until the Panel exposes a canonical create-thread contract. "Ważne" and "Archiwum" require canonical Panel fields and actions and are deferred rather than simulated locally.

## Repository Boundaries

The Panel/API remains the source of truth for threads, messages, read state, provider actions, permissions and tenant isolation. The Panel agent must expose a small signed mobile contract under `/api/mobile/messages`; Android must not call browser `/api/messages` routes or add provider models.

Android owns only presentation state, request coordination and safe DTO mapping. Existing signed transport, session verification, `DlaFlowUiState`, notification preferences and `message.created` FCM deep links are reused.

## Mobile API Contract

The Panel counterpart exposes these operations with the existing mobile bearer, request signature and tenant scope:

### `GET /api/mobile/messages`

Query parameters:

- `limit`: 1-20 for the initial page;
- `cursor`: opaque cursor from the previous response;
- `search`: bounded search over customer, order number and provider thread fields;
- `channel`: `all`, `marketplace`, `store`, `email` or `social`;
- `unreadOnly`: boolean.

The response contains `{ data: { items, total, nextCursor, unreadCount } }`. Each item contains only:

```json
{
  "id": "thread-id",
  "providerId": "allegro",
  "integrationId": "connection-id",
  "buyer": { "name": "Anna Kowalska", "login": "buyer-login" },
  "subject": "Pytanie o czas realizacji zamówienia",
  "lastMessage": { "body": "...", "direction": "inbound", "messageAt": "..." },
  "lastMessageAt": "...",
  "messageCount": 3,
  "orderLink": { "id": "12345", "orderId": "order-id" },
  "readAt": null,
  "status": "unread"
}
```

The API must bound text, omit raw provider payloads and return a safe empty value when a customer or order relation is missing.

### `GET /api/mobile/messages/:threadId`

The detail response contains the same thread identity, customer/order context, `readAt`, provider and status plus a cursor-paginated `messages` array. Each message exposes `id`, `author`, `direction`, `body`, `messageAt`, `status` and safe attachment descriptors (`id`, `filename`, `contentType`, `size`, `status`, canonical same-origin URL). It never exposes provider credentials, raw headers or unbounded payloads.

### Mutations

- `POST /api/mobile/messages/:threadId/read` marks the provider thread and Panel record read;
- `POST /api/mobile/messages/:threadId/refresh` queues the existing bounded Sync Engine refresh and returns an operation identifier;
- `POST /api/mobile/messages/:threadId/reply` accepts a bounded body and client `requestId`, queues the existing provider job and returns `operationId`, `messageId`, `queued`, `duplicate` and safe status.

Mutation permissions mirror the Panel: `messages.view` for reads, `messages.reply` for replies, and `integrations.runJobs` for provider refresh. A provider/auth failure is projected to a safe API error; Android does not retry blindly or clear a valid session after one auxiliary 401.

## Android Architecture

Create a `feature/messages` boundary following the existing Orders and Notifications patterns:

- contract types for list/detail content, filters, routes, actions and effects;
- mapper functions from `MobileApiClient` DTOs to UI content;
- gateway methods using the signed client;
- state holder with request/session guards, optimistic read/reply handling and pagination;
- coordinator for list/detail loads, refresh, read, reply and operation polling;
- Compose list and detail screens using `core/designsystem` cards, fields, badges, skeletons and state components.

The current legacy `MessagesTab` preview is replaced only after the new feature is wired through the existing `MobileAssistantTab.MESSAGES` route. Notification-center entries remain a separate surface; a notification action opens the messages route or a concrete thread when the payload includes a valid thread ID.

## UI and Interaction

The list follows the approved reference: DlaFlow header, "Wiadomości" title, "Skrzynka odbiorcza" subtitle, search field, compact filter segments, bordered thread rows and bottom navigation. Rows show provider/avatar, unread marker, customer or channel, subject, bounded preview, timestamp and a status pill. No phone number or technical endpoint text is shown.

The detail view uses a back action, provider/customer header, linked order row, chronological bubbles, attachment rows and a reply composer pinned above the bottom navigation. Outbound replies use the Panel identity and show `queued` until the Panel confirms the operation. The send button remains at least 48 dp and supports TalkBack labels, disabled, sending and error states.

At 600 dp the screen may use a stable list/detail split; at 360/412 dp it uses a navigation stack. All text uses Inter, existing DlaFlow color roles, `letterSpacing = 0.sp`, safe insets and no new hex values.

## Loading, Errors and Offline

- Initial list/detail load renders shape-matched skeletons, not a blank surface.
- Pull-to-refresh preserves visible content while showing the shared refresh indicator.
- Empty results distinguish an empty inbox from a search with no matches.
- Offline retains the last canonical content and shows a concise stale-data state.
- 403 renders no-access UI; 401 follows the existing `/api/mobile/me` confirmation flow.
- Invalid or unsupported notification deep links open the inbox safely.
- A reply is locally shown only after the API accepts the bounded request; duplicate `requestId` responses do not create another bubble.

## Verification

Android JVM tests cover DTO parsing, mapper safety, filter/search state, cursor pagination, read/reply transitions, duplicate request handling, deep links, all UI states and 360/412/600 dp layout invariants. The required Android gate is:

```text
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

The Panel counterpart must add API tests for tenant isolation, RBAC, safe projection, cursor bounds, provider refresh/reply authorization and operation responses. Integration verification uses a controlled seeded thread and confirms Panel persistence, Android list/detail rendering and a queued reply without logging customer content.

No version bump, tag, public release or VPS deployment is part of this design. Those require separate approval after both repositories pass their gates and the FCM production configuration is confirmed.
