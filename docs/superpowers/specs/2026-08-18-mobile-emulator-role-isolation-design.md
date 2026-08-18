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
- **QA:** Gradle Managed Virtual Device `dlaflowQaApi35`. It runs instrumentation, visual snapshots, permission-state tests, package clears, and test APK installs. No real customer account is paired there.

The QA device uses the already installed API 35 Google APIs x86_64 image and a Pixel 6 profile. Android Gradle Plugin creates, starts, snapshots, and removes its own managed device without enumerating connected devices. It never clones the Operator data image.

## Commands And Guardrails

Repository-owned PowerShell scripts provide the only documented local paths:

1. `app/build.gradle.kts` defines only the Gradle Managed Virtual Device `dlaflowQaApi35` for instrumentation.
2. `scripts/run-qa-emulator-tests.ps1` runs only `:app:dlaflowQaApi35DebugAndroidTest --no-daemon`. It never invokes a connected-device task.
3. `scripts/install-operator-apk.ps1` resolves only the Operator AVD and installs `app-debug.apk` with `adb install -r`. It refuses ambiguous devices, missing APKs, and any operation requiring uninstall or package-data clearing.

The Operator role resolver queries the AVD name through `adb -s <serial> emu avd name`. It fails closed when the expected AVD is unavailable, multiple matching devices exist, a physical phone is selected, or the AVD role cannot be proven. Serial numbers are runtime details and are never treated as stable role identities.

README verification commands change from raw `connectedDebugAndroidTest` to the managed-device test script. A repository contract test requires the Operator AVD name, the managed QA device, the QA-only Gradle task, and the preserving `adb install -r` operator path. Raw connected-device and destructive commands remain undocumented.

## Data And Security

- No session, token, ECDSA key, Firebase installation ID, customer data, screenshot, or emulator data directory is copied between AVDs.
- The Operator AVD is never used by instrumentation tests.
- QA may use synthetic test data and the existing local API only.
- A normal APK update must retain the same package name and debug signing identity; a signing mismatch fails without uninstalling the installed app.
- The scripts do not stop shared panel services or mutate production/VPS state.

## Failure Handling

Every script stops before mutation when role detection is uncertain. The error states the detected serial and AVD role without exposing application data. A missing QA AVD is fixed through the setup script; a signing mismatch on Operator requires an explicit engineering decision and never triggers automatic uninstall.

## Verification

- Static PowerShell contract tests require managed-device isolation, Operator AVD identity checks, physical-device rejection, ambiguity handling, and exact Gradle/ADB command construction.
- QA runs the full managed-device suite successfully while the Operator remains paired before and after the run.
- Operator receives the current debug APK through `install -r`, starts successfully, and retains its session.
- Existing five boundary guards and `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` remain green.

## Scope

No Android business code, API contract, Firebase configuration, application ID, version, release tag, push, PR, or VPS deployment changes. The result is local emulator workflow isolation only.
