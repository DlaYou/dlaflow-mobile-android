# Mobile Notification Center Sync

## Decision

Android reuses the panel's canonical notification center through the existing signed `GET /api/mobile/notifications` contract. Every unread entry is eligible for delivery when its classified category is enabled in local preferences; tone (`info`, `success`, `warning`, `error`) and action type do not suppress an enabled entry. A disabled category remains silent and can be re-enabled later without losing the server entry.

## Data Flow

The foreground `MobileDataRefreshController` refreshes dashboard, notifications, and the visible orders list on its existing interval. `MainActivity` also starts one notification refresh immediately after a saved session is verified or a new pairing succeeds. The existing coordinator prevents overlapping requests and preserves session/401 handling.

The background service and JobScheduler continue polling the same endpoint. `NotificationsBackgroundCoordinator` filters only blank IDs and already-read entries, deduplicates successful native effects by notification ID, and delegates category preference checks to the callback that owns Android notification policy.

## Compatibility and Safety

No API, database, FCM event, signed transport, or provider-specific mapper changes are introduced. The legacy tone/action helper remains source-compatible but no longer blocks entries; category preferences are the single native eligibility decision. Panel/API remains the source of truth and tenant/session boundaries are unchanged.

## Verification

JVM regression tests cover periodic refresh, every tone for the important panel category, disabled categories, and delivery/deduplication of unread entries. The Android lint and debug build remain required before handoff; FCM still provides immediate delivery only for event types emitted by the panel, while polling covers all notification-center entries.
