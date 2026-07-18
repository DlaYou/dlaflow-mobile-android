# Mobile Orders Feature Extraction Design

**Status:** Approved by the product owner on 2026-07-18

**Repository:** `DlaYou/dlaflow-mobile-android`

**Base:** `origin/main` at Android Mobile Assistant 0.4.4 (`9685b5a`)

**Scope:** Phase 4 orders and order-detail extraction from the approved Mobile Design System + Architecture Foundation

## Context

Pairing and dashboard already use vertical feature boundaries, but orders are still split across legacy owners:

- `MainActivity` stores list, pagination and detail state and owns both request chains;
- `MobileAssistantScreen.kt` owns search, filters, list, pagination and detail composition;
- `mobile_api.kt` owns transport DTOs, endpoints and parsers;
- `MobileOrdersState.kt` mixes API query values with presentation labels and tones.

The panel API already exposes tenant-scoped, permission-protected `GET /api/mobile/orders` and `GET /api/mobile/orders/{orderNumber}` endpoints. This project does not change either endpoint, authentication, permissions or payloads.

The current implementation also has two lifecycle defects:

1. Android Back cannot close order detail while it is still loading because closing clears the selected DTO but not the loading route.
2. Resetting the list does not invalidate a pending detail request, so a stale callback can reopen detail after search or filter changes.

## Goals

1. Extract list and detail vertically into `pl.dlaflow.mobile.feature.orders`.
2. Preserve the current 0.4.4 UI, copy, endpoint behavior and navigation destinations.
3. Give list and detail independent, monotonically increasing request chains.
4. Make close, list reset, logout and session replacement invalidate the appropriate pending work.
5. Remove order DTO and request ownership from `MainActivity` and order composition from the legacy host.
6. Keep bearer tokens and `MobileApiClient` out of Compose and presentation state.
7. Use shared DlaFlow state and design-system standards with deterministic tests and a repository boundary gate.

## Non-goals

- No orders redesign, new filters, debounce or new business data.
- No backend, database, API schema, permission or parser ownership change.
- No extraction of scanner, products, notifications or signed transport.
- No Retrofit, Hilt, Room, Navigation Compose or Gradle multi-module migration.
- No version bump, tag, APK publication or production rollout.

## Chosen boundary

The feature contains:

- `OrdersContract.kt` — immutable presentation models, query, route, UI state and typed actions;
- `OrdersGateway.kt` — list-page and detail loading boundary;
- `OrdersMapper.kt` — transitional mapping from current transport DTOs;
- `OrdersFailure.kt` — orders-specific error classification using the shared mapper;
- `OrdersStateHolder.kt` — list/detail transitions, pagination and stale-result protection;
- `OrdersCoordinator.kt` — background work and main-thread delivery;
- `OrdersScreen.kt` — search, filters, list, pagination and route selection;
- `OrderDetailPanel.kt` — order-detail presentation.

Only `OrdersGateway.kt` and `OrdersMapper.kt` may depend on legacy `MobileOrder*` transport models. Plan 3 will move transport/parsers later.

```text
MainActivity
  -> OrdersCoordinator
    -> OrdersGateway
      -> MobileApiClient.listOrders/getOrder
      -> OrdersMapper
    -> OrdersStateHolder
  -> OrdersUiState
    -> OrdersFeatureScreen
      -> OrderDetailPanel
```

Rules:

- `feature/orders` must not import `MainActivity`, session storage, dashboard, scanner, products or platform launchers.
- The screen emits `OrdersAction`; it does not navigate, scan or create API clients.
- `MainActivity` remains the platform/session adapter and translates scanner results into `OpenOrder`.
- A host-provided Compose slot preserves the current scanner card and dashboard KPI block above the list without cross-feature imports.
- A neutral thumbnail loader hides the bearer token and transport client from the feature.

## Presentation contract

`OrdersListContent` contains the mapped list rows, total and next offset. `OrderDetailContent` contains only fields rendered by the existing detail surface, including customer, address, payment, item, shipment, document and message projections already visible in 0.4.4. Neither model contains bearer tokens, tenant IDs or raw payloads.

`OrdersQuery` owns search and `OrdersFilter`. The filter maps to the existing API query only inside the mapper/gateway seam. Search keeps the current immediate request timing; debounce is a separate future product change.

Typed actions cover:

- search change;
- filter change;
- refresh;
- load more;
- open order;
- close detail;
- retry the active surface.

## State model

```kotlin
data class OrdersUiState(
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
```

State rules:

1. Initial list load is `Loading`; a successful zero-result response is `Empty`.
2. Refresh keeps the previous list visible.
3. Offline keeps the last in-memory list where available.
4. Load more cannot overlap and appends without duplicate stable order identities.
5. A new search/filter reset invalidates list pagination and any detail request, then returns to list.
6. Opening detail starts a separate request chain and immediately changes route to detail loading.
7. Closing detail invalidates the detail chain, including while loading.
8. HTTP 403 produces whole-feature `NoAccess` and invalidates both chains.
9. HTTP 404 for detail produces a controlled detail error without destroying the list.
10. HTTP 401 uses the existing host confirmation/retry flow; a temporary failure of session verification ends in a request-bound retryable feature error instead of an orphaned spinner, and the feature never clears persisted session state itself.
11. `reset()` invalidates both chains and never reuses request IDs.

## Concurrency and session safety

List and detail use separate request IDs plus the session identity expected by the request. Success, failure and host effects are accepted only for the newest matching request. Unauthorized confirmation callbacks retain the originating request identity, so a late verification failure cannot overwrite newer content. Resetting list state invalidates detail. Logout, revocation or session replacement invalidates both chains.

The coordinator performs work on the existing executor and posts accepted results to the main thread. A stale callback may not mutate state, reopen detail or emit feedback.

## UI and design-system ownership

`OrdersFeatureScreen` owns the complete orders surface below the host compatibility slot:

- search;
- filter chips;
- list loading, empty, offline, error and no-access states;
- order rows and pagination;
- detail loading, failure and content route.

The extraction preserves current order, spacing, colors, typography, data and interaction behavior. Existing hard-coded orders copy moves to `strings.xml` without rewriting it.

Shared empty/error/offline/no-access and skeleton primitives are added only with real call sites and use `core/designsystem` tokens. The thumbnail boundary is neutral and reusable by products; the feature receives no token or API client.

## Compatibility seams

Until scanner and dashboard areas are extracted independently, the host supplies a list-only `leadContent` slot containing the current package scanner and KPI presentation. The feature does not inspect or own this content.

`MobileApiClient`, current DTOs and JSON parsers remain transport code until Plan 3. This is an explicit temporary seam, not a second orders system.

## Testing strategy

Implementation follows RED -> GREEN. Tests cover:

- list and detail parser fixtures, including optional/default fields;
- DTO-to-presentation mapping and no secret-bearing state;
- initial, content, empty, refresh and offline list transitions;
- pagination merge and deduplication;
- stale list and stale detail success/failure rejection;
- close while detail is loading;
- search/filter reset invalidating detail;
- logout/session replacement invalidating both chains;
- 401 retry-once, unconfirmed-session recovery, 403 and detail 404 behavior;
- typed actions and Compose semantics for filters, rows, load more, retry and Back;
- minimum 48 dp interactive targets and responsive state surfaces.

## Boundary gate

`scripts/verify-orders-feature-boundary.ps1` requires all eight feature files, restricts transport DTO dependencies to gateway/mapper, rejects cross-feature/platform imports, rejects legacy activity state/loaders, rejects legacy private order composables and confirms the two canonical endpoints remain present.

## Verification matrix

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-repository-contract.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-design-system-boundary.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-pairing-feature-boundary.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-dashboard-feature-boundary.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-orders-feature-boundary.ps1
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

Visual and interaction smoke covers light/dark, 360/412/600 dp, larger font, no overflow, search/filter/pagination, detail loading/content/error, Back during loading, offline retention and session replacement. Fixtures and screenshots must use synthetic data.

## Completion criteria

The change is complete only when:

1. `feature/orders` owns list and detail state, coordination and UI.
2. `MainActivity` no longer stores or loads order DTO state.
3. The legacy screen no longer contains private order list/detail composables.
4. The two lifecycle defects are covered by passing tests.
5. All automated gates and available device visual checks have recorded evidence.
6. API, version, workflow, tag and APK publication remain unchanged.
7. README and canonical project memory record only verified facts.

Release/version work requires a separate explicit decision after integration.
