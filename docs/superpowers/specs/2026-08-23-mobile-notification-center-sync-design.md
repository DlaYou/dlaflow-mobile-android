# Mobile Notification Center Sync

## Decision

Android reuses the panel's canonical notification center through the existing signed `GET /api/mobile/notifications` contract. The center is an in-app history surface; it is not a second push transport. Ordinary history rows (including inbound-message rows such as `Odebrano nową wiadomość`, order/status updates and successful operations) never create a native alert. The only history exception is a red critical entry, which is controlled by the `Ważne sprawy z panelu` switch.

Native delivery is standardized by origin and category:

| Origin | Native delivery rule |
| --- | --- |
| FCM `order.created` | `Nowe zamówienia` switch |
| FCM `message.created` | `Wiadomości od klientów` switch |
| Photo-task dispatch | `Zadania zdjęciowe` switch |
| Notification-center history | Critical `error` only, using `Ważne sprawy z panelu`; ordinary rows stay in the center |

Order-status and shipment-status switches are reserved for explicitly mapped future push events. An explicit event received while its switch is disabled is consumed once without an alert, so enabling the switch later does not replay an old event. History rows that are not eligible for native delivery remain available in the in-app center.

## Data Flow

The foreground `MobileDataRefreshController` refreshes dashboard, notifications, and the visible orders list on its existing interval. `MainActivity` also starts one notification refresh immediately after a saved session is verified or a new pairing succeeds. The existing coordinator prevents overlapping requests and preserves session/401 handling.

The background service and JobScheduler continue polling the same endpoint. `NotificationsBackgroundCoordinator` filters blank IDs and already-read entries, deduplicates native effects by the canonical notification ID, and delegates the origin/category decision to the single policy callback that owns Android notification delivery. FCM and polling therefore share one claim memory and cannot emit the same canonical event twice.

## Compatibility and Safety

No new mobile endpoint, database field or signed transport is introduced. The panel's bounded FCM payload carries the target device and canonical notification ID; Android keeps a backward-compatible fallback for older payloads. The legacy tone/action helper remains source-compatible, while all real delivery paths use the origin-aware preference policy. Panel/API remains the source of truth and tenant/session boundaries are unchanged.

## Verification

JVM regression tests cover periodic refresh, every preference switch, disabled-event consumption, history-vs-FCM separation, canonical-ID deduplication and session reset. The Android lint and debug build remain required before handoff; FCM still provides immediate delivery only for event types emitted by the panel, while polling refreshes the in-app notification center and emits only the critical-history exception.
