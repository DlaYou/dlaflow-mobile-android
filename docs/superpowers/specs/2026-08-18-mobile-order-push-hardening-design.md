# Mobile Order and Push Hardening

**Status:** Approved for implementation by the product owner on 2026-08-18

**Goal:** Make mobile order timing and new-order notifications correct, testable, and releasable from a clean checkout.

## Scope

- Recreate the Firebase Google Services configuration in CI from an ignored GitHub secret and fail clearly when it is absent.
- Convert UTC timestamps from the mobile API to the device time zone before rendering order times.
- Make order timing formatting deterministic and cover missing, future, imminent, and overdue deadlines.
- Open the Orders tab when an operator taps a new-order notification, after the existing session checks.
- Add focused regression coverage without changing the mobile API contract or app version.

## Architecture

`OrdersScreen` will use pure order-time helpers that accept an explicit `Clock` and `ZoneId`; Compose string resources remain at the UI boundary. Notification taps will carry a typed launch extra owned by `DlaFlowDeepLinks`; `MainActivity` will consume it through the existing launch-intent path and select Orders only after the normal session state is available. CI will materialize `app/google-services.json` only in the runner workspace from `GOOGLE_SERVICES_JSON_BASE64`.

## Safety

No Firebase JSON, tokens, customer data, raw payloads, version changes, tags, pushes, or deployments are committed. The local panel remains the source of truth for production configuration and will be updated only with a redacted memory entry after verification.

## Verification

- JVM tests for UTC-to-device formatting and deadline labels.
- Android instrumentation coverage for notification launch intent and order-tab routing.
- Repository contract guard for the CI Firebase configuration step.
- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- `:app:connectedDebugAndroidTest` on API 35, plus manual install/start on `emulator-5554`.
