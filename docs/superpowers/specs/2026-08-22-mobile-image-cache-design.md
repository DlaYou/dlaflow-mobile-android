# Mobile Image Cache And Loading Design

## Goal

Avoid downloading the same product or order image repeatedly while keeping tenant data isolated, memory bounded, and the existing signed mobile transport unchanged.

## Scope

- Cache media by the canonical mobile path including the query string and variant.
- Keep a bounded in-memory LRU for the authenticated session.
- Keep a bounded disk cache for thumbnail bytes under the app cache directory. Disk entries are scoped by a session identity and are removed when the session changes or is cleared.
- Deduplicate concurrent requests for the same cache key. Failed requests never become cache entries.
- Honor a one-hour freshness window from the panel media contract and support `ETag` revalidation with `304 Not Modified`.
- Decode images to the requested UI target size while preserving the existing input byte/pixel safety limits.
- Prefetch only a small bounded set of first visible thumbnails after list content is available; never prefetch an entire order history.
- Expose only aggregate cache hit, miss, revalidation and failure counters. No URLs, tokens, customer data or raw payloads are logged or persisted.

## Boundaries and invariants

- The panel remains the source of truth for media and supplies `Cache-Control: private, max-age=3600` plus a stable `ETag`.
- Android continues to validate media paths and use the existing bearer/signature headers. No new image or networking dependency is introduced.
- Cache keys include session scope, canonical path and query/variant. A cache hit must never cross an authenticated session.
- Disk cache is best-effort. Corrupt, oversized, expired or unreadable entries are discarded and the request falls back to network.
- Cache limits are byte-bounded and deterministic; eviction is least-recently-used.
- A cache entry is committed only after the complete response passes the existing media byte limit and decodes successfully when loaded.

## Data flow

1. The shared screen-level loader canonicalizes the media path and asks the session cache for a decoded image.
2. The cache returns a fresh memory/disk value, or joins an existing in-flight request.
3. A network miss sends the signed request, optionally with `If-None-Match` from the disk entry.
4. `200` stores bounded bytes and validators; `304` refreshes freshness and reuses the stored bytes.
5. The loader decodes with a target dimension derived from the UI slot and returns the image bitmap.

## Verification

- Unit tests cover LRU eviction, TTL, session isolation, URL/variant key separation, concurrent deduplication, failure handling, validator handling and target-size decode planning.
- Existing JVM tests, lint and debug build remain green.
- UI verification checks list/detail reuse, light/dark, 360/412/600 dp and larger font without overflow. No release version or tag is changed.
