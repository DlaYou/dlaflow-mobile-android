# Mobile Orders Compact Scanner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce the Orders screen to three visible filters and move package scanning into a contextual header action with a compact result strip.

**Architecture:** Preserve the existing mobile API, signed transport, scan launcher, `MobilePackageScanUiState`, and order actions. Add one feature-owned visible-filter definition, one reusable DlaFlow header icon primitive, and one Orders-owned scanner strip; `MobileAssistantScreen` remains the composition boundary that connects those pieces.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android resources, JUnit 4, Compose UI tests, Gradle.

---

## File Structure

- Modify `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersContract.kt`: define the three filters exposed by the mobile Orders UI without deleting compatibility enum values.
- Modify `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt`: render only the feature-owned visible filter set.
- Modify `app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersFeatureScreenTest.kt`: prove the three-filter contract and responsive layout.
- Modify `app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowComponents.kt`: add the shared 48 dp DlaFlow header icon action used by app-header tools.
- Modify `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`: connect the contextual QR action, reorder Orders lead content, and remove the large scanner card.
- Create `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersPackageScannerStrip.kt`: own compact rendering of scan loading, resolved, ambiguous, unmatched, and failed states.
- Create `app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersPackageScannerStripTest.kt`: verify scanner visibility, copy, actions, semantics, and compact layout.
- Modify `app/src/test/java/pl/dlaflow/mobile/MobilePackageScannerTest.kt`: verify contextual header-action policy while preserving existing scanner parsing coverage.
- Modify `app/src/main/res/values/strings.xml`: add operator-facing scanner strip and accessibility strings.
- Modify `D:/ECOM/Maciek/PANEL DLAYOU/PROJECT_TODO.md` after merge: record the verified mobile UI standard and evidence.

### Task 1: Expose Exactly Three Orders Filters

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersContract.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt`
- Test: `app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersFeatureScreenTest.kt`

- [ ] **Step 1: Write the failing visible-filter tests**

Replace the old five-filter layout expectation and add an explicit compatibility assertion:

```kotlin
@Test
fun ordersExposeOnlyThreePrimaryFilters() {
    setOrders(contentState(), mutableListOf())

    composeRule.onNodeWithText("Wszystkie").assertIsDisplayed()
    composeRule.onNodeWithText("Nowe").assertIsDisplayed()
    composeRule.onNodeWithText("Do wysyłki").assertIsDisplayed()
    composeRule.onNodeWithText("Problemy").assertDoesNotExist()
    composeRule.onNodeWithText("Wiadomości").assertDoesNotExist()
}

@Test
fun largeFontNarrowScreenWrapsThreeFiltersWithoutOverflow() {
    setOrders(contentState(), mutableListOf(), screenWidthDp = 360, fontScale = 1.3f)

    val allTop = composeRule.onNodeWithText("Wszystkie").fetchSemanticsNode().boundsInRoot.top
    val newTop = composeRule.onNodeWithText("Nowe").fetchSemanticsNode().boundsInRoot.top
    val toShipTop = composeRule.onNodeWithText("Do wysyłki").fetchSemanticsNode().boundsInRoot.top

    assertEquals(allTop, newTop, 0f)
    assertTrue(toShipTop > newTop)
}
```

- [ ] **Step 2: Run the focused instrumentation test to verify RED**

Run:

```powershell
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=pl.dlaflow.mobile.feature.orders.OrdersFeatureScreenTest --no-daemon
```

Expected: `ordersExposeOnlyThreePrimaryFilters` fails because `Problemy` and `Wiadomości` are still rendered.

- [ ] **Step 3: Add the single feature-owned visible filter set**

In `OrdersContract.kt` keep all enum cases and add:

```kotlin
internal val visibleOrdersFilters = listOf(
    OrdersFilter.ALL,
    OrdersFilter.NEW,
    OrdersFilter.TO_SHIP,
)
```

In `OrdersScreen.kt` replace `OrdersFilter.entries.chunked(columnCount)` with:

```kotlin
visibleOrdersFilters.chunked(columnCount).forEach { row ->
```

- [ ] **Step 4: Run filter tests to verify GREEN**

Run the focused instrumentation command from Step 2.

Expected: all `OrdersFeatureScreenTest` tests pass, including the existing typed `OrdersAction.FilterChanged` assertion.

- [ ] **Step 5: Commit the filter slice**

```powershell
git add app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersContract.kt app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersFeatureScreenTest.kt
git commit -m "feat: simplify mobile order filters"
```

### Task 2: Add The Contextual Header Scanner Action

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowComponents.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- Modify: `app/src/test/java/pl/dlaflow/mobile/MobilePackageScannerTest.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Write the failing action-policy unit test**

Add:

```kotlin
@Test
fun packageScannerHeaderActionIsLimitedToVisibleOrdersContext() {
    assertTrue(shouldShowPackageScannerHeaderAction(MobileAssistantTab.ORDERS, MobileAssistantOverlayScreen.NONE))
    assertFalse(shouldShowPackageScannerHeaderAction(MobileAssistantTab.DASHBOARD, MobileAssistantOverlayScreen.NONE))
    assertFalse(shouldShowPackageScannerHeaderAction(MobileAssistantTab.ORDERS, MobileAssistantOverlayScreen.NOTIFICATIONS))
}
```

- [ ] **Step 2: Run the focused unit test to verify RED**

Run:

```powershell
./gradlew :app:testDebugUnitTest --tests pl.dlaflow.mobile.MobilePackageScannerTest --no-daemon
```

Expected: compilation fails because `shouldShowPackageScannerHeaderAction` does not exist.

- [ ] **Step 3: Add a shared DlaFlow header action primitive**

Add to `DlaFlowComponents.kt`:

```kotlin
@Composable
internal fun DlaFlowHeaderIconButton(
    colors: DlaFlowComposeColors,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(DlaFlowDimensions.minimumTouchTarget)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.text,
            modifier = Modifier.size(25.dp),
        )
    }
}
```

Use existing imports for `Box`, `CircleShape`, `Role`, `clickable`, and `Icon`; do not add a page-local color or dimension.

- [ ] **Step 4: Implement contextual composition and immediate dispatch**

Add a pure policy helper in `MobileAssistantScreen.kt`:

```kotlin
internal fun shouldShowPackageScannerHeaderAction(
    selectedTab: MobileAssistantTab,
    overlayScreen: MobileAssistantOverlayScreen,
): Boolean = selectedTab == MobileAssistantTab.ORDERS && overlayScreen == MobileAssistantOverlayScreen.NONE
```

Extend `AppHeader` with `onScanPackage: (() -> Unit)? = null` and render this before the notification bell:

```kotlin
onScanPackage?.let { scan ->
    DlaFlowHeaderIconButton(
        colors = colors,
        icon = Icons.Rounded.QrCodeScanner,
        contentDescription = stringResource(R.string.orders_scan_package),
        onClick = scan,
    )
}
```

At the `AssistantContent` call site pass the existing action directly:

```kotlin
onScanPackage = if (shouldShowPackageScannerHeaderAction(selectedTab, mobileOverlayScreen)) {
    { onDashboardAction(DashboardAction.ScanPackage) }
} else {
    null
},
```

Add the resource:

```xml
<string name="orders_scan_package">Skanuj paczkę</string>
```

- [ ] **Step 5: Run the unit test to verify GREEN**

Run the focused unit test command from Step 2.

Expected: all `MobilePackageScannerTest` tests pass.

- [ ] **Step 6: Commit the header action slice**

```powershell
git add app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowComponents.kt app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt app/src/main/res/values/strings.xml app/src/test/java/pl/dlaflow/mobile/MobilePackageScannerTest.kt
git commit -m "feat: add orders scanner header action"
```

### Task 3: Replace The Scanner Card With A Compact Result Strip

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersPackageScannerStrip.kt`
- Create: `app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersPackageScannerStripTest.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Write failing Compose tests for the state contract**

Create `OrdersPackageScannerStripTest.kt` with tests using `createComposeRule()` and `DlaFlowTheme`:

```kotlin
@Test
fun emptyStateRendersNoScannerSurface() {
    setStrip(MobilePackageScanUiState.Empty)
    composeRule.onNodeWithTag("orders_package_scanner_strip").assertDoesNotExist()
}

@Test
fun matchedStateOpensNormalizedOrder() {
    val opened = mutableListOf<String>()
    setStrip(matchedState(orderNumber = "ORD-1001"), onOpenOrder = opened::add)

    composeRule.onNodeWithText("#ORD-1001 · Nowe").assertIsDisplayed()
    composeRule.onNodeWithText("Otwórz zamówienie").performClick()
    composeRule.runOnIdle { assertEquals(listOf("ORD-1001"), opened) }
}

@Test
fun failedStateOffersAnotherImmediateScan() {
    var retries = 0
    setStrip(
        MobilePackageScanUiState.Failed("ignored", "Sprawdź internet i spróbuj ponownie."),
        onScanAgain = { retries += 1 },
    )

    composeRule.onNodeWithText("Nie udało się sprawdzić paczki").assertIsDisplayed()
    composeRule.onNodeWithText("Skanuj ponownie").performClick()
    composeRule.runOnIdle { assertEquals(1, retries) }
}
```

Add these focused cases to the same test class:

```kotlin
@Test
fun loadingStateDoesNotExposeRawScanCode() {
    setStrip(MobilePackageScanUiState.Loading("PRIVATE-CODE"))

    composeRule.onNodeWithText("Sprawdzam paczkę").assertIsDisplayed()
    composeRule.onNodeWithText("PRIVATE-CODE").assertDoesNotExist()
    composeRule.onNodeWithText("Skanuj ponownie").assertDoesNotExist()
}

@Test
fun ambiguousMatchKeepsWarningAndOpenAction() {
    setStrip(matchedState(orderNumber = "ORD-1001", ambiguous = true))

    composeRule.onNodeWithText("Znaleziono kilka możliwych paczek").assertIsDisplayed()
    composeRule.onNodeWithText("Otwórz zamówienie").assertHeightIsAtLeast(48.dp)
}

@Test
fun unmatchedResultOffersRetry() {
    setStrip(unmatchedState(), onScanAgain = retryActions::add)

    composeRule.onNodeWithText("Nie znaleziono paczki").assertIsDisplayed()
    composeRule.onNodeWithText("Skanuj ponownie").performClick()
    composeRule.runOnIdle { assertEquals(listOf(Unit), retryActions) }
}
```

The test fixture must provide concrete `matchedState` and `unmatchedState` builders using `MobilePackageScanLookupResult`, plus a `setStrip` helper that composes the strip inside a tagged full-width viewport. Compose the same matched fixture once with `DlaFlowTheme(dark = false)` and once with `DlaFlowTheme(dark = true)`. In the 360 dp, `fontScale = 1.3f` fixture assert the strip bounds remain inside the viewport and the action is below the copy rather than beyond the right edge.

- [ ] **Step 2: Run the scanner-strip instrumentation test to verify RED**

Run:

```powershell
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=pl.dlaflow.mobile.feature.orders.OrdersPackageScannerStripTest --no-daemon
```

Expected: compilation fails because `OrdersPackageScannerStrip` does not exist.

- [ ] **Step 3: Add scanner strings**

Add to `strings.xml`:

```xml
<string name="orders_scanner_checking">Sprawdzam paczkę</string>
<string name="orders_scanner_failed">Nie udało się sprawdzić paczki</string>
<string name="orders_scanner_open_order">Otwórz zamówienie</string>
<string name="orders_scanner_retry">Skanuj ponownie</string>
<string name="orders_scanner_order_status">#%1$s · %2$s</string>
```

- [ ] **Step 4: Implement the feature-owned strip**

Create `OrdersPackageScannerStrip.kt` in `pl.dlaflow.mobile.feature.orders`. The public composition contract is:

```kotlin
private data class ScannerStripPresentation(
    val title: String,
    val supportingText: String,
    val orderStatus: String? = null,
    val orderNumber: String? = null,
    val loading: Boolean = false,
    val retryable: Boolean = false,
)

@Composable
internal fun OrdersPackageScannerStrip(
    colors: DlaFlowComposeColors,
    scanState: MobilePackageScanUiState,
    onOpenOrder: (String) -> Unit,
    onScanAgain: () -> Unit,
) {
    if (scanState is MobilePackageScanUiState.Empty) return

    val presentation = scannerStripPresentation(scanState)
    DlaFlowCard(colors, accent = !presentation.loading) {
        BoxWithConstraints(Modifier.testTag("orders_package_scanner_strip")) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.2f
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScannerStripCopy(colors, presentation, Modifier.fillMaxWidth())
                    ScannerStripAction(colors, presentation, Modifier.fillMaxWidth(), onOpenOrder, onScanAgain)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ScannerStripCopy(colors, presentation, Modifier.weight(1f))
                    ScannerStripAction(colors, presentation, Modifier, onOpenOrder, onScanAgain)
                }
            }
        }
    }
}

@Composable
private fun scannerStripPresentation(scanState: MobilePackageScanUiState): ScannerStripPresentation = when (scanState) {
    MobilePackageScanUiState.Empty -> error("Empty is handled before presentation")
    is MobilePackageScanUiState.Loading -> ScannerStripPresentation(
        title = stringResource(R.string.orders_scanner_checking),
        supportingText = stringResource(R.string.orders_scanner_checking_description),
        loading = true,
    )
    is MobilePackageScanUiState.Failed -> ScannerStripPresentation(
        title = stringResource(R.string.orders_scanner_failed),
        supportingText = scanState.message,
        retryable = true,
    )
    is MobilePackageScanUiState.Resolved -> {
        val copy = packageScannerResolvedCopy(scanState.result)
        val order = scanState.result.order.takeIf { scanState.result.matched }
        ScannerStripPresentation(
            title = copy.title,
            supportingText = copy.supportingText,
            orderStatus = order?.let {
                stringResource(R.string.orders_scanner_order_status, it.orderNumber, it.status)
            },
            orderNumber = order?.orderNumber,
            retryable = order == null,
        )
    }
}

@Composable
private fun ScannerStripCopy(
    colors: DlaFlowComposeColors,
    presentation: ScannerStripPresentation,
    modifier: Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (presentation.loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = colors.primary,
                strokeWidth = 2.dp,
            )
        } else {
            DlaFlowIcon(Icons.Rounded.QrCodeScanner, colors.primary, Modifier.size(22.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = presentation.title,
                color = colors.textStrong,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
            )
            Text(
                text = presentation.supportingText,
                color = colors.textMuted,
                fontSize = 12.sp,
                letterSpacing = 0.sp,
            )
            presentation.orderStatus?.let {
                Text(text = it, color = colors.textMuted, fontSize = 12.sp, letterSpacing = 0.sp)
            }
        }
    }
}

@Composable
private fun ScannerStripAction(
    colors: DlaFlowComposeColors,
    presentation: ScannerStripPresentation,
    modifier: Modifier,
    onOpenOrder: (String) -> Unit,
    onScanAgain: () -> Unit,
) {
    when {
        presentation.orderNumber != null -> DlaFlowSecondaryButton(
            colors = colors,
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            text = stringResource(R.string.orders_scanner_open_order),
            modifier = modifier,
            onClick = { onOpenOrder(presentation.orderNumber) },
        )
        presentation.retryable -> DlaFlowSecondaryButton(
            colors = colors,
            icon = Icons.Rounded.QrCodeScanner,
            text = stringResource(R.string.orders_scanner_retry),
            modifier = modifier,
            onClick = onScanAgain,
        )
    }
}
```

Add `<string name="orders_scanner_checking_description">Szukam pasującego zamówienia.</string>` with the other scanner resources. Import only the Compose, DlaFlow, and existing mobile scan types used above. Do not log or display the raw `code` value.

- [ ] **Step 5: Replace the large card at the composition boundary**

Delete private `PackageScannerCard` from `MobileAssistantScreen.kt`, import `OrdersPackageScannerStrip`, and change Orders lead content to:

```kotlin
leadContent = {
    LegacyKpiGrid(colors, dashboard?.kpis)
    OrdersPackageScannerStrip(
        colors = colors,
        scanState = packageScanState,
        onOpenOrder = { onOrdersAction(OrdersAction.OpenOrder(it)) },
        onScanAgain = { onDashboardAction(DashboardAction.ScanPackage) },
    )
},
```

`Empty` contributes no node and no spacing of its own. The KPI grid therefore becomes the first lead element and the strip, when present, remains immediately above the list.

- [ ] **Step 6: Run scanner-strip tests to verify GREEN**

Run the focused instrumentation command from Step 2.

Expected: all strip state, action, accessibility, and bounds tests pass.

- [ ] **Step 7: Commit the compact strip slice**

```powershell
git add app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersPackageScannerStrip.kt app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt app/src/main/res/values/strings.xml app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersPackageScannerStripTest.kt
git commit -m "feat: compact mobile package scanner results"
```

### Task 4: Full Verification, Local Main Merge, And Project Memory

**Files:**
- Modify after merge: `D:/ECOM/Maciek/PANEL DLAYOU/PROJECT_TODO.md`
- Do not modify: `D:/ECOM/Maciek/PANEL DLAYOU/VPS_TODO.md`

- [ ] **Step 1: Run the complete local quality gate**

```powershell
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

Expected: `BUILD SUCCESSFUL` with no failing unit tests or lint errors.

- [ ] **Step 2: Run managed phone instrumentation QA**

Run the repository's managed Pixel 6 QA command or script for `OrdersFeatureScreenTest` and `OrdersPackageScannerStripTest`.

Expected: all tests pass at the managed phone profile, including 360/412 dp and larger-font assertions.

- [ ] **Step 3: Run real managed tablet instrumentation where available**

Run the existing real Pixel Tablet target for the two Orders test classes.

Expected: all wide-layout tests pass. If the device is unavailable, record that limitation explicitly and do not claim tablet evidence.

- [ ] **Step 4: Perform visual and interaction smoke**

Verify on the QA emulator:

- light and dark themes,
- 360, 412, and 600+ dp behavior,
- larger font and TalkBack labels,
- no overflow or white dark-mode artifacts,
- only three visible filters,
- no empty scanner card,
- QR action immediately launches camera permission/camera flow,
- loading/result/error strip remains secondary,
- Back and camera denial/cancellation preserve the screen and session.

- [ ] **Step 5: Request code review and resolve findings**

Use `superpowers:requesting-code-review`, compare the diff to the approved spec, and rerun any affected focused tests after corrections.

- [ ] **Step 6: Merge the verified branch locally to mobile `main`**

Use `superpowers:finishing-a-development-branch`. Confirm the integration checkout contains only the known untracked emulator files before merging. Merge the exact verified `codex/*` branch without push, tag, release, or publication.

- [ ] **Step 7: Install the Operator APK without clearing app data**

From mobile `main`, run only the preserving installer:

```powershell
./scripts/install-operator-apk.ps1
```

Expected: the Operator package updates through `adb install -r`; pairing/session data remains present. Do not use `adb uninstall`, `pm clear`, or a QA package over the Operator package.

- [ ] **Step 8: Update canonical project memory**

Append a dated, verified entry to `D:/ECOM/Maciek/PANEL DLAYOU/PROJECT_TODO.md` containing:

- variant C implemented on mobile Orders,
- visible filters are `Wszystkie`, `Nowe`, `Do wysyłki`,
- scanner is a contextual header action with an empty-state-free compact result strip,
- exact commits and verification evidence,
- Operator installation result and confirmation that app data was preserved.

Do not modify `VPS_TODO.md`, because this task does not release or deploy to VPS.

- [ ] **Step 9: Run mobile graph refresh only if the repository exposes it**

The Android repository currently has no panel Graphify ownership. Do not copy panel scripts into mobile. If a repository-local graph refresh command exists at execution time, run it; otherwise record that Graphify does not apply to this Android-only code change.
