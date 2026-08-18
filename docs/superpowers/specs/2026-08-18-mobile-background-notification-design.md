# Background Notification Summary

## Decision

The foreground-service notification keeps the Android-required ongoing-service presentation but uses a dedicated monochrome DlaFlow status-bar icon. Its title remains `DlaFlow działa w tle`; the content is an honest, dynamic summary of unread panel notifications: no new cases, one unread case, or the correctly inflected count.

The count comes from the existing `/api/mobile/notifications` polling response. No new endpoint, order-list fetch, channel, permission, or background workload is introduced. Caller ID and product-photo notifications keep their own channels and actions, but use the same valid monochrome small icon.

## Safety and behavior

If notification permission is unavailable, existing Android notification permission handling remains unchanged. If a poll fails, the last foreground notification remains visible. The service does not claim to count new orders because it does not fetch the order list in this path.

## Verification

Unit coverage locks Polish count formatting and the dedicated icon reference. Full Android unit tests, lint, debug build, and managed-device QA remain required before completion.
