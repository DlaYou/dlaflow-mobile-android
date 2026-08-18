# Mobile Order Card Status And Timing

**Date:** 2026-08-18

**Status:** Approved by the product owner on 2026-08-18

## Goal

Make every order card answer three operator questions without cramped text:

1. What is the fulfillment state?
2. What is the payment state?
3. When was the order placed and when must it be shipped?

The selected presentation is variant C2: two labeled, dynamic order-status fields followed by a compact vertical timing line. The phone number is removed from the list card.

## Source Of Truth And Data Flow

The panel and API remain the only source of business meaning:

`integration -> integration normalizer -> DlaFlow order model -> /api/mobile/orders DTO -> Android mapper -> UI`

- Integration-specific values from Allegro, WooCommerce, InPost, or future providers are normalized before they reach Android.
- Android consumes the existing list DTO fields `status`, `statusTone`, `paymentStatus`, `paymentTone`, `createdAt`, and `shippingDeadlineAt`.
- The Android mapper transfers those fields without inventing provider-specific states or translating raw integration values.
- The UI may format timestamps in the device time zone and map the finite API tone vocabulary to DlaFlow design-system colors. It must not infer business status from raw strings.
- Every non-blank canonical status label from the API is displayed as received, including future statuses. Only a blank value is displayed neutrally as `Do sprawdzenia`; Android must not guess a more specific status.
- If the panel normalizer or mobile API does not yet provide a canonical state, that gap is fixed in the panel repository. It is not patched with an Android-only mapping.

This design does not require a second mobile model or a parallel order-status system.

## Card Information Hierarchy

The order card uses this stable order:

1. Customer, order number, sales channel, and amount.
2. Product summary.
3. Two C2 status fields.
4. Mini timing line.
5. Quiet delivery and activity footer.

The phone number is removed only from the list card. It remains available in order details and in the existing DTO where it is needed for operational actions.

## C2 Status Fields

The status area contains two equal fields:

- `Realizacja` displays the normalized fulfillment state, such as `Nowe`, `W realizacji`, `Gotowe do wysyłki`, `Wysłane`, or `Anulowane`.
- `Płatność` displays the normalized payment state, such as `Opłacone`, `Oczekuje na płatność`, `Płatność przy odbiorze`, or `Zwrot`.

These are live order states, not decorative labels and not hard-coded copies of `Nowe` and `Opłacone`. Value and tone come from the mobile API response.

The visual standard is a shared DlaFlow labeled-status component in `core/designsystem`, composed by the Orders feature. It uses the existing typography, spacing, surfaces, and semantic color roles. It does not introduce page-local hex colors or another pill system. The component communicates meaning through label and value; color is supporting information only.

On normal width, the two fields use a stable two-column layout. At 360 dp or when larger font makes either value unsafe, they stack vertically instead of truncating or overflowing. Long valid values may wrap to two lines without changing the width of the card.

## Mini Timing Line

Below the statuses, a compact vertical timeline presents:

- `Zamówiono` with the exact order date and time in the device time zone.
- `Wyślij do` with the API-provided deadline rendered as a business-friendly relative state and exact local date/time.

The deadline keeps the existing deterministic states: unavailable, overdue, minutes, hours, and days. Urgency may change the semantic tone, but the text always carries the meaning. Missing or invalid API data is shown as `Termin niedostępny`; the field is not silently removed.

The timeline uses two separate rows and does not force both values into one horizontal sentence. This prevents the truncation visible in the current card.

## Footer

The footer keeps the delivery method as quiet supporting information and preserves the existing message, shipment, and document counts. It no longer includes the customer phone number. When the delivery method is unavailable, the existing product-count fallback remains.

## Shared Component Boundary

The implementation adds or extends a generic DlaFlow status presentation primitive in `core/designsystem`. The primitive owns:

- label/value typography,
- semantic tone treatment,
- spacing and surface treatment,
- wrapping behavior,
- accessibility semantics.

The Orders feature owns:

- the labels `Realizacja` and `Płatność`,
- binding order DTO values to the shared primitive,
- the mini timing line,
- delivery and activity footer content.

`core/designsystem` must not depend on `feature/orders`, and no integration-specific status vocabulary is added to the Android design system.

## Accessibility And Visual Requirements

- Inter Variable and `letterSpacing = 0.sp` remain unchanged.
- Text and semantic roles work in light and dark mode using existing DlaFlow colors.
- TalkBack reads each field as one meaningful phrase, for example `Realizacja, Nowe`.
- Status meaning never depends on color alone.
- The full card remains the action target with at least 48 dp touch size.
- The layout is verified at 360, 412, and 600 dp, with larger font, without clipped text or horizontal overflow.
- No white borders, surfaces, or loading artifacts appear in dark mode.

## Failure And Compatibility Behavior

- Unknown tone values fall back to the neutral DlaFlow tone.
- Blank status values show `Do sprawdzenia` without local inference.
- Missing or malformed timestamps keep the existing safe fallback and never crash composition.
- Existing loading, content, empty, error, offline, and no-access states are unchanged.
- Existing signed transport, authentication, tenant isolation, navigation, and detail loading are unchanged.

## Verification

Implementation follows RED -> GREEN and adds focused coverage for:

- preservation of normalized API status values and tones through the Android mapper,
- neutral fallback for blank status values and unknown tone values,
- removal of the phone number from list-card quick information,
- timing presentation for missing, future, imminent, and overdue deadlines,
- responsive C2 status layout and mini timeline in light/dark at 360/412/600 dp,
- larger font, TalkBack semantics, card navigation, and overflow.

Required repository verification:

```powershell
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

UI verification runs on the QA emulator. The paired Operator emulator is not used for instrumentation and receives no destructive package operation.

## Non-Goals

- No new integration normalizer is implemented in the Android repository.
- No Retrofit, Hilt, Room, FCM, Navigation Compose, or Gradle module is added.
- No order-detail redesign is included.
- No API token, customer data, raw payload, or private screenshot is stored.
- No version bump, tag, APK publication, push, PR, panel deployment, or VPS change is included.
