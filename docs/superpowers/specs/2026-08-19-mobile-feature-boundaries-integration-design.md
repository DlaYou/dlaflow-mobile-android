# Mobile Feature Boundaries Integration Design

## Goal

Move the reusable scanner, product, notification, and settings boundaries from the historical extraction branches onto the current Android `main` without reverting newer order, notification, Firebase, or compact scanner behavior.

## Non-negotiable current behavior

- `OrdersPackageScannerStrip` remains the only scanner presentation on Orders. The historical large `ScannerFeatureCard` is not restored.
- Order DTOs keep `shippingDeadlineAt`, `statusColor`, canonical status tones, current order dates, and current detail/list behavior.
- Firebase installation registration, `order.created` routing, the canonical notification icon, dynamic foreground-service summary, and `MobileNotificationPreferences` remain active.
- Existing signed transport, ECDSA key handling, request body hash, timestamp, nonce, device ID, and `/api/mobile/*` restriction remain unchanged.
- No version bump, tag, APK publication, panel/API implementation, or VPS deployment is part of this integration.

## Architecture

The historical branches are reference implementations only. Code is ported into the current tree in dependency order and adapted to the current host:

1. Scanner domain: contract, failure, mapper, gateway, state holder, and coordinator. Android activity launchers stay in `MainActivity`; rendering stays in `OrdersPackageScannerStrip`.
2. Photo upload transport: bounded, repeatable streaming source with exact length, SHA-256 signing, path encoding, filename/MIME sanitization, and deterministic stream disposal. It extends the current `MobileApiClient` without deleting current DTOs or methods.
3. Products and photo tasks: contracts, mappers, gateways, state holders, and coordinators move under `feature/products`. Host-owned camera/gallery intents and URI access stay outside the feature.
4. Notifications: contract, mapper, gateway, state holder, coordinator, and background coordinator move under `feature/notifications`. Current FCM delivery and preferences remain the final delivery policy.
5. Settings: contract, mapper, state holder, coordinator, launch policy, and screen move under `feature/settings`. Android platform effects remain host-owned.

`feature/*` may depend on `core/*` and narrow compatibility interfaces. It may not own Activities, Intents, Keystore material, raw scan codes, content URIs, Firebase services, or session persistence.

## Data and error flow

- Gateways adapt the current `MobileApiClient`; mappers convert current DTOs to feature models.
- Coordinators attach a request generation and session identity to asynchronous work. Stale responses are ignored.
- A first `401` from a supporting call is verified through `/api/mobile/me`; session clearing remains a host/session-store decision.
- UI receives typed business states and failures, never response bodies, raw payloads, exception messages, tokens, or scan contents.
- Photo uploads use a repeatable stream because signing requires hashing before transmission. The byte count is bounded before network I/O, and every opened stream is closed.

## UI

The integration is primarily architectural. Existing current-main layouts remain the visual source of truth. New or ported screens use `core/designsystem`, Inter Variable, zero letter spacing, 48 dp actions, safe insets, TalkBack semantics, and the shared loading/content/empty/error/offline/no-access states.

Validation covers light/dark, widths 360/412/600 dp, font scale 1.0/1.3, overflow, Back, notification permissions, and scanner permission/cancel states. Physical-phone checks remain required for camera, Caller ID, overlay, APK install/update, notification channels, and OEM behavior.

## Integration and cleanup

Each layer is implemented test-first and committed separately. Whole historical branches are never merged. After the complete branch passes unit tests, lint, debug assembly, managed-device instrumentation, visual checks, and review, it is merged locally into mobile `main`. Only then are the three historical extraction worktrees and local branches removed. Remote branches are outside scope.

