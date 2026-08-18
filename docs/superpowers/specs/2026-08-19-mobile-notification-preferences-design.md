# Mobile Notification Preferences

## Decision

Mobile notification delivery remains on the existing two transports: Firebase data messages for immediate order-created alerts and the authenticated background poll for panel notifications. The phone adds a local, encrypted-session-scoped preference set with independent switches for:

- new orders,
- customer messages,
- order status changes,
- shipment status changes,
- photo tasks,
- other important panel matters.

All switches default to enabled so existing users do not lose alerts after an update. The foreground-service notification is mandatory Android infrastructure and is never disabled by these switches.

## Classification and privacy

Notifications are classified from the existing safe title, action type, source and description metadata. The classifier is shared by Firebase and background polling. It does not persist raw provider payloads or customer message bodies. New-order Firebase messages are classified explicitly as `NEW_ORDER`. Unknown notifications retain the existing important-panel behavior and can be controlled by the important-panel switch.

## UI

The existing DlaFlow More -> Powiadomienia detail screen gains a compact list of six 48dp+ switch rows using the shared Compose colors, typography and card primitives. The Android system notification settings remain available for the global permission/channel controls. The summary row reports how many categories are enabled.

## Failure and lifecycle

If Android notification permission is denied, preferences still save and are applied when permission is restored. A single failed poll does not change preferences or clear the session. Session disconnect clears device-scoped notification preferences back to defaults, avoiding leakage between paired tenants.

## Verification

Unit tests cover defaults, persistence encoding, every category classification, disabled-category filtering, Firebase order gating, and the settings model. Android unit tests, lint, debug build and emulator smoke must pass. No new API endpoint, database field, secret or version bump is introduced.
