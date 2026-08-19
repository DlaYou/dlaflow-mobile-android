# Mobile KPI Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the existing four KPI cards navigate to the matching filtered orders list from both Dashboard and Orders.

**Architecture:** Define a small app-level KPI destination type shared by the two presentation sites. The host maps each destination to the existing `OrdersFilter` and `OrdersCoordinator`; the shared design-system tile receives an optional accessible click action.

**Tech Stack:** Kotlin, Jetpack Compose, existing DlaFlow design system, JUnit, Android Gradle Plugin.

---

### Task 1: KPI destination contract

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/app/navigation/MobileKpiDestination.kt`
- Create: `app/src/test/java/pl/dlaflow/mobile/app/navigation/MobileKpiDestinationTest.kt`

- [ ] Write a failing test asserting `NEW_ORDERS`, `TO_SHIP`, `OVERDUE` and `MESSAGES` map to `NEW`, `TO_SHIP`, `PROBLEMS` and `MESSAGES`.
- [ ] Run the focused unit test and confirm RED.
- [ ] Implement the destination enum and mapping function.
- [ ] Run the focused unit test and confirm GREEN.

### Task 2: Shared clickable KPI tile

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowComponents.kt`
- Modify: `app/src/test/java/pl/dlaflow/mobile/feature/dashboard/DashboardSourceBoundaryTest.kt`

- [ ] Add a failing source-boundary test requiring the shared KPI tile to expose an optional `onClick` and button semantics.
- [ ] Run the focused test and confirm RED.
- [ ] Add optional click handling while preserving the current non-clickable use and visual styling.
- [ ] Run the focused test and confirm GREEN.

### Task 3: Wire both KPI grids to orders

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardScreen.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersContract.kt`
- Modify: `app/src/test/java/pl/dlaflow/mobile/feature/dashboard/DashboardSourceBoundaryTest.kt`

- [ ] Add failing boundary assertions that Dashboard and Orders pass all four destination callbacks.
- [ ] Run the focused test and confirm RED.
- [ ] Route a KPI click through the host to the existing filtered orders list and expose all four filters in Orders.
- [ ] Run focused tests and confirm GREEN.

### Task 4: Verification and emulator smoke

**Files:**
- Modify: `D:/ECOM/Maciek/PANEL DLAYOU/PROJECT_TODO.md`

- [ ] Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon`.
- [ ] Install with `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk`.
- [ ] Verify at least one Dashboard card and one Orders card select the expected filter without clearing application data.
- [ ] Record the verified local state in panel `PROJECT_TODO.md` and commit the Android implementation to `main`.
