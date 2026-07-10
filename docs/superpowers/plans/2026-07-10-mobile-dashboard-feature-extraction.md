# Mobile Dashboard Feature Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the current Android dashboard into `feature/dashboard` with typed state, safe asynchronous refreshes and presentation models while preserving the Mobile Assistant 0.4.2 UI and behavior.

**Architecture:** Follow the existing pairing pattern: a feature-owned state holder and coordinator consume a small gateway implemented by `MainActivity`. The current `MobileApiClient` and dashboard response DTO remain transport code; a feature mapper converts them to an immutable `DashboardContent`. The legacy host receives presentation state/projections only until later feature extractions remove those compatibility seams.

**Tech Stack:** Kotlin, Jetpack Compose, Android resources, JUnit 4, Gradle 8.13, PowerShell repository gates.

## Global Constraints

- Work only in the standalone Android repository and the `codex/mobile-dashboard-extraction` worktree.
- Preserve the current dashboard appearance and behavior one-to-one; no redesign or new business functionality.
- Do not change `GET /api/mobile/assistant/dashboard`, backend code, response JSON, authentication or permissions.
- Do not add Retrofit, Hilt, Room, Navigation Compose, FCM, a new Gradle module or another state system.
- Do not bump `versionCode` or `versionName`, create a `mobile-v*` tag, publish an APK or change production manifests.
- Never persist or log a bearer token, tenant ID, raw payload or customer-sensitive response.
- Use `DlaFlowUiState`, `DlaFlowUiMessage`, `mobileErrorToUiMessage`, the current executor/main-thread bridge and existing design-system tokens/components.
- Keep zero revenue, zero KPIs and an empty notification list as valid `Content`, not `Empty`.
- Preserve unrelated changes and do not modify panel/API files.
- Implement every behavior through RED -> GREEN -> focused test -> commit.

---

## File map

**Create under `app/src/main/java/pl/dlaflow/mobile/feature/dashboard/`:**

- `DashboardContract.kt` — presentation models, UI state, load request, action/effect enums and content projection helper.
- `DashboardMapper.kt` — transport DTO to presentation mapping only.
- `DashboardStateHolder.kt` — deterministic transitions, request identity and reset invalidation.
- `DashboardGateway.kt` — one synchronous load boundary.
- `DashboardFailure.kt` — dashboard failure classification.
- `DashboardCoordinator.kt` — executor orchestration and main-thread result acceptance.
- `DashboardScreen.kt` — dashboard composition and dashboard-private UI.

**Create under `app/src/test/java/pl/dlaflow/mobile/feature/dashboard/`:**

- `DashboardMapperTest.kt`
- `DashboardStateHolderTest.kt`
- `DashboardCoordinatorTest.kt`
- `DashboardActionTest.kt`

**Create:**

- `scripts/verify-dashboard-feature-boundary.ps1`

**Modify:**

- `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt`
- `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- `app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowComponents.kt` only for display primitives with at least two real call sites.
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/pl/dlaflow/mobile/MobileNotificationsTest.kt`
- `app/src/test/java/pl/dlaflow/mobile/ProductPhotoTaskActionTest.kt`
- `README.md`

---

### Task 1: Dashboard presentation contract, mapper and deterministic state

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardContract.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardMapper.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardStateHolder.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/feature/dashboard/DashboardMapperTest.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/feature/dashboard/DashboardStateHolderTest.kt`

**Interfaces:**
- Consumes: existing `MobileAssistantDashboard`, `MobileAssistantKpis`, `MobileAssistantNotification`, `MobileAssistantPhotoTask`, `MobileAssistantTrendPoint`, `DlaFlowUiState`, `DlaFlowUiMessage`.
- Produces: `DashboardContent`, `DashboardUiState`, `DashboardLoadRequest`, `DashboardAction`, `DashboardFeedback`, `MobileAssistantDashboard.toDashboardContent()`, and `DashboardStateHolder`.

- [ ] **Step 1: Write failing mapper and state tests**

Create tests that use concrete fixtures and assert these exact behaviors:

```kotlin
@Test
fun `transport dashboard maps to presentation snapshot without secrets`() {
    val content = dashboardDto().toDashboardContent()

    assertEquals("Maciej", content.userName)
    assertEquals("DlaFlow", content.tenantName)
    assertEquals(350.0, content.todayRevenue, 0.0)
    assertEquals(2, content.kpis.newOrders)
    assertEquals("notification-1", content.notifications.single().id)
    assertEquals("task-1", content.activePhotoTask?.id)
    assertEquals(listOf(100.0, 350.0), content.trend.map { it.revenue })
}

@Test
fun `zero business snapshot remains content`() {
    val holder = DashboardStateHolder()
    val request = holder.beginLoad("session-a")
    val zero = dashboardDto(
        todayRevenue = 0.0,
        notifications = emptyList(),
        activePhotoTask = null,
    ).toDashboardContent()

    assertTrue(holder.acceptSuccess(request, zero))
    assertEquals(DlaFlowUiState.Content(zero), holder.state.contentState)
}

@Test
fun `new request keeps content visible and rejects stale result`() {
    val holder = DashboardStateHolder()
    val first = holder.beginLoad("session-a")
    val old = dashboardDto(todayRevenue = 100.0).toDashboardContent()
    assertTrue(holder.acceptSuccess(first, old))

    val stale = holder.beginLoad("session-a")
    val current = holder.beginLoad("session-a")
    assertEquals(old, holder.state.contentOrNull())
    assertTrue(holder.state.isRefreshing)
    assertFalse(holder.acceptSuccess(stale, dashboardDto(todayRevenue = 200.0).toDashboardContent()))
    assertTrue(holder.acceptSuccess(current, dashboardDto(todayRevenue = 300.0).toDashboardContent()))
    assertEquals(300.0, holder.state.contentOrNull()?.todayRevenue, 0.0)
}

@Test
fun `reset invalidates active request without reusing its id`() {
    val holder = DashboardStateHolder()
    val stale = holder.beginLoad("session-a")
    holder.reset()
    val current = holder.beginLoad("session-b")

    assertFalse(holder.acceptSuccess(stale, dashboardDto().toDashboardContent()))
    assertTrue(current.requestId > stale.requestId)
}
```

The fixture must construct the current DTO classes explicitly; do not parse JSON or use reflection.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Maciej\AppData\Local\Android\Sdk'
.\gradlew.bat :app:testDebugUnitTest --tests "pl.dlaflow.mobile.feature.dashboard.*" --no-daemon
```

Expected: compilation fails because `feature.dashboard` contracts do not exist.

- [ ] **Step 3: Add the minimal presentation contract**

Implement these exact public-to-module shapes in `DashboardContract.kt`:

```kotlin
internal data class DashboardKpis(val newOrders: Int, val toShip: Int, val overdueOrProblems: Int, val messages: Int)
internal data class DashboardNotification(
    val id: String,
    val title: String,
    val description: String,
    val tone: String,
    val source: String,
    val account: String,
    val occurredAt: String,
    val readAt: String?,
    val actionType: String,
    val actionLabel: String,
)
internal data class DashboardNotificationSummary(val unreadCount: Int, val unreadAttentionCount: Int)
internal data class DashboardTrendPoint(val date: String, val orders: Int, val revenue: Double)
internal data class DashboardCallerIdStatus(val enabled: Boolean, val label: String)
internal data class DashboardPhotoTask(
    val id: String,
    val productName: String,
    val productSku: String,
    val productImage: String,
    val status: String,
    val mediaCount: Int,
    val maxPhotos: Int,
    val expiresAt: String,
)
internal data class DashboardContent(
    val userName: String,
    val tenantName: String,
    val todayRevenue: Double,
    val revenueChangePercent: Double,
    val kpis: DashboardKpis,
    val notificationSummary: DashboardNotificationSummary,
    val notifications: List<DashboardNotification>,
    val activePhotoTask: DashboardPhotoTask?,
    val callerIdStatus: DashboardCallerIdStatus,
    val trend: List<DashboardTrendPoint>,
    val generatedAt: String,
)
internal data class DashboardUiState(
    val contentState: DlaFlowUiState<DashboardContent> = DlaFlowUiState.Loading,
    val isRefreshing: Boolean = false,
    val activeRequestId: Long? = null,
    val transientMessage: DlaFlowUiMessage? = null,
)
internal data class DashboardLoadRequest(val requestId: Long, internal val sessionKey: String)
internal sealed interface DashboardAction {
    data object Refresh : DashboardAction
    data object ScanPackage : DashboardAction
    data object OpenProductWork : DashboardAction
    data object OpenStatistics : DashboardAction
    data object OpenProducts : DashboardAction
    data object OpenNotifications : DashboardAction
    data class TakePhoto(val taskId: String) : DashboardAction
    data class PickPhoto(val taskId: String) : DashboardAction
    data class CompletePhotoTask(val taskId: String) : DashboardAction
}
internal enum class DashboardFeedback { REFRESHING, REFRESHED, LOAD_FAILED }
```

Add `DashboardUiState.contentOrNull()` that returns data from `Content` and `Offline(lastContent)`, otherwise `null`.

- [ ] **Step 4: Implement the mapper with field-for-field copies**

`MobileAssistantDashboard.toDashboardContent()` must map every field, create immutable list copies with `map`, flatten notification `mobileAction` into `actionType`/`actionLabel`, and never retain the transport object.

- [ ] **Step 5: Implement state transitions**

`DashboardStateHolder` must expose `var state by mutableStateOf(DashboardUiState()); private set`, keep `nextRequestId` monotonic and keep the active session key private. Implement:

```kotlin
fun beginLoad(sessionKey: String): DashboardLoadRequest
fun acceptSuccess(request: DashboardLoadRequest, content: DashboardContent): Boolean
fun acceptOffline(request: DashboardLoadRequest, message: DlaFlowUiMessage): Boolean
fun acceptFailure(request: DashboardLoadRequest, message: DlaFlowUiMessage): Boolean
fun acceptNoAccess(request: DashboardLoadRequest): Boolean
fun acceptUnauthorized(request: DashboardLoadRequest): Boolean
fun reset()
```

All accept methods must return `false` without mutation unless both request ID and session key match. `beginLoad` keeps existing content visible and sets `isRefreshing=true`; the first load uses `Loading`. Offline keeps last content in `DlaFlowUiState.Offline`. Generic failure keeps existing content as `Content` plus `transientMessage`; without content it uses `Error`. Accepted terminal results clear `activeRequestId` and `isRefreshing`. `acceptUnauthorized` validates the request, resets feature state and preserves monotonic request numbering.

- [ ] **Step 6: Run focused tests and verify GREEN**

Run the Task 1 command. Expected: all dashboard mapper/state tests pass.

- [ ] **Step 7: Commit Task 1**

```powershell
git add app/src/main/java/pl/dlaflow/mobile/feature/dashboard app/src/test/java/pl/dlaflow/mobile/feature/dashboard
git commit -m "feat: add mobile dashboard state contract"
```

---

### Task 2: Gateway, failure mapping and coordinator

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardGateway.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardFailure.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardCoordinator.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/feature/dashboard/DashboardCoordinatorTest.kt`

**Interfaces:**
- Consumes: Task 1 contracts and mapper, `MobileAssistantDashboard`, `MobileApiException`, `mobileErrorToUiMessage`, `Executor`.
- Produces: `DashboardGateway.load(token)`, `mapDashboardFailure(Throwable)`, `DashboardCoordinator.refresh(token, showFeedback)` and `DashboardCoordinator.reset()`.

- [ ] **Step 1: Write coordinator RED tests with a queued executor**

Use a fake `Executor` that stores `Runnable`s and a fake main-thread queue. Cover:

```kotlin
@Test fun `successful refresh maps response and emits refreshed feedback`()
@Test fun `latest refresh wins when callbacks complete out of order`()
@Test fun `offline refresh retains last content`()
@Test fun `forbidden response becomes no access`()
@Test fun `unauthorized response clears feature and emits session revoked once`()
@Test fun `reset before callback prevents state and effect delivery`()
```

Use `MobileApiException(401, "AUTH_REQUIRED", "expired")`, `MobileApiException(403, "MOBILE_DEVICE_REQUIRED", "forbidden")` and `UnknownHostException("offline")` as concrete failures. Assert state and typed effects, not implementation strings.

- [ ] **Step 2: Run focused coordinator tests and verify RED**

Run the Task 1 Gradle command. Expected: coordinator/gateway symbols are missing.

- [ ] **Step 3: Implement the gateway and failure classifier**

Use:

```kotlin
internal fun interface DashboardGateway {
    fun load(token: String): MobileAssistantDashboard
}

internal sealed interface DashboardFailure {
    data object Unauthorized : DashboardFailure
    data object NoAccess : DashboardFailure
    data class Offline(val message: DlaFlowUiMessage) : DashboardFailure
    data class Retryable(val message: DlaFlowUiMessage) : DashboardFailure
}
```

Classify HTTP 401, HTTP 403, `UnknownHostException`/`ConnectException`, and all remaining errors in that order. Reuse `mobileErrorToUiMessage`; do not copy error strings.

- [ ] **Step 4: Implement the coordinator**

Constructor dependencies:

```kotlin
internal class DashboardCoordinator(
    private val stateHolder: DashboardStateHolder,
    private val gateway: DashboardGateway,
    private val executor: Executor,
    private val postToMain: (() -> Unit) -> Unit,
    private val onFeedback: (DashboardFeedback) -> Unit,
    private val onSessionRevoked: () -> Unit,
)
```

`refresh(token, showFeedback)` calls `beginLoad(token)`, optionally emits `REFRESHING`, performs `gateway.load(token).toDashboardContent()` on the executor, posts the result, and emits success/failure effects only when the state holder accepts that exact request. Unauthorized must call `acceptUnauthorized(request)` and emit `onSessionRevoked` only when it returns `true`. `reset()` delegates to the state holder. No background callback may mutate Compose state directly.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run the Task 1 command. Expected: all dashboard tests pass.

- [ ] **Step 6: Commit Task 2**

```powershell
git add app/src/main/java/pl/dlaflow/mobile/feature/dashboard app/src/test/java/pl/dlaflow/mobile/feature/dashboard
git commit -m "feat: coordinate mobile dashboard refreshes"
```

---

### Task 3: Extract the Compose dashboard and typed actions

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardScreen.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt:145-150,602-844,3204-3634`
- Modify: `app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowComponents.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/pl/dlaflow/mobile/feature/dashboard/DashboardActionTest.kt`
- Modify test: `app/src/test/java/pl/dlaflow/mobile/MobileNotificationsTest.kt:45-52`

**Interfaces:**
- Consumes: `DashboardUiState`, `DashboardContent`, `DashboardAction`, `DlaFlowComposeColors`.
- Produces: `DashboardFeatureScreen(colors, sessionUserName, state, fallbackPhotoTask, onAction)`.

- [ ] **Step 1: Write failing action and source-boundary tests**

Test a pure mapping function used by quick buttons:

```kotlin
@Test
fun `dashboard quick actions preserve current destinations`() {
    assertEquals(DashboardAction.ScanPackage, dashboardQuickAction(0))
    assertEquals(DashboardAction.OpenProductWork, dashboardQuickAction(1))
    assertEquals(DashboardAction.OpenStatistics, dashboardQuickAction(2))
    assertEquals(DashboardAction.OpenProducts, dashboardQuickAction(3))
}
```

Replace the notification source-string test with assertions against the new `DashboardScreen.kt`: it must contain the `OPEN_NOTIFICATIONS` action and must not import `MainActivity` or `MobileApiClient`.

- [ ] **Step 2: Run focused tests and verify RED**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Maciej\AppData\Local\Android\Sdk'
.\gradlew.bat :app:testDebugUnitTest --tests "pl.dlaflow.mobile.feature.dashboard.*" --tests "pl.dlaflow.mobile.MobileNotificationsTest" --no-daemon
```

Expected: `DashboardFeatureScreen`/action mapping does not exist and the old source assertion fails after its target is changed.

- [ ] **Step 3: Move dashboard composition without changing dimensions or order**

Move these exact existing symbols from `MobileAssistantScreen.kt` to `DashboardScreen.kt`, preserving bodies before substituting presentation types and string resources:

```text
DashboardTab -> DashboardFeatureScreen
GreetingRow
RevenueCard
RevenueTrendChart
revenueSparklinePoints
KpiGrid
KpiTile
QuickActions
QuickActionButton
NotificationsList
```

The new top-level call order must remain:

```kotlin
GreetingRow(colors, content?.userName ?: sessionUserName) { onAction(DashboardAction.Refresh) }
RevenueCard(colors, content)
KpiGrid(colors, content?.kpis)
NotificationsList(colors, content?.notifications.orEmpty()) { onAction(DashboardAction.OpenNotifications) }
QuickActions(colors, onAction)
ActivePhotoTaskSection(colors, content?.activePhotoTask, fallbackPhotoTask, onAction)
```

The screen reads `state.contentOrNull()`. When content is absent it renders the same current zero/empty fallbacks. When content is `Offline(lastContent)`, it renders `lastContent`. Dashboard buttons emit `DashboardAction`; photo buttons emit `TakePhoto(taskId)`, `PickPhoto(taskId)` and `CompletePhotoTask(taskId)`. Do not add a new skeleton, banner, card, dialog or animation.

- [ ] **Step 4: Preserve shared rows without cross-feature DTO dependencies**

Any notification or photo-task presentation used both in dashboard and the legacy host must be lifted to `core/designsystem` as a display-only composable accepting primitive text/count/icon/color/callback arguments. It must not accept `MobileAssistantDashboard`, `MobileAssistantNotification`, `MobilePhotoTask` or a `feature.*` type. Keep the current pixel values and require two real call sites before retaining the shared primitive.

- [ ] **Step 5: Move dashboard copy to resources**

Add named resources for the current visible dashboard strings, including greeting/subtitle, revenue labels, KPI labels, quick-action labels/subtitles, notifications heading/link/empty copy and photo-task labels. Use `stringResource` and formatted resources for user name, percentage, SKU and photo counts. Do not rewrite Polish copy in this extraction.

- [ ] **Step 6: Run focused tests and verify GREEN**

Run the Task 3 command. Expected: dashboard and notification tests pass.

- [ ] **Step 7: Commit Task 3**

```powershell
git add app/src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardScreen.kt app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowComponents.kt app/src/main/res/values/strings.xml app/src/test/java/pl/dlaflow/mobile/feature/dashboard/DashboardActionTest.kt app/src/test/java/pl/dlaflow/mobile/MobileNotificationsTest.kt
git commit -m "refactor: extract mobile dashboard compose screen"
```

---

### Task 4: Integrate the feature and remove dashboard ownership from MainActivity

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt:107-129,337-402,696-868,1182-1207,2139-2155`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt:363-784,846-920,1280-1360,2500-2580,2870-2910,3940-3980`
- Modify test: `app/src/test/java/pl/dlaflow/mobile/ProductPhotoTaskActionTest.kt`

**Interfaces:**
- Consumes: Tasks 1-3 feature API.
- Produces: a feature-owned dashboard state wired to all current host projections and typed action routing.

- [ ] **Step 1: Add a RED integration/source test**

Add a test that reads `MainActivity.kt` and asserts all of these are absent:

```kotlin
assertFalse(source.contains("private var assistantDashboard"))
assertFalse(source.contains("private fun refreshAssistantDashboard("))
assertFalse(source.contains("MobileAssistantQuickAction"))
```

Also assert `DashboardStateHolder`, `DashboardCoordinator` and `DashboardGateway` are wired in the activity.

- [ ] **Step 2: Run the integration test and verify RED**

Run the Task 3 test command. Expected: source ownership assertions fail.

- [ ] **Step 3: Wire state holder, gateway and coordinator**

Follow the pairing construction pattern:

```kotlin
private val dashboardStateHolder = DashboardStateHolder()
private val dashboardCoordinator by lazy {
    DashboardCoordinator(
        stateHolder = dashboardStateHolder,
        gateway = DashboardGateway { token -> mobileApiClientForSession(sessionStore).getAssistantDashboard(token) },
        executor = executor,
        postToMain = { action -> runOnUiThread(action) },
        onFeedback = ::handleDashboardFeedback,
        onSessionRevoked = { clearDisconnectedSession("Telefon został odłączony w panelu. Sparuj go ponownie.") },
    )
}
```

Use the current status strings for `REFRESHING`, `REFRESHED` and `LOAD_FAILED`. Replace both `refreshAssistantDashboard(showLoading = true)` and `refreshAssistantDashboard(showLoading = false)` with the matching `dashboardCoordinator.refresh(session.token, showFeedback = true/false)` call. `clearDisconnectedSession` must call `dashboardCoordinator.reset()`.

- [ ] **Step 4: Route typed actions without changing behavior**

Replace `handleQuickAction(MobileAssistantQuickAction)` with `handleDashboardAction(DashboardAction)` and preserve exact current destinations:

- `ScanPackage` starts the package scan;
- `OpenProductWork` uses `chooseProductPhotoTaskAction`, preferring loaded tasks and then `dashboardStateHolder.state.contentOrNull()?.activePhotoTask?.id`;
- `OpenStatistics` selects Orders, loads orders and keeps the current status copy;
- `OpenProducts` selects Products, loads products and keeps the current status copy;
- `OpenNotifications` opens the notification overlay;
- `Refresh` refreshes with feedback.

Route `TakePhoto`, `PickPhoto` and `CompletePhotoTask` to the existing platform callbacks without changing camera, picker or completion behavior.

- [ ] **Step 5: Replace all transport DTO host reads with presentation projections**

Change `MobileAssistantScreen` to accept `dashboardState: DashboardUiState`, derive one `val dashboard = dashboardState.contentOrNull()`, and update header badges, notification fallback, active photo task, caller ID, user/tenant labels and KPI badges to use `DashboardContent`. Remove the root `MobileAssistantQuickAction` enum and dashboard-private composables.

Update `ProductPhotoTaskActionTest` only where the dashboard projection type or source location changed; retain its three behavioral cases.

- [ ] **Step 6: Run all unit tests**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Maciej\AppData\Local\Android\Sdk'
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

Expected: all tests pass with no stale callback or existing behavior regression.

- [ ] **Step 7: Commit Task 4**

```powershell
git add app/src/main/java/pl/dlaflow/mobile/MainActivity.kt app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt app/src/test/java/pl/dlaflow/mobile/ProductPhotoTaskActionTest.kt app/src/test/java/pl/dlaflow/mobile/feature/dashboard
git commit -m "refactor: delegate dashboard ownership to feature"
```

---

### Task 5: Boundary gate and repository documentation

**Files:**
- Create: `scripts/verify-dashboard-feature-boundary.ps1`
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-07-10-mobile-dashboard-feature-extraction-design.md` only if implementation reveals a factual mismatch; do not silently expand scope.

**Interfaces:**
- Consumes: final package/file ownership from Tasks 1-4.
- Produces: an executable repository contract and documented required gate.

- [ ] **Step 1: Write and run a failing boundary script**

The script must use `$ErrorActionPreference = "Stop"`, `Set-StrictMode -Version Latest`, require all seven production feature files, join all dashboard Kotlin source, and reject:

```powershell
$forbiddenFeatureDependencies = @(
    "MainActivity",
    "MobileSessionStore(",
    "DlaFlowBackgroundSyncService",
    "IntentIntegrator",
    "startActivityForResult"
)
$forbiddenActivityOwnership = @(
    "private var assistantDashboard",
    "private fun refreshAssistantDashboard(",
    "MobileAssistantQuickAction"
)
$forbiddenLegacyScreenSymbols = @(
    "private fun DashboardTab(",
    "private fun RevenueCard(",
    "private fun KpiGrid(",
    "private fun QuickActions("
)
```

Run before completing integration. Expected: FAIL until all legacy ownership is removed.

- [ ] **Step 2: Complete the boundary script and make it pass**

Also assert the feature contains `DashboardStateHolder`, `DashboardCoordinator`, `DashboardGateway`, `DashboardFeatureScreen`, `DlaFlowUiState`, `activeRequestId` and `contentOrNull`. Print exactly `Mobile dashboard feature boundary: OK` on success.

- [ ] **Step 3: Update README architecture and verification commands**

Add `feature/dashboard` to the Architecture Foundation list and add the dashboard gate immediately after the pairing gate. State that dashboard transport still uses the current `MobileApiClient` until the approved transport phase; do not claim Plan 2 or the full foundation is complete.

- [ ] **Step 4: Run documentation and script checks**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-dashboard-feature-boundary.ps1
git diff --check
```

Expected: boundary OK and no whitespace errors.

- [ ] **Step 5: Commit Task 5**

```powershell
git add scripts/verify-dashboard-feature-boundary.ps1 README.md docs/superpowers/specs/2026-07-10-mobile-dashboard-feature-extraction-design.md
git commit -m "docs: enforce mobile dashboard feature boundary"
```

---

### Task 6: Full verification, visual smoke and project memory

**Files:**
- Modify in panel repository after evidence is known: `PROJECT_TODO.md`
- Modify `VPS_TODO.md` only if a real release/deploy/VPS action occurs; none is planned for this feature.

**Interfaces:**
- Consumes: completed feature branch.
- Produces: verification evidence, a clean Android worktree and an accurate project-memory entry.

- [ ] **Step 1: Run every repository gate**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-repository-contract.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-design-system-boundary.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-pairing-feature-boundary.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-dashboard-feature-boundary.ps1
$env:ANDROID_HOME='C:\Users\Maciej\AppData\Local\Android\Sdk'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

Expected: four boundary scripts report OK and Gradle reports `BUILD SUCCESSFUL`.

- [ ] **Step 2: Verify scope and release metadata**

```powershell
git diff origin/main..HEAD -- app/build.gradle.kts .github/workflows
git diff --check origin/main..HEAD
git status --short --branch
```

Expected: no version/workflow diff, no whitespace errors and no uncommitted files.

- [ ] **Step 3: Perform visual and interaction smoke**

Use an emulator or connected physical phone and record results for:

- light and dark mode;
- 360 dp, 412 dp and 600 dp widths where available;
- increased system font size;
- no horizontal overflow or clipped quick actions;
- refresh success and refresh while offline with last content retained;
- notification opening, scan, product-work, statistics, products and photo-task actions;
- Android Back behavior;
- revoked session returning to pairing.

If a device configuration cannot be exercised, report it explicitly rather than claiming it passed.

- [ ] **Step 4: Update canonical project memory with validated facts**

Append a concise dated entry to the panel repository `PROJECT_TODO.md` containing Android repository, branch/commit or PR state, extracted boundary, exact automated gate results, visual-smoke evidence and the explicit statement that no APK release occurred. Do not modify `VPS_TODO.md` unless the scope changes to a real deployment.

- [ ] **Step 5: Final review before integration**

Use `superpowers:requesting-code-review`, fix verified findings through TDD, rerun Task 6 Steps 1-2, and only then use `superpowers:finishing-a-development-branch`. Do not merge, push, tag or publish without the corresponding explicit user decision.
