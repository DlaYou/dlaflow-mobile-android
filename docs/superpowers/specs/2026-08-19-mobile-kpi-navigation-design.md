# Mobilna nawigacja kart KPI

## Cel

Istniejące karty `Nowe zamówienia`, `Do wysyłki`, `Po terminie` i `Wiadomości` mają działać jako skróty do odpowiadających im list zamówień. Zachowujemy obecny wygląd i dane z kanonicznego dashboardu.

## Zachowanie

- Karty są klikalne na Pulpicie i w sekcji Zamówienia.
- Kliknięcie przełącza aplikację na zakładkę Zamówienia i ustawia odpowiednio filtr `NEW`, `TO_SHIP`, `PROBLEMS` albo `MESSAGES`.
- Lista jest pobierana przez istniejący `OrdersCoordinator` i `/api/mobile/orders`; Android nie filtruje danych lokalnie i nie interpretuje integratora.
- Wszystkie cztery filtry są widoczne w ekranie Zamówień, aby wybrany stan był jednoznaczny i możliwy do zmiany.
- Wspólny `DlaFlowKpiTile` dostaje opcjonalną akcję z semantyką przycisku i minimum 48 dp. Bez akcji pozostaje zwykłą kartą.

## Granice

- Bez nowych endpointów, modeli biznesowych, bibliotek i modułów Gradle.
- Bez zmiany liczb KPI, kolorów i układu kart.
- Bez podbijania wersji i publikacji APK.

## Weryfikacja

- Test mapowania celu KPI na filtr zamówień.
- Test kontraktu, że oba miejsca przekazują akcję do wspólnej karty.
- Testy JVM, lint i debug build.
- Smoke na emulatorze Operator: karta przełącza zakładkę i zaznacza właściwy filtr bez utraty sesji.
