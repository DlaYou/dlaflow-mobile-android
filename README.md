# DlaFlow Mobile Assistant

Native Android workspace for the DlaFlow Mobile Assistant.

This is the standalone Android repository for the DlaFlow Mobile Assistant. The panel and API stay in `DlaYou/dlaflow-panel`; this repository contains only the native Android app.

## Current Scope

- Pair phone by scanning the Mobile Assistant QR code from `Integracje -> Wtyczki` and entering a required device name.
- Store mobile bearer token through Android Keystore.
- Verify the mobile session with `/api/mobile/me`.
- Clear the local session automatically when the panel revokes the phone.
- List active product photo tasks from the panel.
- Capture a full-resolution camera JPEG through `FileProvider` / `MediaStore.EXTRA_OUTPUT` or select an existing product photo.
- Upload the image to the product photo task and complete the task.
- Register Android call screening service for Caller ID.
- Show compact bottom Caller ID card with customer and order context.
- Test Caller ID lookup from the app without placing a real call.

## Architecture Foundation

The Android app remains a single Gradle `app` module with explicit package boundaries:

- `app/navigation` owns tabs, overlays and Android Back rules;
- `core/designsystem` owns Compose colors, dimensions, Inter typography, theme and shared UI primitives;
- `core/state` owns shared loading/content/empty/error/offline/no-access contracts;
- `core/network` owns transport-level error types and, in the next approved stage, signed transport;
- `feature/pairing` owns the pairing form, help, code/QR validation and the required device name;
- `feature/dashboard` owns dashboard presentation state, refresh coordination and the Compose dashboard screen;
- feature extraction is performed one area at a time without changing `/api/mobile/*` contracts.

The DlaFlow panel/API remains the source of truth for business models, tenant isolation, permissions, normalizers, storage and APK release metadata.

`MainActivity` remains the platform adapter for launching ZXing, persisting the successful session and starting post-pair Android services. The custom name and code are sent together through the existing atomic `POST /api/mobile/devices/pair/complete`; this extraction does not add an endpoint, bump the Android version, create a tag or publish an APK.

Dashboard transport still uses the current `MobileApiClient` through the host-owned `DashboardGateway` adapter until the approved transport phase.

## Required Local Verification

Run before every push:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-repository-contract.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-design-system-boundary.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-pairing-feature-boundary.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-dashboard-feature-boundary.ps1
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

Do not bump `versionCode`/`versionName`, create a `mobile-v*` tag or publish an APK without a separate release decision.

## Local Setup

Required on Windows:

- Android Studio
- Android SDK Platform 35 or newer
- Android SDK Platform Tools (`adb`)
- JDK 21

Local SDK path must stay outside Git. Use either `ANDROID_HOME` or a local `local.properties` file:

```text
sdk.dir=C:\\Users\\Maciej\\AppData\\Local\\Android\\Sdk
```

Open this folder in Android Studio:

```text
dlaflow-mobile-android
```

For local emulator API URL use:

```text
http://10.0.2.2:4000
```

For a physical phone on the same Wi-Fi use the computer LAN IP and API port:

```text
http://192.168.x.x:4000
```

Production must use HTTPS.

## First Smoke

1. Start local panel/API.
2. Open panel `/integrations?category=plugins&plugin=mobile-assistant`.
3. Click `Konfiguruj`, then generate the Mobile Assistant pairing QR code.
4. Run this Android app.
5. Configure the debug app to use the local API URL through the existing local smoke setup.
6. Tap `Skanuj kod QR` or enter the code manually; a valid code opens `Nazwij ten telefon` without consuming it.
7. Enter a synthetic device name and tap `Połącz telefon`.
8. Confirm that the app and panel both show the exact chosen device name together with the connected tenant/user.
9. In the panel product media tab, send a task with `Wyślij z telefonu`.
10. Tap `Odśwież zadania` in the app.
11. Open camera or select an image and confirm the panel product gallery receives the uploaded photo.
12. For camera capture, verify the app status shows a full file size and the backend stored image is not a thumbnail.
13. Use the Caller ID test field with a known order phone number and confirm the bottom card shows customer/order details.
14. Enable Caller ID role on a physical Android phone and perform a real incoming call smoke.

Caller ID UI can be previewed from the app, but final acceptance still requires a real physical-phone call smoke.

## Automated APK Release

The panel downloads the Android app from the private release directory used by `DlaYou/dlaflow-panel`, not from a public GitHub artifact.

GitHub Actions behavior:

- push to `main`: build-checks the Android APK and validates the panel `latest.json` manifest;
- tag `mobile-v<versionName>`: builds the APK and publishes the APK plus `latest.json` to the VPS release directory;
- manual workflow dispatch with `publish=true`: publishes the current build to the configured channel.

Before creating a release tag, update `versionCode` and `versionName` in `app/build.gradle.kts`. The tag must match `versionName`, for example:

```text
git tag mobile-v0.1.1
git push origin mobile-v0.1.1
```

Required GitHub secrets in this repository:

```text
ANDROID_SIGNING_KEYSTORE_BASE64
ANDROID_SIGNING_STORE_PASSWORD
ANDROID_SIGNING_KEY_ALIAS
ANDROID_SIGNING_KEY_PASSWORD
MOBILE_RELEASE_SSH_HOST
MOBILE_RELEASE_SSH_USER
MOBILE_RELEASE_SSH_KEY
MOBILE_RELEASE_DIR
```

Optional:

```text
MOBILE_RELEASE_SSH_PORT
```

`MOBILE_RELEASE_DIR` must point to the persistent VPS path that is mounted/read by the panel API as `MOBILE_ANDROID_RELEASES_DIR`. The workflow writes only `latest.json` and the generated APK there. Do not commit APK files, keystores, `local.properties`, `google-services.json` or release secrets.

Main branch builds use a debug APK only for CI validation. Tag releases and manual publish runs build a signed release APK with the repository signing secrets and publish it to the VPS release directory.

## Google Play Protect / Android Developer Verification

DlaFlow Mobile Assistant is distributed from the DlaFlow panel as a signed APK. The current production standard is to register `pl.dlaflow.mobile` through Android Developer Verification without publishing the app to Google Play Store yet.

Use the operational guide:

```text
docs/google-play-protect-verification.md
```

Use the local helper to export public APK and signing-certificate metadata for Google registration:

```powershell
.\scripts\export-google-play-protect-kit.ps1
```

The helper reads an already signed production APK and writes a local verification kit under `mobile-release/google-play-protect/`. It does not read or export the private signing key.
