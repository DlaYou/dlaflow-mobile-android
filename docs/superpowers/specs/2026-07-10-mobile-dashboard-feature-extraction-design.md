# Mobile Dashboard Feature Extraction Design

**Status:** Approved by the product owner on 2026-07-10

**Repository:** `DlaYou/dlaflow-mobile-android`

**Base:** `origin/main` at Android Mobile Assistant 0.4.2

**Scope:** Phase 4 dashboard extraction from the approved Mobile Design System + Architecture Foundation

## Context

The Android architecture foundation and `feature/pairing` extraction are complete, but the dashboard is still split across three legacy owners:

- `MainActivity` stores the nullable `MobileAssistantDashboard` transport model and performs asynchronous refreshes;
- `MobileAssistantScreen.kt` owns the dashboard layout, its actions, notification preview and photo-task presentation;
- `mobile_api.kt` owns the response models and JSON parser.

This keeps platform orchestration, transport state and Compose presentation coupled. It also allows stale callbacks to update state after a session change and prevents the shared `DlaFlowUiState` contract from governing dashboard loading and failures.

The backend `GET /api/mobile/assistant/dashboard` contract is already lightweight, tenant-scoped and permission-aware. This project does not change that endpoint.

## Goals

1. Extract the dashboard vertically into `pl.dlaflow.mobile.feature.dashboard`.
2. Preserve the current product behavior and visual appearance one-to-one.
3. Move dashboard loading, refresh, mapping and stale-response protection behind an explicit feature contract.
4. Make `MainActivity` a platform and cross-feature adapter rather than the owner of dashboard data.
5. Use shared DlaFlow state, error, string and design-system standards.
6. Add deterministic tests and a dashboard package-boundary gate.
7. Keep the current endpoint, authentication, DTO format and release metadata unchanged.

## Non-goals

- No dashboard redesign or new business functionality.
- No backend, database, permission or API response changes.
- No Retrofit, Hilt, Room, Navigation Compose or Gradle multi-module migration.
- No extraction of orders, products, notifications, scanner or photo tasks as complete features in this change.
- No Android version bump, release tag, APK publication or production rollout.
- No signed-transport split; that remains Phase 5 / Plan 3.

## Chosen approach

The approved approach is a full vertical feature extraction. A UI-only move was rejected because it would leave state and networking in `MainActivity`. A combined dashboard, notifications and photo-task extraction was rejected because it would create an oversized change with multiple independent acceptance surfaces.

The feature will contain:

- `DashboardContract.kt` — immutable UI state, presentation models and typed user actions;
- `DashboardGateway.kt` — the single loading boundary;
- `DashboardStateHolder.kt` — transitions, request identity and last-content retention;
- `DashboardCoordinator.kt` — background execution and main-thread result delivery;
- `DashboardMapper.kt` — legacy transport DTO to dashboard presentation mapping;
- `DashboardFailure.kt` — dashboard-specific classification built on the shared mobile error mapper;
- `DashboardScreen.kt` — the complete Compose dashboard surface;
- private dashboard components and focused tests.

The existing `MobileApiClient` remains the transport implementation. `MainActivity` supplies it through a small `DashboardGateway` adapter, following the already approved pairing pattern. The temporary dependency on the legacy dashboard DTO is removed only when transport ownership moves in Plan 3.

## Dependency and data flow

```text
MainActivity
  -> DashboardCoordinator
    -> DashboardGateway
      -> MobileApiClient.getAssistantDashboard(token)
        -> current response DTO
      -> DashboardMapper
    -> DashboardStateHolder
  -> DashboardUiState
    -> DashboardScreen
```

Rules:

- `feature/dashboard` must not import `MainActivity` or implementations of orders, products, scanner or notifications.
- The screen emits typed `DashboardAction` values. It does not navigate or start Android activities directly.
- `MainActivity` handles platform launchers and translates cross-feature actions into the existing tab/overlay operations.
- `MainActivity` no longer stores a raw `MobileAssistantDashboard?` and no longer contains `refreshAssistantDashboard`.
- The transitional `MobileAssistantScreen` host may read a dashboard presentation projection for header, More, Messages and Products fallback behavior, but it must not receive the transport DTO.
- Later feature extractions will remove those remaining host projections one area at a time.

## Presentation contract

`DashboardContent` is an immutable presentation snapshot containing only values required by the current mobile UI:

- user and tenant display names;
- revenue and seven-day trend;
- dashboard KPI values;
- notification counts and up to four preview rows;
- caller-ID display status;
- optional active photo-task projection;
- generation timestamp if required for diagnostics or freshness decisions.

It never contains bearer tokens, tenant IDs, raw payloads or backend-only metadata.

Typed actions cover the existing interactions:

- refresh dashboard;
- scan package;
- open the current product/photo work path;
- open statistics/orders;
- open products;
- open notifications;
- take, pick or complete an active photo task.

Names in code may be more precise than the existing button copy, but the visible labels and behavior remain unchanged in this extraction.

## State model

`DashboardUiState` wraps the shared `DlaFlowUiState<DashboardContent>` and adds refresh/request metadata required by this feature. It represents:

- initial loading;
- current content;
- offline with optional last content;
- terminal error without content;
- whole-screen no-access;
- an in-progress refresh while existing content stays visible.

A valid response containing zero revenue, zero KPIs or no notifications is `Content`, not `Empty`.

### Transitions

1. First load starts as `Loading`.
2. A successful response maps and replaces the whole presentation snapshot atomically.
3. Manual refresh keeps the previous snapshot visible and marks the state as refreshing.
4. Offline failure produces `Offline(lastContent)`.
5. A retryable non-offline failure with existing data retains `Content` and emits a controlled transient message.
6. A failure without previous data produces `Error`.
7. HTTP 403 produces `NoAccess` without technical backend wording.
8. HTTP 401 follows the existing revoked-session path and asks the host to clear the local session.

The initial loading surface keeps the existing zero/empty visual placeholders, so this architectural extraction does not introduce a new skeleton or layout change. A future UX project may redesign these states separately.

## Concurrency and session safety

Every load receives a monotonically increasing request ID and the session identity expected by that request.

- Only the newest active request may update state.
- Starting a newer refresh invalidates the previous request result.
- Resetting the feature on logout, revocation or session replacement invalidates all active requests.
- Both success and failure callbacks validate request identity before mutating state or emitting host effects.
- The coordinator performs work on the existing executor and posts accepted results to the main thread.

This prevents a delayed network response from restoring dashboard data after a phone is disconnected or another session becomes active.

## UI ownership and design-system rules

`DashboardScreen` owns the complete dashboard composition:

- greeting;
- today's revenue card and trend chart;
- four KPI tiles;
- recent notification preview;
- quick actions;
- active photo-task presentation.

The extraction preserves current order, spacing, dimensions, colors, typography and interaction behavior. Hard-coded user-facing dashboard copy moves to `strings.xml`.

Visual pieces already used by both the dashboard and another host surface must become neutral shared presentation components with primitive parameters. They must not accept transport DTOs or import feature packages. New shared components require real call sites; this change must not build an unused parallel component library.

All UI continues to use the established DlaFlow theme and `core/designsystem` tokens. Light/dark behavior, safe insets, scrolling, larger font sizes and narrow/wide layouts remain required acceptance surfaces.

## Compatibility boundaries

The dashboard snapshot currently supplies small projections used outside the dashboard:

- notification badge and notification fallback rows;
- active photo-task fallback;
- user, tenant and caller-ID display text;
- KPI-derived bottom-navigation badge behavior.

During this incremental extraction, the legacy host reads those values from `DashboardContent?`. It may not access the API DTO. This is an explicit temporary compatibility seam, not a second dashboard state system.

## Testing strategy

Implementation follows TDD. Focused unit tests cover:

- mapping of full, partial/defaulted and business-empty snapshots;
- initial load to content;
- refresh while retaining content;
- offline, retryable error, no-access and revoked-session behavior;
- stale success and stale failure rejection;
- request invalidation after reset/session replacement;
- every typed dashboard action;
- active photo-task fallback semantics;
- notification preview and badge projections.

Existing notification and photo-task tests must be updated away from brittle source-string assertions where practical. They must continue proving the same user behavior.

A new `scripts/verify-dashboard-feature-boundary.ps1` gate verifies at minimum:

- required dashboard feature files exist;
- dashboard UI/state/coordinator no longer live in `MainActivity` or as private dashboard composables in the legacy host;
- `MainActivity` does not own `assistantDashboard` or `refreshAssistantDashboard`;
- the feature does not import Android activity implementations or other feature implementations;
- the current API endpoint and release metadata are unchanged.

## Verification matrix

Required automated gate:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-repository-contract.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-design-system-boundary.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-pairing-feature-boundary.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-dashboard-feature-boundary.ps1
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

Required visual/interaction verification:

- light and dark mode;
- 360 dp, 412 dp and 600 dp widths;
- increased system font size;
- no horizontal overflow or clipped actions;
- dashboard scrolling and Android Back behavior;
- notification opening, every quick action and active photo-task actions;
- refresh success, offline refresh and revoked-session behavior.

Visual acceptance is zero intended difference from the current 0.4.2 dashboard.

## Completion criteria

The change is complete only when:

1. The dashboard is owned by `feature/dashboard` end to end.
2. `MainActivity` no longer stores or refreshes the raw dashboard DTO.
3. The legacy host receives presentation state/projections only.
4. All old and new automated gates pass.
5. Required visual and interaction checks have recorded evidence.
6. No API, version, tag or APK publication change is present.
7. Repository documentation records the new feature boundary and verification command.

Release/version work, if desired, requires a separate explicit decision after this feature branch is integrated.
