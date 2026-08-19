# Mobile Feature Boundaries Integration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate reusable scanner, product, notification, and settings boundaries into current mobile `main` while preserving all newer operational behavior.

**Architecture:** Port narrow domain layers from historical branches into current code instead of merging their stale hosts. The current signed `MobileApiClient`, host-owned Android effects, compact Orders scanner, order DTOs, Firebase registration, and notification preferences stay canonical.

**Tech Stack:** Kotlin, Android SDK, Jetpack Compose, JUnit, Gradle Managed Devices, existing DlaFlow design system and signed HTTP transport.

---

### Task 1: Preserve current-main contracts with integration guard tests

**Files:**
- Create: `app/src/test/java/pl/dlaflow/mobile/MobileFeatureIntegrationGuardTest.kt`
- Verify: `app/src/main/java/pl/dlaflow/mobile/mobile_api.kt`
- Verify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersPackageScannerStrip.kt`
- Verify: `app/src/main/java/pl/dlaflow/mobile/MobileNotificationPreferences.kt`

- [ ] Add source and behavioral assertions proving order DTO deadline/color fields, push installation registration, notification preferences, and compact scanner presentation remain present.
- [ ] Run the focused test and confirm it passes on the untouched current baseline.
- [ ] Commit the guard before porting historical code.

### Task 2: Port scanner domain behind the current compact Orders UI

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/scanner/ScannerContract.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/scanner/ScannerFailure.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/scanner/ScannerMapper.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/scanner/ScannerGateway.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/scanner/ScannerStateHolder.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/scanner/ScannerCoordinator.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/feature/scanner/*Test.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/MobileFeatureIntegrationGuardTest.kt`

- [ ] Write failing mapper, state, stale-request, session-change, verified-401, retry, ambiguous-match, and saved-session tests against the desired feature API.
- [ ] Run focused scanner tests and confirm RED due to missing feature types.
- [ ] Implement the minimum scanner domain and adapt the current host to it without introducing `ScannerFeatureCard`.
- [ ] Run focused scanner and integration guards to GREEN.
- [ ] Run JVM tests, lint, and debug assembly; commit.

### Task 3: Add bounded repeatable photo upload transport

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/MobilePhotoUploadSource.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/mobile_api.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/MobilePhotoUploadTransportTest.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/MobileFeatureIntegrationGuardTest.kt`

- [ ] Write failing tests for zero/oversized sources, exact content length, repeatable SHA-256, encoded task IDs, sanitized filename/MIME, short/long streams, and stream closure.
- [ ] Confirm RED against the current byte-array upload API.
- [ ] Implement a bounded repeatable source and multipart streaming while preserving all current `MobileApiClient` DTOs and methods.
- [ ] Adapt host URI preparation without exposing URI or bytes to feature state.
- [ ] Run focused transport tests and integration guards to GREEN, then JVM/lint/assemble; commit.

### Task 4: Extract Products and Photo Tasks incrementally

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/ProductsContract.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/ProductsFailure.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/ProductsMapper.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/ProductsGateway.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/ProductsStateHolder.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/ProductsCoordinator.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/PhotoTasksContract.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/PhotoTasksFailure.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/PhotoTasksMapper.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/PhotoTasksGateway.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/PhotoTasksStateHolder.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/products/PhotoTasksCoordinator.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/feature/products/*Test.kt`

- [ ] Write failing tests for mapping, validation, pagination, quick edit, stale requests, session changes, verified 401, photo upload ownership, completion, and disposal.
- [ ] Implement contracts and non-UI domain layers using the current API client and streaming source.
- [ ] Replace host-owned product/photo task state in small slices while preserving current presentation and notification routing.
- [ ] Run focused tests and integration guards to GREEN, then JVM/lint/assemble; commit.

### Task 5: Extract notification state and background coordination

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/notifications/NotificationsContract.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/notifications/NotificationsFailure.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/notifications/NotificationsMapper.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/notifications/NotificationsGateway.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/notifications/NotificationsStateHolder.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/notifications/NotificationsCoordinator.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/notifications/NotificationsBackgroundCoordinator.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/DlaFlowBackgroundSyncService.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/feature/notifications/*Test.kt`

- [ ] Write failing tests for mapper redaction, read-state updates, stale session/request rejection, atomic dedupe, verified 401, and background preference filtering.
- [ ] Implement the domain against current notification DTOs and signed API without changing panel capability rules.
- [ ] Keep `MobileNotificationPreferences`, FCM delivery, `OPEN_ORDERS`, canonical icon, and unread foreground summary in the final delivery path.
- [ ] Run focused tests and integration guards to GREEN, then JVM/lint/assemble; commit.

### Task 6: Extract Settings while keeping platform effects in the host

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/settings/SettingsContract.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/settings/SettingsMapper.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/settings/SettingsStateHolder.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/settings/SettingsCoordinator.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/settings/SettingsIntentLaunchPolicy.kt`
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/feature/settings/*Test.kt`
- Test: `app/src/androidTest/java/pl/dlaflow/mobile/SettingsFeatureScreenTest.kt`

- [ ] Write failing tests for mapping, typed routes/effects, host identity, launch policy, disconnect flow, and notification preference presentation.
- [ ] Implement settings domain and screen with host callbacks for Caller ID, overlay, notification settings, APK update, and disconnect.
- [ ] Preserve current preference storage and application behavior.
- [ ] Run focused tests and integration guards to GREEN, then JVM/lint/assemble; commit.

### Task 7: Repository boundaries, managed-device UI verification, and integration

**Files:**
- Modify: `scripts/verify-repository-contract.ps1`
- Create or modify: `scripts/verify-scanner-feature-boundary.ps1`
- Create or modify: `scripts/verify-products-feature-boundary.ps1`
- Create or modify: `scripts/verify-notifications-feature-boundary.ps1`
- Create or modify: `scripts/verify-settings-feature-boundary.ps1`
- Modify: `.github/workflows/mobile-release.yml`
- Modify: `app/src/androidTest/java/pl/dlaflow/mobile/**`
- Modify: `README.md`

- [ ] Add failing boundary fixtures proving feature-to-feature, platform, session storage, raw DTO, and secret imports are rejected.
- [ ] Implement and run all boundary scripts.
- [ ] Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon`.
- [ ] Run managed-device tests through `scripts/run-qa-emulator-tests.ps1` and verify light/dark at 360/412/600 dp, font 1.0/1.3, TalkBack semantics, overflow, Back, notification permission, and scanner permission/cancel states.
- [ ] Review the complete diff for preserved deadline/status/FCM/preference behavior and secret redaction.
- [ ] Commit, merge locally into mobile `main`, rerun the full gate on merged `main`, then remove the three historical extraction worktrees and local branches plus the integration worktree/branch.

