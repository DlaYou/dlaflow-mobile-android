# Wiadomości: liczniki filtrów i statusy listy

## Cel

Ujednolicić listę wiadomości z referencją DlaFlow: filtry mają pokazywać dostępne liczniki, wiersz ma mieć stabilne miejsce na przyszłą ikonę źródła lub awatar, a wiadomości nowe i nieprzeczytane mają być widoczne przez kropkę oraz kontrolowany badge.

## Zakres

- Zachować obecne dwa filtry biznesowe (`Wszystkie`, `Nieprzeczytane`) oraz kanały źródłowe.
- Pokazać `MessagesContent.total` przy filtrze wszystkich wiadomości i `MessagesContent.unreadCount` przy filtrze nieprzeczytanych.
- Nie wyliczać po stronie Androida liczników „Ważne” ani „Archiwum”, ponieważ obecne API mobilne ich nie dostarcza.
- Zostawić stały slot 38 dp na źródło/awatar; bez pobierania grafik z panelu. Slot ma neutralny placeholder i stabilną semantykę.
- Dla nowej wiadomości pokazać fioletową kropkę oraz badge `Nowe`; dla wiadomości już oznaczonej jako nieprzeczytana pokazać kropkę i badge `Nieprzeczytane`. Badge ma być umieszczony przy czasie i nie może powodować poziomego overflow.
- Zachować istniejące DTO, signed transport, filtry API i nawigację.

## Reguła statusu

`Nowe` oznacza nieprzeczytany wątek bez `readAt` i ze statusem `unread` lub `new`. `Nieprzeczytane` oznacza pozostałe wątki bez `readAt` z innym statusem. Odczytane wątki nie mają kropki ani badge.

## Weryfikacja

- Testy JVM dla liczników, reguły statusu i bezpiecznego fallbacku źródła.
- Test Compose dla filtrów i wiersza na szerokości emulatora.
- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon`.
- Instalacja APK na `DlaFlow_CallerId_API35` i otwarcie ekranu wiadomości.
