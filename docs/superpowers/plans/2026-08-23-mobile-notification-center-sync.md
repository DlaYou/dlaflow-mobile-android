# Mobile Notification Center Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Keep the Android notification center fresh while the app is open and deliver every unread panel entry whose category is enabled, regardless of tone or action.

**Architecture:** Reuse the existing signed `/api/mobile/notifications` client and `NotificationsCoordinator`. `MobileDataRefreshController` receives a third refresh callback for notifications, while `NotificationsBackgroundCoordinator` delegates eligibility entirely to the existing preference-aware callback. No new endpoint, persistence model, or provider-specific logic is introduced.

**Tech Stack:** Kotlin, Android SDK, existing JVM tests, Gradle lint/debug build.

---

### Task 1: Foreground notification refresh

**Files:**
- Modify: `app/src/test/java/pl/dlaflow/mobile/MobileDataRefreshControllerTest.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileDataRefreshController.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt`

- [ ] Add a failing assertion that start and each scheduled tick invoke `refreshNotifications` once, while tab selection remains scoped to dashboard/orders.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests pl.dlaflow.mobile.MobileDataRefreshControllerTest --no-daemon` and confirm the new assertion fails because the controller has no notification callback.
- [ ] Add the callback to the controller constructor and invoke it from the shared visible-data refresh cycle.
- [ ] Pass `::refreshNotificationsIfIdle` from `MainActivity`; the helper refreshes the coordinator only when a paired session exists and the coordinator is idle.
- [ ] Re-run the focused test and confirm GREEN.

### Task 2: Deliver every enabled panel entry

**Files:**
- Modify: `app/src/test/java/pl/dlaflow/mobile/MobileNotificationPreferencesTest.kt`
- Modify: `app/src/test/java/pl/dlaflow/mobile/MobileNotificationsTest.kt`
- Modify: `app/src/test/java/pl/dlaflow/mobile/MobileFeatureIntegrationGuardTest.kt`
- Modify: `app/src/test/java/pl/dlaflow/mobile/feature/notifications/NotificationsBackgroundCoordinatorTest.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileNotificationPreferences.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/notifications/NotificationsBackgroundCoordinator.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/mobile_api.kt`

- [ ] Add RED coverage proving an enabled `IMPORTANT_PANEL` entry with `info`/`success` is accepted and a disabled category is still rejected.
- [ ] Remove the coordinator's legacy tone/action pre-filter so all unread entries reach the preference-aware callback.
- [ ] Make the preference-aware policy return true for every enabled category; retain the legacy overload as an always-allow compatibility helper so it cannot suppress panel entries.
- [ ] Update coordinator and guard tests to assert unread `success`/`info` entries are delivered once and read entries remain ignored.
- [ ] Run the focused notification tests and confirm GREEN.

### Task 3: Verification and handoff

**Files:**
- No additional production files.

- [ ] Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon`.
- [ ] Run `git diff --check` and inspect the final diff for scope, signed transport, session handling, and no secrets.
- [ ] Run `npm run graphify:code` and `npm run agent:handoff -- -Checks "..."` from the panel feature worktree only when the mobile commit is ready for the general agent to integrate.
