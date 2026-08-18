# Mobile Order Card Status And Timing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the cramped order-card pills and timing sentence with two API-driven C2 status fields, a vertical mini timeline, and a delivery footer without the phone number.

**Architecture:** The panel normalizer and `/api/mobile/orders` remain the source of status labels and tones. Android passes canonical labels through unchanged, converts only the finite tone vocabulary into a shared `DlaFlowStatusTone`, and renders both values with a new generic design-system primitive; the Orders feature continues to own order-specific labels and timing.

**Tech Stack:** Kotlin 2.2.21, Jetpack Compose, AndroidX Compose UI tests, JUnit 4, existing DlaFlow design system and signed mobile transport.

---

## File Structure

- Create `app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowStatusField.kt`: generic labeled-status primitive, semantic tone enum, color resolution, wrapping, and TalkBack semantics.
- Create `app/src/test/java/pl/dlaflow/mobile/feature/orders/OrdersStatusPresentationTest.kt`: pure status pass-through and tone fallback contract.
- Modify `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt`: C2 status layout, API status pass-through, vertical mini timeline, and phone-free footer.
- Modify `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrderDetailPanel.kt`: remove the remaining local business-status translation while retaining the existing detail layout.
- Modify `app/src/main/res/values/strings.xml`: business labels and neutral fallback text.
- Modify `app/src/test/java/pl/dlaflow/mobile/feature/orders/OrdersMapperTest.kt`: prove status labels and tones pass through the DTO mapper unchanged.
- Modify `app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersFeatureScreenTest.kt`: verify C2 semantics, responsive layout, timing, phone removal, and card navigation.
- Modify `app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersVisualSnapshotTest.kt`: use canonical API status examples in light/dark snapshots.
- Modify `D:/ECOM/Maciek/PANEL DLAYOU/PROJECT_TODO.md` only after all verification passes: record the verified mobile UI standard and API ownership boundary.

### Task 1: Lock The API Status Boundary With Unit Tests

**Files:**
- Create: `app/src/test/java/pl/dlaflow/mobile/feature/orders/OrdersStatusPresentationTest.kt`
- Modify: `app/src/test/java/pl/dlaflow/mobile/feature/orders/OrdersMapperTest.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt`

- [ ] **Step 1: Write failing tests for verbatim labels and safe tones**

Create `OrdersStatusPresentationTest.kt`:

```kotlin
package pl.dlaflow.mobile.feature.orders

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.dlaflow.mobile.core.designsystem.DlaFlowStatusTone

class OrdersStatusPresentationTest {
    @Test
    fun `canonical status label from API is preserved`() {
        assertEquals("Gotowe do odbioru", ordersStatusValue("  Gotowe do odbioru  ", "Do sprawdzenia"))
    }

    @Test
    fun `blank status uses neutral business fallback`() {
        assertEquals("Do sprawdzenia", ordersStatusValue("   ", "Do sprawdzenia"))
    }

    @Test
    fun `API tones map to shared tones and unknown is neutral`() {
        assertEquals(DlaFlowStatusTone.INFO, ordersStatusTone("info"))
        assertEquals(DlaFlowStatusTone.SUCCESS, ordersStatusTone("SUCCESS"))
        assertEquals(DlaFlowStatusTone.WARNING, ordersStatusTone("warning"))
        assertEquals(DlaFlowStatusTone.DANGER, ordersStatusTone("danger"))
        assertEquals(DlaFlowStatusTone.NEUTRAL, ordersStatusTone("provider-purple"))
    }
}
```

Add a mapper test using the existing fixture:

```kotlin
@Test
fun `list mapper preserves normalized API statuses and tones`() {
    val row = orderListDto().copy(
        status = "Gotowe do odbioru",
        statusTone = "warning",
        paymentStatus = "Płatność przy odbiorze",
        paymentTone = "info",
    )

    val mapped = MobileOrdersPage(
        data = listOf(row),
        count = 1,
        limit = 20,
        nextOffset = null,
        offset = 0,
        total = 1,
    ).toOrdersListContent().items.single()

    assertEquals("Gotowe do odbioru", mapped.status)
    assertEquals("warning", mapped.statusTone)
    assertEquals("Płatność przy odbiorze", mapped.paymentStatus)
    assertEquals("info", mapped.paymentTone)
}
```

- [ ] **Step 2: Run the focused unit tests and verify RED**

Run:

```powershell
./gradlew :app:testDebugUnitTest --tests "pl.dlaflow.mobile.feature.orders.OrdersStatusPresentationTest" --tests "pl.dlaflow.mobile.feature.orders.OrdersMapperTest" --no-daemon
```

Expected: FAIL because `DlaFlowStatusTone`, `ordersStatusValue`, and `ordersStatusTone` do not exist.

- [ ] **Step 3: Add minimal pure presentation helpers**

In `OrdersScreen.kt`, add:

```kotlin
internal fun ordersStatusValue(value: String, fallback: String): String =
    value.trim().ifBlank { fallback }

internal fun ordersStatusTone(value: String): DlaFlowStatusTone = when (value.trim().lowercase(Locale.ROOT)) {
    "brand" -> DlaFlowStatusTone.BRAND
    "info" -> DlaFlowStatusTone.INFO
    "success" -> DlaFlowStatusTone.SUCCESS
    "warning" -> DlaFlowStatusTone.WARNING
    "danger" -> DlaFlowStatusTone.DANGER
    else -> DlaFlowStatusTone.NEUTRAL
}
```

Create the enum initially in `DlaFlowStatusField.kt`:

```kotlin
package pl.dlaflow.mobile.core.designsystem

internal enum class DlaFlowStatusTone {
    NEUTRAL,
    BRAND,
    INFO,
    SUCCESS,
    WARNING,
    DANGER,
}
```

- [ ] **Step 4: Run the focused unit tests and verify GREEN**

Run the Step 2 command again.

Expected: PASS for `OrdersStatusPresentationTest` and `OrdersMapperTest`.

- [ ] **Step 5: Commit the status boundary**

```powershell
git add app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowStatusField.kt app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt app/src/test/java/pl/dlaflow/mobile/feature/orders/OrdersStatusPresentationTest.kt app/src/test/java/pl/dlaflow/mobile/feature/orders/OrdersMapperTest.kt
git commit -m "test: lock mobile order status boundary"
```

### Task 2: Add The Shared DlaFlow Labeled-Status Primitive

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowStatusField.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersFeatureScreenTest.kt`

- [ ] **Step 1: Add failing C2 semantics and responsive-layout tests**

Add tests to `OrdersFeatureScreenTest.kt` that query the merged TalkBack descriptions:

```kotlin
@Test
fun orderCardExposesBothApiDrivenStatusesToTalkBack() {
    setOrders(contentState(), mutableListOf(), screenWidthDp = 412)

    composeRule.onNodeWithContentDescription("Realizacja, Nowe").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Płatność, Opłacone").assertIsDisplayed()
}

@Test
fun statusFieldsStackAt360Dp() {
    setOrders(contentState(), mutableListOf(), screenWidthDp = 360)

    val fulfillmentTop = composeRule.onNodeWithContentDescription("Realizacja, Nowe")
        .fetchSemanticsNode().boundsInRoot.top
    val paymentTop = composeRule.onNodeWithContentDescription("Płatność, Opłacone")
        .fetchSemanticsNode().boundsInRoot.top

    assertTrue(paymentTop > fulfillmentTop)
}

@Test
fun statusFieldsStaySideBySideAt412Dp() {
    setOrders(contentState(), mutableListOf(), screenWidthDp = 412)

    val fulfillmentTop = composeRule.onNodeWithContentDescription("Realizacja, Nowe")
        .fetchSemanticsNode().boundsInRoot.top
    val paymentTop = composeRule.onNodeWithContentDescription("Płatność, Opłacone")
        .fetchSemanticsNode().boundsInRoot.top

    assertEquals(fulfillmentTop, paymentTop, 0f)
}
```

Change the synthetic status in `ordersContent` from `new` to `Nowe`. Import `onNodeWithContentDescription`.

- [ ] **Step 2: Run the managed-device class and verify RED**

Run:

```powershell
./gradlew :app:dlaflowQaApi35DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=pl.dlaflow.mobile.feature.orders.OrdersFeatureScreenTest --no-daemon
```

Expected: FAIL because the C2 status semantics and layout are not rendered yet.

- [ ] **Step 3: Implement the reusable design-system field**

Complete `DlaFlowStatusField.kt` with a generic component that:

```kotlin
@Composable
internal fun DlaFlowStatusField(
    colors: DlaFlowComposeColors,
    label: String,
    value: String,
    tone: DlaFlowStatusTone,
    modifier: Modifier = Modifier,
) {
    val toneColor = tone.color(colors)
    val shape = RoundedCornerShape(DlaFlowDimensions.controlRadius)
    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(shape)
            .background(colors.surfaceSubtle)
            .border(DlaFlowDimensions.borderWidth, toneColor.copy(alpha = 0.24f), shape)
            .semantics(mergeDescendants = true) { contentDescription = "$label, $value" }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = colors.textMuted, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            color = toneColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
```

Add `DlaFlowStatusTone.color(colors)` in the same file, mapping `NEUTRAL` to `textMuted`, `BRAND` to `primary`, `INFO` to `info`, `SUCCESS` to `success`, `WARNING` to `orange`, and `DANGER` to `danger`. Every `Text` must explicitly use `DlaFlowInter` and `letterSpacing = 0.sp`.

- [ ] **Step 4: Render C2 fields from API values**

Add the C2-owned business labels to `strings.xml`:

```xml
<string name="orders_status_fulfillment_label">Realizacja</string>
<string name="orders_status_payment_label">Płatność</string>
<string name="orders_status_check">Do sprawdzenia</string>
```

In `OrdersScreen.kt`, replace `OrdersTinyPill` with `OrderStatusFields`:

```kotlin
@Composable
private fun OrderStatusFields(colors: DlaFlowComposeColors, order: OrdersListItem) {
    val fallback = stringResource(R.string.orders_status_check)
    val fulfillment = ordersStatusValue(order.status, fallback)
    val payment = ordersStatusValue(order.paymentStatus, fallback)
    if (ordersUsesStackedStatusLayout()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DlaFlowStatusField(
                colors = colors,
                label = stringResource(R.string.orders_status_fulfillment_label),
                value = fulfillment,
                tone = ordersStatusTone(order.statusTone),
                modifier = Modifier.fillMaxWidth(),
            )
            DlaFlowStatusField(
                colors = colors,
                label = stringResource(R.string.orders_status_payment_label),
                value = payment,
                tone = ordersStatusTone(order.paymentTone),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DlaFlowStatusField(
                colors = colors,
                label = stringResource(R.string.orders_status_fulfillment_label),
                value = fulfillment,
                tone = ordersStatusTone(order.statusTone),
                modifier = Modifier.weight(1f),
            )
            DlaFlowStatusField(
                colors = colors,
                label = stringResource(R.string.orders_status_payment_label),
                value = payment,
                tone = ordersStatusTone(order.paymentTone),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ordersUsesStackedStatusLayout(): Boolean {
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    return configuration.screenWidthDp <= 360 || (configuration.screenWidthDp < 480 && fontScale >= 1.2f)
}
```

Remove `OrdersTinyPill` after replacement.

- [ ] **Step 5: Run the managed-device test and verify GREEN**

Run the Step 2 command again.

Expected: PASS for both semantics and layout tests.

- [ ] **Step 6: Commit the shared status presentation**

```powershell
git add app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowStatusField.kt app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt app/src/main/res/values/strings.xml app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersFeatureScreenTest.kt
git commit -m "feat: add mobile order status fields"
```

### Task 3: Build The Vertical Mini Timeline And Phone-Free Footer

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersFeatureScreenTest.kt`

- [ ] **Step 1: Add failing timeline and footer tests**

Add to `OrdersFeatureScreenTest.kt`:

```kotlin
@Test
fun orderTimingIsVerticalAtNormalWidth() {
    setOrders(contentState(), mutableListOf(), screenWidthDp = 412)

    val orderedTop = composeRule.onNodeWithText("Zamówiono", useUnmergedTree = true)
        .fetchSemanticsNode().boundsInRoot.top
    val shippingTop = composeRule.onNodeWithText("Wyślij do", useUnmergedTree = true)
        .fetchSemanticsNode().boundsInRoot.top

    assertTrue(shippingTop > orderedTop)
}

@Test
fun orderCardKeepsDeliveryMethodButHidesPhoneNumber() {
    setOrders(
        OrdersUiState(listState = DlaFlowUiState.Content(ordersContent(phone = "+48 100 200 300"))),
        mutableListOf(),
    )

    composeRule.onNodeWithText("Paczkomat").assertIsDisplayed()
    composeRule.onNodeWithText("+48 100 200 300", substring = true).assertDoesNotExist()
}
```

Extend the `ordersContent` fixture with `phone: String = ""` and pass it to `OrdersListItem`.

- [ ] **Step 2: Run the managed-device class and verify RED**

Run the Task 2 Step 2 command.

Expected: FAIL because normal-width timing is horizontal and the phone remains in quick information.

- [ ] **Step 3: Split timing labels from values and add timeline rows**

Add string resources:

```xml
<string name="orders_timeline_ordered_label">Zamówiono</string>
<string name="orders_timeline_shipping_label">Wyślij do</string>
<string name="orders_deadline_unavailable">Termin niedostępny</string>
```

Replace the horizontal timing branch with an always-vertical `OrderTimelineEntry` pair. Each entry has a fixed leading marker column and a text column containing a muted label and a wrapping value. The first marker column draws the connector to the second row; the second has no connector. Use existing DlaFlow colors, `DlaFlowInter`, `letterSpacing = 0.sp`, `maxLines = 2`, and no one-line ellipsis for the combined label/value.

The call site becomes:

```kotlin
OrderTimelineEntry(
    colors = colors,
    label = stringResource(R.string.orders_timeline_ordered_label),
    value = orderedAt.ifBlank { stringResource(R.string.orders_value_missing) },
    tone = colors.textMuted,
    showConnector = true,
)
OrderTimelineEntry(
    colors = colors,
    label = stringResource(R.string.orders_timeline_shipping_label),
    value = deadlineAt?.let { ordersShippingDeadlineLabel(it) }
        ?: stringResource(R.string.orders_deadline_unavailable),
    tone = ordersShippingDeadlineColor(colors, deadlineAt.orEmpty()),
    showConnector = false,
)
```

The complete entry helper is:

```kotlin
@Composable
private fun OrderTimelineEntry(
    colors: DlaFlowComposeColors,
    label: String,
    value: String,
    tone: Color,
    showConnector: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.width(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 5.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(tone),
            )
            if (showConnector) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(colors.border),
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (showConnector) 5.dp else 0.dp),
        ) {
            Text(
                label,
                color = colors.textMuted,
                fontFamily = DlaFlowInter,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            )
            Text(
                value,
                color = tone,
                fontFamily = DlaFlowInter,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
```

Import `CircleShape` and `DlaFlowInter`; remove `ordersUsesCompactLayout` and `OrderTimingValue`.

- [ ] **Step 4: Remove phone composition from list quick information**

Change `ordersQuickInfo` so it includes only `shippingMethod`, with product count as the fallback:

```kotlin
@Composable
private fun ordersQuickInfo(order: OrdersListItem): String =
    order.shippingMethod.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.orders_value_products_short, order.itemCount.coerceAtLeast(1))
```

Do not remove `phone` from DTOs or detail models.

- [ ] **Step 5: Run the managed-device class and verify GREEN**

Run the Task 2 Step 2 command.

Expected: PASS, including vertical timing and no phone text on the list card.

- [ ] **Step 6: Commit timing and footer changes**

```powershell
git add app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt app/src/main/res/values/strings.xml app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersFeatureScreenTest.kt
git commit -m "feat: refine mobile order card timing"
```

### Task 4: Remove Android Business-Status Translation And Refresh Visual Fixtures

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrderDetailPanel.kt`
- Modify: `app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersVisualSnapshotTest.kt`

- [ ] **Step 1: Add a failing future-status UI assertion**

Extend `ordersContent` with `status: String = "Nowe"` and add:

```kotlin
@Test
fun futureCanonicalApiStatusIsDisplayedWithoutAndroidTranslation() {
    setOrders(
        OrdersUiState(listState = DlaFlowUiState.Content(ordersContent(status = "Gotowe do odbioru"))),
        mutableListOf(),
    )

    composeRule.onNodeWithContentDescription("Realizacja, Gotowe do odbioru").assertIsDisplayed()
}
```

- [ ] **Step 2: Run the managed-device class and verify the new contract**

Run the Task 2 Step 2 command.

Expected: PASS only when the list consumes the API label unchanged; otherwise FAIL and remove the local translation in Step 3.

- [ ] **Step 3: Remove the local raw-status vocabulary**

Delete `ordersStatusLabel`. Use `ordersStatusValue(order.status, stringResource(R.string.orders_status_check))` in both the list and `OrderDetailPanel`. Keep filter labels unchanged because filters are Android-owned UI choices, not integration states.

Update the snapshot fixtures from raw English values to canonical API examples:

```kotlin
status = when (tone) {
    "warning" -> "Do wysyłki"
    "success" -> "Wysłane"
    else -> "W realizacji"
}
```

Keep the synthetic phone in the fixture so visual and UI tests prove it is not rendered on the list card.

- [ ] **Step 4: Run unit and managed-device order tests**

Run:

```powershell
./gradlew :app:testDebugUnitTest :app:dlaflowQaApi35DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=pl.dlaflow.mobile.feature.orders --no-daemon
```

Expected: PASS for unit tests and all Orders instrumentation tests.

- [ ] **Step 5: Commit the API ownership cleanup**

```powershell
git add app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt app/src/main/java/pl/dlaflow/mobile/feature/orders/OrderDetailPanel.kt app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersFeatureScreenTest.kt app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersVisualSnapshotTest.kt
git commit -m "refactor: trust normalized mobile order statuses"
```

### Task 5: Full Verification, Visual Review, Memory, And Integration

**Files:**
- Modify after verification: `D:/ECOM/Maciek/PANEL DLAYOU/PROJECT_TODO.md`

- [ ] **Step 1: Run the required Android quality gate**

```powershell
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

Expected: BUILD SUCCESSFUL with unit tests, lint, and debug APK complete.

- [ ] **Step 2: Run QA emulator coverage and generate snapshots**

```powershell
./scripts/run-qa-emulator-tests.ps1
```

Expected: the managed `dlaflowQaApi35` suite passes; the paired Operator emulator remains untouched.

- [ ] **Step 3: Inspect visual evidence**

Pull and inspect the generated `orders-list-light` and `orders-list-dark` PNG files. Verify C2 at 360, 412, and 600 dp where available, larger font, no horizontal overflow, no clipped status labels, no phone text, readable timing, and no white surfaces in dark mode. If a required width is not covered by the managed-device configuration, run the focused Compose class with the matching injected `LocalConfiguration` assertions and report that screenshot coverage was unavailable for that width.

- [ ] **Step 4: Verify repository cleanliness and exact diff**

```powershell
git diff --check
git status --short --branch
git log --oneline --decorate -6
```

Expected: no whitespace errors and only task-owned committed changes.

- [ ] **Step 5: Update canonical project memory after evidence exists**

Append this verified decision to the current `PROJECT_TODO.md` through a separate panel worktree, preserving unrelated panel changes:

```markdown
- 2026-08-18: Android order cards use the shared C2 status standard: separate `Realizacja` and `Płatność` fields consume canonical labels/tones from `/api/mobile/orders`, Android performs no provider-status normalization, timing is a vertical `Zamówiono`/`Wyślij do` line, and phone is omitted from list cards. Verified with Android unit tests, lint, debug assembly, managed QA Compose tests, and light/dark synthetic snapshots; the Operator emulator remains isolated.
```

Do not modify `VPS_TODO.md` because this plan performs no release, deployment, or production smoke.

- [ ] **Step 6: Commit the final verified state and merge locally to mobile main**

```powershell
git status --short --branch
git log --oneline main..HEAD
```

After reviewing the exact commits from the clean feature worktree, merge `codex/mobile-order-card-c2` into the local mobile `main` with a non-fast-forward merge. Do not push, tag, publish an APK, or alter the unrelated `.manus-emulator*` files.
