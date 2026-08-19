# Mobile Shipment Timestamps Design

**Status:** Approved by the product owner on 2026-08-19

## Goal

Show the canonical shipment milestone on the orders list and in order details:

- before dispatch: `Wyślij do`,
- after dispatch: `Wysłano`,
- after delivery: `Dostarczono`,
- missing required milestone timestamp: `Brak daty z integracji`.

## Source Of Truth

The panel/API remains the business source of truth. For every exact `SalesOrderShipment.id`, `/api/mobile/orders` derives `shippedAt` and `deliveredAt` from tenant-scoped `SalesOrderShipmentHistory`. The API also exposes the canonical DlaFlow shipment stage. Android does not inspect provider payloads, provider-specific statuses, label creation dates, or tracking text.

The list DTO carries the current shipment stage/status and milestone timestamps. The detail DTO carries stage and timestamps for each shipment. Missing or invalid history dates are returned as empty/null values and are never replaced with `shipmentCreatedAt` or another guessed timestamp.

## Android Presentation

One shared pure presentation rule is used by list and detail:

1. `delivered` selects `Dostarczono` and `deliveredAt`.
2. `sent`, `transit`, `delivery`, `pickup`, `problem`, or `returned` selects `Wysłano` and `shippedAt`.
3. Earlier or unknown stages select `Wyślij do` and `shippingDeadlineAt`.
4. A missing/invalid selected milestone date shows `Brak daty z integracji`.

Dates are formatted in the device time zone. Existing deadline urgency presentation remains unchanged. All operator strings remain Android resources and the existing DlaFlow timeline/design-system components are reused.

## Scale And Safety

The mobile list loads shipment history only for the selected shipment IDs in the current page and always filters by tenant. Detail history is matched by exact shipment ID. No provider payload, tracking number, secret, or customer data is persisted in documentation or logs.

## Verification

- API helper tests: dispatch, delivery, exact shipment identity, missing ID, invalid date, no guessing.
- API route tests: tenant-scoped list and detail DTOs.
- Android parser, mapper, and pure presentation tests.
- Android unit tests, lint, debug assembly, and focused Compose UI tests for list/detail states.
- Visual behavior checked in light/dark and 360/412/600 dp using existing order-screen coverage.

