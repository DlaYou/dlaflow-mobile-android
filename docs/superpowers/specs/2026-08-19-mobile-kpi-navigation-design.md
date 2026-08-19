# Mobilna nawigacja kart KPI

## Cel

Istniejące karty `Nowe zamówienia`, `Do wysyłki`, `Po terminie` i `Wiadomości` mają działać jako skróty do odpowiadających im miejsc pracy operatora. Zachowujemy obecny wygląd i dane z kanonicznego dashboardu.

## Zachowanie

- Karty są klikalne na Pulpicie i w sekcji Zamówienia.
- `Nowe zamówienia` przełącza aplikację na Zamówienia z filtrem `NEW`.
- `Do wysyłki` przełącza aplikację na Zamówienia z filtrem `TO_SHIP`.
- `Po terminie` przełącza aplikację na Zamówienia z osobnym filtrem `OVERDUE`. Panel/API wybiera wyłącznie zamówienia z kanonicznym `shippingDeadlineAt` wcześniejszym niż chwila zapytania i bez potwierdzonego etapu wysyłki lub doręczenia. Android nie porównuje dat lokalnie.
- `Wiadomości` przełącza aplikację bezpośrednio na istniejącą dolną zakładkę `Wiadomości`; nie otwiera filtra zamówień ani nakładki powiadomień.
- Lista jest pobierana przez istniejący `OrdersCoordinator` i `/api/mobile/orders`; Android nie filtruje danych lokalnie i nie interpretuje integratora.
- Filtry `Wszystkie`, `Nowe`, `Do wysyłki`, `Po terminie` i `Problemy` są widoczne w ekranie Zamówień. Usuwamy z tego miejsca filtr `Wiadomości`, ponieważ wiadomości mają własną zakładkę.
- Wspólny `DlaFlowKpiTile` dostaje opcjonalną akcję z semantyką przycisku i minimum 48 dp. Bez akcji pozostaje zwykłą kartą.

## Granice

- Bez nowego endpointu, modelu biznesowego, biblioteki i modułu Gradle. Rozszerzamy istniejący filtr `/api/mobile/orders` o wartość `overdue`.
- Dashboard API liczy `Po terminie` według tej samej kanonicznej reguły co filtr listy, zamiast łączyć nieopłacone zamówienia z problemami przesyłek.
- Bez zmiany kolorów i układu kart.
- Bez podbijania wersji i publikacji APK.

## Weryfikacja

- Test mapowania celów zamówieniowych KPI na filtry oraz osobnego celu zakładki Wiadomości.
- Test API filtra `overdue`, wykluczenia zamówienia bez terminu, przed terminem i już wysłanego oraz izolacji tenantów.
- Test zgodności licznika `Po terminie` z regułą filtra.
- Test kontraktu, że oba miejsca przekazują akcję do wspólnej karty.
- Testy JVM, lint i debug build.
- Smoke na emulatorze Operator: `Wiadomości` przełączają dolną zakładkę, a `Po terminie` otwiera Zamówienia z właściwym filtrem bez utraty sesji.
