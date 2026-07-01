# DlaFlow Mobile Assistant

Native Android workspace for the DlaFlow Mobile Assistant.

This is the standalone Android repository for the DlaFlow Mobile Assistant. The panel and API stay in `DlaYou/dlaflow-panel`; this repository contains only the native Android app.

## Current Scope

- Pair phone by scanning the Mobile Assistant QR code from `Integracje -> Wtyczki`.
- Store mobile bearer token through Android Keystore.
- Verify the mobile session with `/api/mobile/me`.
- Clear the local session automatically when the panel revokes the phone.
- List active product photo tasks from the panel.
- Capture a full-resolution camera JPEG through `FileProvider` / `MediaStore.EXTRA_OUTPUT` or select an existing product photo.
- Upload the image to the product photo task and complete the task.
- Register Android call screening service for Caller ID.
- Show compact bottom Caller ID card with customer and order context.
- Test Caller ID lookup from the app without placing a real call.

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
5. Enter API URL.
6. Tap `Skanuj kod QR`; the app fills the code and starts pairing. Manual code entry remains a fallback.
7. Confirm that the app shows connected tenant/user/device.
8. In the panel product media tab, send a task with `Wyślij z telefonu`.
9. Tap `Odśwież zadania` in the app.
10. Open camera or select an image and confirm the panel product gallery receives the uploaded photo.
11. For camera capture, verify the app status shows a full file size and the backend stored image is not a thumbnail.
12. Use the Caller ID test field with a known order phone number and confirm the bottom card shows customer/order details.
13. Enable Caller ID role on a physical Android phone and perform a real incoming call smoke.

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
