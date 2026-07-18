# Mobile Orders Feature Extraction Implementation Plan

> **For agentic workers:** implement task-by-task with TDD, independent review and a clean handoff.

**Goal:** Extract Android orders and order detail into `feature/orders` while preserving Mobile Assistant 0.4.4 behavior and closing stale-detail lifecycle defects.

**Architecture:** A feature-owned state holder and coordinator consume a small gateway instantiated by `MainActivity`. Current transport DTOs remain behind gateway/mapper seams. List and detail have independent request chains. Scanner/KPI content and signed thumbnails enter through neutral host boundaries.

**Tech stack:** Kotlin, Jetpack Compose, Android resources, JUnit 4, Compose UI instrumentation, Gradle 8.13, PowerShell repository gates.

## Global constraints

- Work only in `codex/mobile-orders-feature-extraction` under the isolated Android worktree.
- Preserve current UI, copy, endpoints and navigation; no redesign or debounce.
- Do not change backend/API/auth/permissions or move transport parsers in this phase.
- Do not add a new DI, navigation, persistence, networking or Gradle-module system.
- Never place token, tenant ID, raw payload or customer data in state, logs, docs or fixtures.
- Do not bump version, tag, publish APK or deploy.
- Follow RED -> GREEN and commit coherent verified slices.

## File map

Create under `app/src/main/java/pl/dlaflow/mobile/feature/orders/`:

- `OrdersContract.kt`
- `OrdersGateway.kt`
- `OrdersMapper.kt`
- `OrdersFailure.kt`
- `OrdersStateHolder.kt`
- `OrdersCoordinator.kt`
- `OrdersScreen.kt`
- `OrderDetailPanel.kt`

Create focused tests under `app/src/test/java/pl/dlaflow/mobile/feature/orders/` and device tests under `app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/`.

Modify only the necessary host, API tests, design-system/resources, boundary scripts and README files.

### Task 1: Lock the transport and presentation contract

- [ ] Add RED parser tests for full/partial list pages and full/partial detail.
- [ ] Add RED mapper tests for list rows, detail projection, filters and business-empty/default values.
- [ ] Create immutable presentation models, query, route and typed action contract.
- [ ] Implement transitional DTO mapping without parser/API behavior changes.
- [ ] Run focused tests GREEN.
- [ ] Commit the contract slice.

### Task 2: Implement deterministic list/detail state

- [ ] Add RED tests for initial/content/empty, refresh retention and offline retention.
- [ ] Add RED pagination merge, duplicate suppression and overlapping-load protection tests.
- [ ] Add RED stale success/failure tests for both request chains.
- [ ] Add RED regression tests: close during detail loading and list reset invalidating detail.
- [ ] Add RED reset/session replacement and whole-feature 403 tests.
- [ ] Implement `OrdersStateHolder` with monotonically increasing IDs and session matching.
- [ ] Run focused tests GREEN.
- [ ] Commit the state slice.

### Task 3: Coordinate gateway work and failures

- [ ] Add RED queued-executor tests for list, load more and detail.
- [ ] Add RED 401 confirmation/retry-once, 403, offline and detail-404 tests.
- [ ] Implement gateway, failure classifier and main-thread coordinator.
- [ ] Ensure stale callbacks emit neither state nor feedback effects.
- [ ] Run focused tests GREEN.
- [ ] Commit the coordinator slice.

### Task 4: Extract Compose orders surfaces

- [ ] Add RED action and Compose instrumentation tests for loading, empty, no-access, offline/error and content.
- [ ] Add RED interaction tests for search, filters, load more, open detail, close detail, retry and 48 dp semantics.
- [ ] Add only real shared design-system state/skeleton primitives.
- [ ] Add a neutral signed-thumbnail loader boundary with no token/client in Compose state.
- [ ] Move list composition to `OrdersScreen.kt` and detail to `OrderDetailPanel.kt` one-to-one.
- [ ] Move visible orders copy to resources without rewriting it.
- [ ] Preserve the host `leadContent` slot for scanner and KPI content.
- [ ] Run unit and device-focused tests GREEN.
- [ ] Commit the Compose slice.

### Task 5: Integrate host and remove legacy ownership

- [ ] Add RED source/integration assertions for removed activity fields/loaders and legacy private composables.
- [ ] Wire state holder, gateway and coordinator using the existing client, executor and main-thread bridge.
- [ ] Route scanner order numbers and tab actions through typed `OrdersAction`.
- [ ] Route Android Back through `CloseDetail`, including loading state.
- [ ] Reset the feature on logout, revocation and session replacement.
- [ ] Remove legacy list/detail state, loaders, helpers and unused request counters.
- [ ] Run all unit tests GREEN.
- [ ] Commit the integration slice.

### Task 6: Enforce boundary and documentation

- [ ] Create a RED `scripts/verify-orders-feature-boundary.ps1`.
- [ ] Require eight production files and restrict `MobileOrder*` DTOs to gateway/mapper.
- [ ] Reject activity ownership, legacy private order UI and cross-feature/platform imports.
- [ ] Confirm both `/api/mobile/orders` endpoints and unchanged release metadata.
- [ ] Update README architecture and verification commands without claiming Plan 2 complete.
- [ ] Run the boundary GREEN and `git diff --check`.
- [ ] Commit the gate/docs slice.

### Task 7: Full verification, review and handoff

- [ ] Run repository, design-system, pairing, dashboard and orders boundary scripts.
- [ ] Run `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- [ ] Run `:app:connectedDebugAndroidTest` on an available emulator/device.
- [ ] Smoke light/dark, 360/412/600 dp, font 130%, no overflow, list/detail/Back/offline/session reset using synthetic data.
- [ ] Verify no version/workflow/API drift and no secrets/customer data.
- [ ] Request independent code review and fix verified findings through tests.
- [ ] Update panel `PROJECT_TODO.md` with exact evidence; update `VPS_TODO.md` only if deployment scope changes.
- [ ] Produce a clean committed handoff. Do not merge, push, tag or publish without explicit authorization.
