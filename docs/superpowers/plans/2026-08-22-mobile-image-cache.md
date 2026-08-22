# Mobile Image Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Stop repeat downloads of product/order thumbnails while preserving signed transport, session isolation and bounded image decoding.

**Architecture:** `MobileApiClient` exposes a media response with bytes, ETag and freshness metadata while retaining the existing byte-only compatibility method. `MobileImageCache` owns bounded RAM bytes, session-scoped disk thumbnail entries and in-flight request deduplication. A single screen-level loader uses that cache; the UI passes target dimensions into the loader and prefetches only a bounded first batch.

**Tech Stack:** Kotlin, Android `HttpURLConnection`, coroutines already present in the app, Compose, JVM unit tests, `java.io.File` and `java.security.MessageDigest`.

---

### Task 1: Media response validators

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/mobile_api.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/MobileApiClientTest.kt`

- [x] Add `MobileMediaResponse(bytes, etag, maxAgeMillis, notModified)` and `getMobileMediaResponse(token, pathWithQuery, ifNoneMatch)`.
- [x] Send `If-None-Match` only when a nonblank validator is supplied; parse `Cache-Control: max-age` with a one-hour fallback; accept `304` without reading an error body.
- [x] Keep `getMobileMedia` delegating to the response method and returning bytes only.
- [x] Add tests for 200 bytes/headers and 304 with the validator request header.
- [x] Run `./gradlew :app:testDebugUnitTest --tests pl.dlaflow.mobile.MobileApiClientTest --no-daemon` and confirm RED before implementation, then GREEN.

### Task 2: Session image cache

**Files:**
- Create: `app/src/main/java/pl/dlaflow/mobile/MobileImageCache.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/MobileImageCacheTest.kt`

- [x] Define a blocking loader API accepting a canonical path, a network callback and an optional disk flag.
- [x] Add session-scoped key hashing, synchronized byte LRU with 6 MiB/64-entry limits, and disk thumbnail entries under `cacheDir/mobile-image-cache/<session-scope>` with 24 MiB/128-entry limits.
- [x] Persist bytes and ETag/freshness metadata atomically; discard corrupt, expired and oversized entries.
- [x] Join concurrent same-key calls through `FutureTask`; remove in-flight state and avoid caching failures.
- [x] Handle 304 by refreshing metadata and returning stored bytes; expose aggregate `hits`, `misses`, `revalidations`, `failures` counters and a clear operation.
- [x] Test cache hit, key separation, deduplication, failure retry, TTL/304, session isolation and deterministic eviction.

### Task 3: Target-sized decode and shared loader

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileImageDecoding.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/MobileImageDecodePlanTest.kt`

- [x] Extend decode planning with a target maximum dimension, retaining hard input bounds and never upscaling.
- [x] Replace direct media calls with one `remember`ed session cache/loader; remove the product-specific loader recreation.
- [x] Use the same loader for orders, products and details; pass slot target dimensions from `DlaFlowThumbnail` and keep the current placeholder.
- [x] Test target-size sampling and invalid target values.

### Task 4: Bounded prefetch and session cleanup

**Files:**
- Modify: `app/src/main/java/pl/dlaflow/mobile/MobileAssistantScreen.kt`
- Modify: `app/src/main/java/pl/dlaflow/mobile/session_store.kt`
- Test: `app/src/test/java/pl/dlaflow/mobile/MobileImagePrefetchTest.kt`

- [x] Prefetch at most six distinct thumbnail URLs from currently visible list content after content becomes available; do not prefetch originals or all pages.
- [x] Clear the active cache when the session is cleared or replaced, without touching tokens or other session data.
- [x] Test URL deduplication and the six-item bound.

### Task 5: Verification and handoff

- [x] Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --no-daemon`.
- [ ] Run `npm run graphify:code` from the panel integration checkout only if the project memory update is accepted by Agent General; do not run shared services from this worktree.
- [x] Run `git diff --check`, inspect `git status --short --branch` and commit the branch. The mobile repository has no `npm`/handoff script; report the exact commit and checks to Agent General for integration.
