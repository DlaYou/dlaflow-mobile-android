# Mobile Orders Compact Scanner

**Date:** 2026-08-18

**Status:** Approved by the product owner on 2026-08-18

## Goal

Restore the Orders screen information hierarchy so filtering and order handling are primary, while package scanning remains available as a fast secondary action.

The selected presentation is variant C:

- expose exactly three order filters: `Wszystkie`, `Nowe`, and `Do wysyłki`,
- move package scanning to a QR action in the Orders header,
- launch the camera immediately after the QR action is tapped,
- show scan progress or the latest result in a compact strip above the order list,
- render no scanner surface when no scan has been started.

## Source Of Truth And Boundaries

The panel and mobile API remain the source of all order, shipment, and scan-result business data:

`integration -> integration normalizer -> DlaFlow model -> /api/mobile/* DTO -> Android mapper -> UI`

Android does not infer shipment ownership, match packages locally, translate provider-specific statuses, or add a parallel scanner model. It reuses the existing signed transport, scan lookup action, `MobilePackageScanUiState`, and normalized API response.

This change is limited to presentation and action placement. It does not change API contracts, integration normalizers, authentication, tenant isolation, camera permission handling, or package lookup semantics.

## Orders Filters

The visible filter set is ordered and fixed:

1. `Wszystkie` maps to `OrdersFilter.ALL`.
2. `Nowe` maps to `OrdersFilter.NEW`.
3. `Do wysyłki` maps to `OrdersFilter.TO_SHIP`.

`OrdersFilter.PROBLEMS` and `OrdersFilter.MESSAGES` remain in the internal contract and existing API mapping for compatibility. They are not rendered as buttons in this screen. The implementation exposes the visible set from one feature-owned definition instead of duplicating three ad hoc conditions in the UI.

The three controls use the existing DlaFlow filter component and semantic colors. They form one stable row where width permits. At 360 dp and with larger font they may wrap using the existing responsive layout, but labels must remain readable and touch targets remain at least 48 dp.

## Header Scanner Action

The existing DlaFlow top header gains a QR scanner icon action next to the notification action while the Orders tab is active. The control:

- uses the existing Material `QrCodeScanner` icon and DlaFlow icon-button styling,
- has a TalkBack label equivalent to `Skanuj paczkę`,
- preserves a minimum 48 dp touch target,
- dispatches the existing `DashboardAction.ScanPackage` immediately,
- does not open an intermediate card, sheet, or confirmation dialog.

The QR action is contextual. It is not added to Dashboard, Products, Notifications, or Settings. The notification action and its unread indicator retain their current behavior.

## Compact Scanner Result Strip

The large `Skaner paczek` card is removed from the Orders lead content. Scanner state is rendered as follows:

- `Empty`: render nothing and reserve no vertical space.
- `Loading`: show a compact progress strip above the order list with plain business copy indicating that the package is being checked.
- matched `Resolved`: show the order number, normalized order status, and an `Otwórz zamówienie` action.
- ambiguous `Resolved`: keep the existing warning meaning and require the operator to inspect the suggested order before further handling.
- unmatched `Resolved`: show `Nie znaleziono paczki` and a compact `Skanuj ponownie` action.
- `Failed`: show the existing safe error message and a compact `Skanuj ponownie` action.

The strip reuses DlaFlow surfaces, typography, semantic tones, buttons, and spacing. It must not introduce page-local colors or a second alert/card language. It may wrap vertically on narrow width or larger font, but it must not become a large promotional card.

Opening a matched order dispatches the existing order navigation action with the normalized order number. Retrying dispatches the same camera launch action. Empty, cancelled, or invalid camera output follows the current scanner flow and does not create a false result.

## Screen Information Hierarchy

The Orders list route uses this order:

1. DlaFlow top header with notification and contextual QR actions.
2. Orders title and supporting copy.
3. Search.
4. Three visible filters.
5. Existing KPI tiles.
6. Compact scanner result strip only when scanner state is not `Empty`.
7. Order list and its existing loading, content, empty, error, offline, and no-access states.

Removing the empty scanner card moves KPIs and orders upward without changing order-card content or the previously approved C2 status and timing layout.

## Accessibility And Visual Requirements

- Inter Variable and `letterSpacing = 0.sp` remain unchanged.
- Existing DlaFlow design-system colors provide light and dark mode behavior.
- The QR icon has an explicit TalkBack label; icon shape is not the only source of meaning.
- Result title, supporting text, status, and actions follow a coherent TalkBack order.
- All actions retain at least 48 dp touch targets.
- The UI is verified at 360, 412, and 600+ dp with larger font and no horizontal overflow.
- No clipped labels, white dark-mode frames, nested cards, or empty scanner gaps are allowed.
- Camera permission denied, denied permanently, cancellation, Back, and successful scan continue to use the existing safe behavior.

## Testing And Verification

Implementation follows RED -> GREEN. Focused tests cover:

- the visible filter set and its stable order,
- absence of `Problemy` and `Wiadomości` buttons while their enum/API mappings remain intact,
- QR action visibility only in the Orders context,
- immediate dispatch of the existing scan action,
- no scanner content for `Empty`,
- compact loading, matched, ambiguous, unmatched, and failed presentations,
- open-order and retry actions,
- light/dark layout at 360/412/600+ dp, larger font, TalkBack, and overflow.

Required repository verification:

```powershell
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

Managed Pixel 6 QA verifies the list flow and responsive phone layouts. The real managed Pixel Tablet test verifies the wide layout where relevant. The Operator package is installed only with the preserving `scripts/install-operator-apk.ps1` flow after the implementation is merged locally to `main`; QA must not clear or replace the paired Operator app data.

## Non-Goals

- No new API endpoint, normalizer, shipment matching, or business status mapping.
- No order-card or order-detail redesign.
- No new Retrofit, Hilt, Room, FCM, Navigation Compose, or Gradle module.
- No version bump, tag, APK publication, push, PR, panel deployment, or VPS change.
- No secret, token, raw scan code, customer data, payload, or private screenshot is stored in the repository or project memory.
