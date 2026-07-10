# Android pairing version metadata hotfix

## Cel

Nowo sparowane urządzenie Android ma przekazywać do istniejącego endpointu zakończenia parowania swoją rzeczywistą wersję aplikacji. Panel ma dzięki temu od razu pokazywać `appVersion.code` i `appVersion.name` zamiast wartości `null`.

Hotfix zostanie wydany jako `versionName = 0.4.2` i `versionCode = 16`.

## Zakres

- Zmiana wyłącznie w repozytorium Android Mobile Assistant.
- Bez zmian kontraktu, schematu ani implementacji API panelu; backend już przyjmuje, zapisuje i zwraca oba pola.
- Bez zmiany ekranów i przebiegu obowiązkowego nadawania nazwy telefonu.
- Bez refaktoru transportu, podpisu żądań lub przechowywania sesji.

## Rozważone podejścia

1. **Wartości wersji jako jawne, testowalne zależności `MobileApiClient` — wybrane.** Domyślnie pochodzą z `BuildConfig.VERSION_CODE` i `BuildConfig.VERSION_NAME`, a test może przekazać kontrolowane wartości. Zapewnia małą zmianę, jednoznaczny kontrakt i dokładny test payloadu.
2. Odczyt `BuildConfig` bezpośrednio wewnątrz `completePairing`. Kod byłby krótszy, ale metoda byłaby silniej związana z globalnym stanem buildu i trudniejsza do precyzyjnego testowania.
3. Wnioskowanie wersji po stronie API z nagłówka lub User-Agent. Odrzucone, ponieważ tworzyłoby drugi, mniej wiarygodny kanał danych mimo istniejącego kontraktu pairing DTO.

## Projekt

`MobileApiClient` otrzyma dwa prywatne parametry konstruktora z bezpiecznymi wartościami domyślnymi z `BuildConfig`:

- `appVersionCode: Int`
- `appVersionName: String`

`completePairing` doda je do istniejącego JSON obok `deviceName`, `pairingCode` i `platform` jako:

- `appVersionCode`
- `appVersionName`

Pozostały przepływ pozostaje bez zmian: klucz publiczny podpisu jest opcjonalnie dołączany, API zwraca token i urządzenie, a klient potwierdza sesję przez `/api/mobile/me`.

## Obsługa błędów i bezpieczeństwo

- Wersja pochodzi z artefaktu aplikacji, a nie z danych wpisywanych przez użytkownika.
- Nie logujemy kodu parowania, tokenu, klucza, pełnego payloadu ani danych klienta.
- Zachowujemy istniejący signed transport i canonical request bez zmian.
- Błąd API nadal przechodzi przez obecną obsługę `postJson`; hotfix nie dodaje alternatywnej ścieżki parowania.

## Testy i kryteria akceptacji

1. Test jednostkowy najpierw ma się nie powieść, gdy oczekuje obu pól wersji w body zakończenia parowania.
2. Po minimalnej implementacji test potwierdza dokładne wartości `16` i `0.4.2` oraz zachowanie klucza publicznego podpisu.
3. Pełna bramka: testy jednostkowe, lint, debug APK i release APK.
4. Metadane zbudowanego APK muszą wskazywać `0.4.2 (16)`.
5. Po PR, merge, tagu `mobile-v0.4.2` i publikacji manifest produkcyjny oraz checksum muszą zgadzać się z artefaktem.
6. Izolowany smoke parowania ma potwierdzić w panelowym API `appVersion.code = 16` i `appVersion.name = 0.4.2`, działający podpis żądań, revoke oraz pełne usunięcie tymczasowych danych.
7. Fizyczny telefon ma zostać zaktualizowany przez podpisany APK z zachowaniem istniejącej sesji; nie rozłączamy go wyłącznie w celu ponownego parowania.

## Poza zakresem

- QR smoke na fizycznym telefonie, jeśli wymagałby utraty istniejącej sesji.
- Zmiany backendu, panelu, modeli danych lub migracji.
- Nowe biblioteki, moduły Gradle i zmiany UI.
