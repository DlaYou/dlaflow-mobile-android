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
