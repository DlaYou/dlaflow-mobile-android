# Mobile Messages Source Logos Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render canonical panel source logos in the Android messages list while preserving a stable avatar-ready slot.

**Architecture:** Keep `providerId` from the signed mobile DTO as the only input. A small pure resolver maps normalized IDs to local drawable resources and accessible labels; a Compose mark renders the resource for known providers and the existing chat icon for unknown providers. InPost has separate light/dark drawables.

**Tech Stack:** Kotlin, Jetpack Compose, Android drawable-nodpi resources, JUnit4, Compose Android tests.

---

### Task 1: Resolver contract

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessageSourceMark.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/feature/messages/MessageSourceMarkTest.kt`

- [ ] Write failing tests for Allegro, case-insensitive Gmail, InPost light/dark, and unknown fallback.
- [ ] Run the focused JVM test and confirm it fails because the resolver does not exist.
- [ ] Implement the minimal enum and resolver with bounded normalized IDs.
- [ ] Run the focused JVM test and confirm it passes.

### Task 2: Local canonical assets

**Files:**
- Create: `app/src/main/res/drawable-nodpi/message_source_*.png`

- [ ] Convert the panel-approved SVG assets to PNG without changing their source artwork; copy existing PNG assets unchanged.
- [ ] Include light/dark InPost variants and keep dimensions appropriate for a 38 dp slot.
- [ ] Verify every resource name is Android-safe and has no duplicate resource collision.

### Task 3: Compose rendering

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/feature/messages/MessagesScreen.kt`
- Modify: `app/src/androidTest/java/pl/dlaflow/mobile/feature/messages/MessagesFeatureScreenTest.kt`

- [ ] Add a failing Compose assertion that a known provider exposes its source content description while unknown providers keep the slot.
- [ ] Render `MessageSourceMark` inside the current `message_source_slot` with fixed size, semantic label, and dark-aware InPost resource.
- [ ] Keep unread dot, status badges, and row sizing unchanged.
- [ ] Run the focused Compose test and confirm it passes.

### Task 4: Verification and handoff

**Files:**
- No additional source files.

- [ ] Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon`.
- [ ] Run `./gradlew :app:connectedDebugAndroidTest --no-daemon` on the available emulator.
- [ ] Run `git diff --check`, `npm run graphify:code` in the panel integration checkout, and inspect `git status`.
- [ ] Commit the branch and run `npm run agent:handoff -- -Checks "..."`; do not merge or publish.
