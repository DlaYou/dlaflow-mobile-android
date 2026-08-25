# Wiadomości: liczniki filtrów i statusy listy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dopasować listę wiadomości do referencji DlaFlow bez pobierania ikon źródłowych i bez rozszerzania kontraktu panelu.

**Architecture:** Liczniki są prezentacją istniejących pól `MessagesContent`. Reguła statusu pozostaje w modelu UI, a Compose renderuje stabilny slot źródła i badge przy czasie. Nie dodajemy nowych endpointów, modeli sieciowych ani zależności.

**Tech Stack:** Kotlin, Jetpack Compose, istniejący `DlaFlowComposeColors`, JUnit4, Compose Android tests.

---

### Task 1: Model statusu i liczników

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesContract.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/feature/messages/MessagesContractTest.kt`

- [ ] Add failing tests for `isNew`, `isUnread`, and count lookup from `MessagesContent`.
- [ ] Run the focused JVM test and confirm it fails because the helpers do not exist.
- [ ] Add minimal derived properties/helpers with no network or provider interpretation.
- [ ] Run the focused JVM test and confirm it passes.

### Task 2: Filter chip counts

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesScreen.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/feature/messages/MessagesUiTextTest.kt`

- [ ] Add failing tests for count labels and count fallback when content is unavailable.
- [ ] Run the focused JVM test and confirm the expected failure.
- [ ] Render count as a compact secondary number inside the existing filter chip, using zero when content is not loaded.
- [ ] Keep channel chips unchanged and preserve horizontal scrolling.
- [ ] Run the focused JVM test and confirm it passes.

### Task 3: Source slot and row status badges

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesScreen.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/core/designsystem/DlaFlowComponents.kt` only if a shared badge primitive is required
- Test: `app/src/androidTest/java/pl/dlaflow/mobile/feature/messages/MessagesFeatureScreenTest.kt` (create if absent)

- [ ] Add a Compose test asserting the stable source slot and `Nowe`/`Nieprzeczytane` labels.
- [ ] Run the focused Compose test and confirm it fails before implementation.
- [ ] Replace the inline source icon with a stable neutral placeholder slot; leave the slot API ready for a future painter/avatar.
- [ ] Place status badge beside the timestamp, keep the dot for unread states, and use ellipsis/weight rules for narrow screens.
- [ ] Run the focused Compose test and confirm it passes.

### Task 4: Full verification and emulator

**Files:**
- No source changes.

- [ ] Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon`.
- [ ] Install the debug APK on `emulator-5554`, launch `pl.dlaflow.mobile`, and open messages.
- [ ] Capture a screenshot at the current emulator size and check no overflow in the filters or row badges.
- [ ] Run `git diff --check`, `git status --short --branch`, and commit the implementation.
- [ ] Run `npm run graphify:code` only from the panel integration checkout if requested by the handoff workflow; do not alter panel TODO files in this functional branch.
