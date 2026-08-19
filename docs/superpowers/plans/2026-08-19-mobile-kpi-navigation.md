# Mobile KPI Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the KPI cards navigate to their real operator destinations: Messages to the Messages tab and Overdue to a canonical overdue-shipping orders filter.

**Architecture:** Keep the shared app-level KPI destination type, but let the host distinguish tab navigation from orders-filter navigation. Extend the existing panel mobile orders contract with `filter=overdue`; both its list predicate and dashboard counter use canonical `SalesOrder.shippingDeadlineAt` and exclude orders whose latest shipment has already reached a sent-or-later stage. Android only sends the filter value and renders API results.

**Tech Stack:** Kotlin, Jetpack Compose, existing DlaFlow design system, JUnit, Android Gradle Plugin.

---

### Task 1: Panel overdue filter contract

**Files:**
- Modify: `apps/api/src/modules/mobile/orders.test.ts`
- Modify: `apps/api/src/modules/mobile/orders.ts`
- Modify: `apps/api/src/modules/mobile/assistant-dashboard.test.ts`
- Modify: `apps/api/src/modules/mobile/assistant-dashboard.ts`

- [ ] Add failing API tests for `filter=overdue`: include a tenant-owned order with an expired deadline, exclude future/no-deadline orders, exclude sent-or-later shipments, and exclude another tenant.
- [ ] Run the focused mobile orders test and confirm RED because `overdue` is rejected.
- [ ] Add `overdue` to the existing query schema and implement the indexed, tenant-scoped Prisma predicate using `shippingDeadlineAt < now` plus the shared sent-or-later shipment semantics.
- [ ] Run the focused mobile orders test and confirm GREEN.
- [ ] Add a failing dashboard test proving `overdueOrProblems` contains only the canonical overdue-shipping count.
- [ ] Replace the legacy unpaid-plus-problems calculation with the same overdue predicate and confirm the dashboard test is GREEN.

### Task 2: Android destination and filter contract

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileOrdersState.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/app/navigation/MobileKpiDestination.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersContract.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersMapper.kt`
- Modify: `app/src/test/java/pl/dlaflow/mobile/app/navigation/MobileKpiDestinationTest.kt`
- Modify: `app/src/test/java/pl/dlaflow/mobile/feature/orders/OrdersMapperTest.kt`

- [ ] Add failing tests requiring `OVERDUE -> OrdersFilter.OVERDUE`, `OrdersFilter.OVERDUE -> MobileOrderFilter.OVERDUE`, and query value `overdue`.
- [ ] Run focused tests and confirm RED.
- [ ] Add `OVERDUE` at the existing model/mapper seams, label it `Po terminie`, expose it in Orders, and remove the orders-only `MESSAGES` filter from the visible list.
- [ ] Run focused tests and confirm GREEN.

### Task 3: Route Messages to its tab

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/dashboard/DashboardScreen.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt`
- Modify: `app/src/test/java/pl/dlaflow/mobile/feature/dashboard/DashboardSourceBoundaryTest.kt`

- [ ] Add failing tests/boundary assertions that `MESSAGES` is handled as tab navigation and is never converted to an orders filter.
- [ ] Run the focused test and confirm RED.
- [ ] Route `MESSAGES` to `MobileAssistantTab.MESSAGES`; route the remaining KPI destinations through `OrdersAction.FilterChanged`.
- [ ] Run focused tests and confirm GREEN.

### Task 4: Verification and emulator smoke

**Files:**
- Modify: `D:/ECOM/Maciek/PANEL DLAYOU/PROJECT_TODO.md`

- [ ] Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon`.
- [ ] Install with `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk`.
- [ ] Verify Dashboard and Orders `Wiadomości` cards select the Messages tab without clearing application data.
- [ ] Verify Dashboard and Orders `Po terminie` cards select the overdue orders filter.
- [ ] Record the verified local state in panel `PROJECT_TODO.md` and commit the Android implementation to `main`.
