# Mobile Emulator Role Isolation Design

**Date:** 2026-08-18

## Goal

Keep one Android emulator permanently paired with the operator's DlaFlow account while allowing the full instrumentation suite to clear and reinstall the app on a separate QA emulator.

## Root Cause

`adb install -r` preserves the application data and Android Keystore of `pl.dlaflow.mobile`. `connectedDebugAndroidTest` owns the target test package lifecycle and may clear or reinstall it. Running that suite on the operator emulator therefore removes the paired session even though a later APK installation uses `-r`.

Session backup and restore is not an acceptable workaround. The bearer token and ECDSA key are protected by Android Keystore and must not be exported, copied between AVDs, written to files, or reconstructed outside the normal pairing flow.

## Chosen Architecture

Two AVDs have explicit, non-overlapping roles:

- **Operator:** `DlaFlow_Task6_Dashboard_API35_20260717`. It keeps the existing package `pl.dlaflow.mobile`, account pairing, permissions, and Keystore. It may receive only a preserving APK update and normal manual smoke actions.
- **QA:** `DlaFlow_QA_API35`. It runs `connectedDebugAndroidTest`, visual snapshots, permission-state tests, package clears, and test APK installs. No real customer account is paired there.

The QA AVD uses the already installed `system-images;android-35;google_apis;x86_64` image and a Pixel 6 profile. Creating it is idempotent and does not clone the Operator data image.

## Commands And Guardrails

Repository-owned PowerShell scripts provide the only documented local paths:

1. `scripts/setup-qa-emulator.ps1` creates `DlaFlow_QA_API35` when absent and verifies its API/image/profile contract when present.
2. `scripts/run-connected-tests.ps1` starts or selects only `DlaFlow_QA_API35`, waits for Android boot completion, sets `ANDROID_SERIAL` for the child Gradle process, and runs `:app:connectedDebugAndroidTest --no-daemon`.
3. `scripts/install-operator-apk.ps1` resolves only the Operator AVD and installs `app-debug.apk` with `adb install -r`. It refuses ambiguous devices, the QA AVD, missing APKs, and any operation requiring uninstall or package-data clearing.

The role resolver queries the AVD name through `adb -s <serial> emu avd name`. It fails closed when the expected AVD is unavailable, multiple matching devices exist, a physical phone is selected, or an AVD role cannot be proven. Serial numbers are runtime details and are never treated as stable role identities.

README verification commands change from raw `connectedDebugAndroidTest` to the guarded test script. A repository contract test requires both role names, the QA-only Gradle invocation, `ANDROID_SERIAL`, and the preserving `adb install -r` operator path. Raw destructive commands remain undocumented.

## Data And Security

- No session, token, ECDSA key, Firebase installation ID, customer data, screenshot, or emulator data directory is copied between AVDs.
- The Operator AVD is never used by instrumentation tests.
- QA may use synthetic test data and the existing local API only.
- A normal APK update must retain the same package name and debug signing identity; a signing mismatch fails without uninstalling the installed app.
- The scripts do not stop shared panel services or mutate production/VPS state.

## Failure Handling

Every script stops before mutation when role detection is uncertain. The error states the detected serial and AVD role without exposing application data. A missing QA AVD is fixed through the setup script; a signing mismatch on Operator requires an explicit engineering decision and never triggers automatic uninstall.

## Verification

- Static PowerShell contract tests cover Operator rejection in the QA runner, QA rejection in the Operator installer, physical-device rejection, ambiguity, boot waiting, and exact Gradle/ADB command construction.
- `setup-qa-emulator.ps1` is run twice to prove idempotency.
- QA runs the full connected suite successfully while the Operator remains paired before and after the run.
- Operator receives the current debug APK through `install -r`, starts successfully, and retains its session.
- Existing five boundary guards and `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` remain green.

## Scope

No Android business code, API contract, Firebase configuration, application ID, version, release tag, push, PR, or VPS deployment changes. The result is local emulator workflow isolation only.
