# DlaFlow Mobile Assistant Agent Contract

To repo zawiera wyłącznie natywną aplikację Android `pl.dlaflow.mobile`.
Panel, API, modele, tenant isolation, normalizery, storage i deploy VPS należą do osobnego repo `DlaYou/dlaflow-panel`.

## Start większego zadania

1. Przeczytaj panelowe `PROJECT_TODO.md`, `VPS_TODO.md`, `AGENTS.md`, `docs/AGENT_WORKFLOW.md` i `docs/DLAFLOW_AGENT_RULES.md`.
2. Przeczytaj mobilną specyfikację i plan dotyczący aktualnego zadania.
3. Sprawdź `git status --short --branch`, `git worktree list`, `origin/main`, aktualny tag `mobile-v*` i workflow Android.
4. Nie pracuj w checkoutcie z cudzymi zmianami. Użyj własnego brancha `codex/*` i osobnego worktree.
5. Nie kopiuj kodu Androida do repo panelu ani kodu panelu do repo Androida.

## Architektura

- Panel/API jest źródłem prawdy dla produktów, zamówień, przesyłek, powiadomień, dokumentów i uprawnień.
- Android korzysta wyłącznie z małych DTO pod `/api/mobile/*` i nie tworzy równoległych modeli biznesowych.
- `feature/*` może zależeć od `core/*`; `core/*` nie zależy od funkcji.
- Nowe ekrany używają `core/designsystem`, wspólnego UiState i istniejącego signed transportu.
- Nie dodawaj Retrofit, Hilt, Room, FCM, Navigation Compose ani nowego modułu Gradle bez osobnej zatwierdzonej specyfikacji.

## UI

- Inter Variable, `letterSpacing = 0.sp`, jeden zestaw ról kolorów DlaFlow.
- Zakaz nowych hexów poza `core/designsystem` i zatwierdzonymi assetami.
- Wspólne karty, nagłówki, przyciski, pola, badge, skeletony i stany loading/content/empty/error/offline/no-access.
- Minimum 48 dp dla akcji, TalkBack, większy font, safe insets, light/dark i brak overflow.
- UI używa prostego języka biznesowego, bez endpointów, payloadów, tenantów i technicznych wyjątków.

## Bezpieczeństwo

- Bearer token i klucz ECDSA pozostają w Android Keystore.
- Requesty zachowują body hash, timestamp, nonce, device ID i canonical signature.
- Token mobilny nie może wyjść poza `/api/mobile/*`.
- Nie zapisuj sekretów, tokenów, raw kodów skanów, danych klientów, payloadów ani prywatnych screenshotów.
- Pojedynczy `401` z pomocniczego endpointu nie kasuje sesji bez potwierdzenia przez `/api/mobile/me`.

## Definition of done

- Najpierw RED, potem minimalna implementacja i GREEN.
- Uruchom `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon`.
- Dla UI sprawdź light/dark, 360/412/600 dp, większy font, TalkBack, overflow, Back i permission states.
- Kamera, Caller ID, overlay, instalacja APK i zachowanie OEM wymagają fizycznego telefonu, jeśli zostały dotknięte.
- Nie podbijaj `versionCode`/`versionName`, nie twórz taga i nie publikuj APK bez osobnej zgody.
- Po zweryfikowanej zmianie zaktualizuj panelowy `PROJECT_TODO.md`; `VPS_TODO.md` tylko przy release/deploy/smoke produkcyjnym.
