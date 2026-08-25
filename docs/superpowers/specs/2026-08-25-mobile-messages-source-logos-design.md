# Wiadomości: kanoniczne logotypy źródeł

## Cel

Lista wiadomości ma pokazywać realny znak źródła, gdy `providerId` wskazuje dostawcę obsługiwanego przez panel. Android nie pobiera grafiki z URL-a ani nie odtwarza logiki panelu; korzysta z zatwierdzonych, lokalnych assetów oraz bezpiecznego fallbacku.

## Zakres

- Zachować istniejący kontrakt `/api/mobile/messages` i pole `providerId`.
- Obsłużyć lokalne assety Allegro, Apilo, DPD, Gmail, InPost, ORLEN Paczka i WooCommerce.
- Dla InPost wybrać wariant jasny/ciemny zgodnie z motywem DlaFlow.
- Dla nieznanego lub pustego `providerId` użyć ikony rozmowy i etykiety „Nieznane źródło”.
- Zachować stały slot 38 dp, semantykę TalkBack `Źródło: ...` oraz test tag `message_source_slot`.
- Zostawić slot gotowy do późniejszego zastąpienia awatarem bez zmiany układu wiersza.

## Poza zakresem

- Zmiany panelowego API, zdalne URL-e, pobieranie grafik, cache obrazów i zmiany wersji aplikacji.

## Weryfikacja

- Test JVM resolvera dla aliasów, wielkości liter i fallbacku.
- Test Compose obecności assetów oraz fallbacku na liście wiadomości.
- JVM, lint, debug build i testy instrumentacyjne; kontrola light/dark bez overflow.
