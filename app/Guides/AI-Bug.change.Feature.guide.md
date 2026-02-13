# AI Coding Assistant Guide (Android) — WellStoreDroid

This guide defines how an AI coding assistant should work in this Android project.
Optimize for correctness, minimal diffs, and consistency with the existing app behavior.

---

## Project Summary

WellStoreDroid collects per-app usage statistics in the background and uploads them to a user-configured endpoint.

Core flow:
- Collect usage stats periodically
- Store samples locally (Room)
- Upload in batches with retries/backoff (WorkManager + Retrofit/OkHttp)
- User config via UI (Jetpack Compose) and persistence (DataStore)

---

## Tech Stack

- Kotlin
- Jetpack Compose
- MVVM
- WorkManager
- Room
- DataStore
- Retrofit + OkHttp
- kotlinx.serialization

---

## Non-negotiables

- No large rewrites unless explicitly requested.
- No new architecture frameworks/libraries unless explicitly requested.
- Do not change the core pipeline (collect → store → upload) unless asked.
- Keep diffs small and scoped; avoid unrelated cleanup/formatting churn.
- If behavior changes (even “slightly”), call it out clearly.

If something is ambiguous:
- Ask 1 focused question
  OR
- Offer 2 options with trade-offs.

---

## Workflow (every task)

1. Restate the task in 1–2 sentences.
2. Identify impacted area(s): UI / ViewModel / DB / Worker / Network / DataStore.
3. Propose a short plan (3–6 bullets).
4. Implement the smallest correct change.
5. Provide:
   - what changed
   - why
   - risks/edge cases
   - manual verification steps (since tests may be minimal)

Keep explanations short. Code is the output.

---

## Common Work Types and Rules

### 1) Bug fixes
- Find root cause first; don’t “paper over” with extra state or retries.
- Fix as close to the source as possible.
- Avoid changing behavior elsewhere.
- If a bug fix risks altering data semantics (timestamps, interval boundaries, sent/unsent state), call it out explicitly.

Deliverable format:
- Repro steps (if known)
- Root cause
- Patch
- How to verify

---

### 2) UI changes (Compose)
- Composables render state and emit events; they do not do IO.
- State belongs in ViewModel (or existing state holder).
- Prefer minimal UI edits over layout rewrites.
- Keep existing navigation + permission/config flow intact.
- Don’t introduce new design systems or UI component libraries.

If the UI change requires new state:
- Add it to the ViewModel state model
- Wire events from UI → ViewModel
- Keep transformations in ViewModel (or existing layer)

---

### 3) Payload / auth changes (Network contract work)
This is the most likely place to accidentally break the system. Be disciplined:

**Contract safety rules**
- Do not change payload shape unless the task explicitly says so.
- If payload shape must change:
  - update serialization models
  - update server request building
  - update any stored “pending upload” assumptions if relevant
- Avoid “clever” dynamic JSON building; prefer typed models.

**Auth rules**
- Never log tokens/secrets.
- Keep auth handling centralized (wherever it currently lives).
- If auth is stored locally:
  - use DataStore (preferred) or existing pattern
  - call out security implications if storing long-lived secrets
- If requests need headers (e.g., `Authorization`):
  - prefer an OkHttp interceptor *only if the project already uses this pattern*
  - otherwise follow existing Retrofit call construction patterns

**Failure handling**
- Non-2xx responses: explicitly handle (don’t silently ignore).
- Auth failures (401/403):
  - do not infinite-retry
  - surface an actionable status in UI if a status screen exists
  - preserve unsent data; do not mark as sent on failure

---

## Data Layer Rules

### Room
- Don’t change schema unless explicitly requested.
- If schema change is required:
  - keep it minimal
  - call out migration implications
- Be careful with “mark as SENT” logic — it’s part of reliability.

### DataStore
- DataStore is the source of truth for user config (endpoint URL, toggles, auth settings if used).
- Avoid duplicating config state in multiple places.

---

## WorkManager Rules

- Periodic work is not exact; do not assume exact timing.
- Preserve interval semantics if modifying collection logic.
- Avoid duplicate enqueues / runaway retries.
- Keep backoff and constraints consistent unless asked.

Auth-related worker behavior:
- 401/403 should not cause tight retry loops.
- Preserve unsent records so upload can resume after auth/config is fixed.

---

## Privacy & Logging

This app touches usage stats. Be extra careful.

- Do not log app usage details.
- Do not log endpoints + payload contents in a way that leaks user data.
- Never log auth secrets.

---

## Testing

If there are no tests yet:
- Still suggest what should be tested and how.
- Prefer adding small unit tests if there’s already any testing infrastructure.
- For Worker changes, propose WorkManager test strategy.
- For Room changes, propose DAO/instrumentation tests.

---

## Golden Examples (copy these patterns)

### Example A: Bug fix in upload marking
**Task:** “Uploads succeed but records aren’t marked SENT.”

Expected approach:
1. Locate where success response is handled.
2. Identify the database update path.
3. Ensure update runs only on success and is not skipped by early returns/cancellation.
4. Keep transaction boundaries sane (batch update).

Deliverables:
- Patch in Worker/repository
- Manual steps to verify (force an upload, confirm db flags or UI status)

---

### Example B: Small UI change on status screen
**Task:** “Add a status indicator for last upload time.”

Expected approach:
1. Add `lastUploadTime` to ViewModel state (or existing model).
2. Populate from existing DB/config/worker result source.
3. Render a simple Compose row; no redesign.
4. Ensure formatting is stable and null-safe (“Never uploaded”).

Deliverables:
- Minimal UI patch
- Note about where the value is sourced and when it updates

---

### Example C: Payload + auth update
**Task:** “Send `deviceId` and include `Authorization: Bearer …` header.”

Expected approach:
1. Add `deviceId` field to the request model (serialization).
2. Add auth token to DataStore if not already present (or match existing storage).
3. Apply header via existing networking pattern (interceptor if already used; otherwise per-call).
4. Update error handling: 401/403 should surface a clear status and stop tight retries.
5. Verify payload matches expected JSON structure.

Deliverables:
- Updated model + retrofit call path
- Clear note: “This changes server contract”
- Manual verification steps (log only high-level status, not secrets)

---

## Manual Verification Checklist (minimum)

- App launches; UI renders.
- Permission flow still works.
- Toggling collection on/off behaves.
- Collection persists locally.
- Upload triggers and on success marks records SENT.
- Failures retry later without data loss.
- Auth failures do not cause rapid retry loops.
