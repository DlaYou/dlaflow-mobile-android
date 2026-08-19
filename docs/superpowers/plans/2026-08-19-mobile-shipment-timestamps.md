# Mobile Shipment Timestamps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose canonical shipment milestone timestamps in the mobile API and render them consistently on the Android orders list and detail screen.

**Architecture:** The panel derives timestamps from tenant-scoped history for an exact shipment ID and exposes a canonical stage. Android maps the DTO fields unchanged and uses one shared presentation selector; provider interpretation stays outside Android.

**Tech Stack:** TypeScript, Fastify, Prisma, Node test runner, Kotlin, Jetpack Compose, JUnit 4.

---

### Task 1: API milestone derivation

- [x] Write failing helper tests for exact shipment history and missing/invalid data.
- [x] Implement canonical `shippedAt`/`deliveredAt` derivation.
- [x] Extend list and detail DTOs with stage and milestone timestamps.
- [x] Add tenant-scoped route assertions and run API typecheck/tests.

### Task 2: Android transport and mapping

- [x] Add failing parser/mapper assertions.
- [x] Extend transport and Orders models without provider-specific fields.
- [x] Map stage and timestamps unchanged.
- [x] Run focused JVM tests.

### Task 3: Shared presentation

- [x] Add failing tests for deadline, shipped, delivered, and missing date.
- [x] Implement one shared stage-based presentation selector.
- [x] Reuse it on the list timing line and in order details.
- [x] Add focused Compose assertions.

### Task 4: Verification and handoff

- [ ] Run API checks, focused route tests, lint, and Graphify.
- [ ] Run Android unit tests, lint, debug assembly, and Orders instrumentation tests.
- [ ] Inspect diff, update `PROJECT_TODO.md`, commit both clean worktrees, and prepare panel handoff/mobile integration.

