# Mobile Order and Push Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make mobile order timing and new-order notifications correct, testable, and releasable from a clean checkout.

**Architecture:** Pure order-time helpers will own parsing, device-zone conversion, and deadline classification. Existing Compose screens will only localize and render the result. Notification routing will use a typed deep-link extra consumed by `MainActivity`; CI will decode an ignored Firebase configuration from a GitHub secret before Gradle tasks.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit, Android instrumentation tests, Gradle, GitHub Actions, PowerShell repository guards.

---

### Task 1: Add RED coverage for order timing

**Files:**
- Create: `app/src/test/java/pl/dlaflow/mobile/feature/orders/OrderTimingTest.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt` only after RED is observed.

- [ ] Write tests proving UTC timestamps render in an explicit Europe/Warsaw zone and deadline labels classify future, imminent, overdue, and invalid values.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests '*OrderTimingTest' --no-daemon` and confirm the new tests fail because the pure helpers do not exist.
- [ ] Implement the smallest pure formatter/classifier API and route the Compose labels through it.
- [ ] Re-run the focused tests and then the complete JVM suite.

### Task 2: Add RED coverage for notification routing

**Files:**
- Create or modify: `app/src/androidTest/java/pl/dlaflow/mobile/NotificationLaunchIntentTest.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/DlaFlowNotifications.kt`, `app/src/main/java/pl/dlaflow/mobile/MainActivity.kt` only after RED is observed.

- [ ] Write an instrumentation test proving a panel alert intent carries the Orders destination.
- [ ] Run the focused connected test and confirm the expected extra is absent or the destination is not consumed.
- [ ] Add a typed deep-link constructor and consume it in the existing launch-intent flow without bypassing pairing/session verification.
- [ ] Re-run the focused test and the complete connected suite.

### Task 3: Make clean CI Firebase-safe

**Files:**
- Modify: `.github/workflows/mobile-release.yml`
- Modify: `scripts/verify-repository-contract.ps1`

- [ ] Add a guard test/contract assertion requiring `GOOGLE_SERVICES_JSON_BASE64` materialization before Gradle validation/build.
- [ ] Run the contract check against the old workflow and confirm RED.
- [ ] Add a masked secret decode step that writes only `app/google-services.json` in the runner and fails when unset.
- [ ] Run the contract check and a clean checkout build with a local ephemeral value supplied through the environment, without printing its contents.

### Task 4: Responsive UI regression coverage

**Files:**
- Modify: `app/src/androidTest/java/pl/dlaflow/mobile/feature/orders/OrdersVisualSnapshotTest.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/orders/OrdersScreen.kt`

- [ ] Add a non-empty shipping deadline to the visual fixture.
- [ ] Run the existing light/dark and 360/412/600 dp matrix to observe any overflow or clipping.
- [ ] Constrain the timing row with weights and truncation that preserve both business values at large font sizes.
- [ ] Re-run the visual matrix and inspect the rendered states.

### Task 5: Full verification and integration

- [ ] Run all repository boundary guards.
- [ ] Run unit tests, lint, assemble, and connected Android tests.
- [ ] Install the APK and launch `MainActivity` on `emulator-5554`.
- [ ] Run `npm run graphify:code` in the panel repo after code changes and update `PROJECT_TODO.md` with redacted evidence.
- [ ] Commit the worktree, run the handoff checks, and merge the exact commit into local mobile `main`.
