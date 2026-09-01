# Pretalx integration requirements

Status: WIP — out of scope for the current migration
Owner: `at.control.shiftcontrol.integrations.pretalx`
Last reviewed: 2026-08-31

## Purpose and scope

The Pretalx integration owns credentials, remote identity mappings, API adaptation, synchronization
runs, checkpoints, and publish/reconciliation work. It converts Pretalx responses into normalized
values for event catalogue and schedule; it never becomes the canonical owner of a local schedule
decision or staffing assignment.

The legacy synchronizer is evidence of required location/session import behavior, but its matching
by event name, location name, and session title is explicitly retired.

No Pretalx credentials, mappings, synchronization, or adapter code belong in the current migration.
This document preserves the later design constraints so Stage 2 scheduling work does not recreate
the unsafe legacy integration.

## Functional requirements

| ID | Status | Requirement | Responsibility/architecture source |
| --- | --- | --- | --- |
| `PTX-SEC-001` | WIP | Each connection credential has an internal UUID, encrypted recoverable secret material, label, host, masked presentation, creation time, and health/status. Full secrets are never returned in lists or placed in URLs, logs, audit records, events, or error messages. | Architecture P0, legacy Pretalx key API |
| `PTX-MAP-001` | WIP | Every imported external object is mapped by provider, Pretalx instance, event slug, object type, and external identifier. Name/title matching is not an identity strategy. | Architecture P1, legacy sync review |
| `PTX-PULL-001` | WIP | The adapter fetches all required pages and normalizes remote event, room, submission, and slot values into a stable internal contract before any owner module persists them. Pretalx room values become event-catalogue locations. | `S3`, architecture adapter flow |
| `PTX-PULL-002` | WIP | Remote timestamps are interpreted with an explicit source/event timezone and converted without using the application server's default timezone. | Architecture P1 |
| `PTX-SYNC-001` | WIP | A sync run records its connection, event scope, start/end time, checkpoint, per-object outcome, detected remote revision/hash, and failure details. One failed object does not roll back unrelated events or objects. | `S3`, architecture adapter flow |
| `PTX-SYNC-002` | WIP | Synchronization reconciles remote create, change, rename, cancellation, and deletion by stable mapping. It is idempotent and safe when rerun after a partial failure. | Architecture P1 |
| `PTX-SYNC-003` | WIP | A detected conflict or operational impact is placed in a reviewable queue or explicit decision flow. The integration never silently overwrites local schedule decisions or staffing assignments. | Architecture schedule impact handling |
| `PTX-PUBLISH-001` | WIP | A supported local publication is emitted as durable outbox work with an idempotency key, remote response/version recording, retry policy, and a reconciliation fetch. | Architecture outbox and Pretalx design |
| `PTX-OPER-001` | WIP | Requests use configured timeouts, rate limits, retry/backoff with jitter, and bounded failure handling. Concurrent application instances must not duplicate the same scheduled run. | Architecture P1/P0 operational findings |
| `PTX-TEST-001` | WIP | Adapter contract tests use recorded or synthetic Pretalx responses for the supported API version, including pagination, rename, deletion, timezone, partial failure, retry, and republish. | Architecture phase 5 exit condition |

## Ownership boundary

- `eventcatalog` accepts identity and descriptive metadata according to an approved field-ownership
  policy.
- `schedule` owns local draft placement and publication decisions.
- `shift` owns volunteer shifts and assignments, and can receive only reviewed/approved schedule
  impacts.
- The integration owns no user-facing access rule beyond its own restricted administration actions.

## Open questions

| ID | Question | Decision needed by |
| --- | --- | --- |
| `PTX-OPEN-001` | WIP — What is the field-level source-of-truth matrix for event metadata, locations/capabilities, sessions/submissions, and location/time placement? | Before the first sync migration. |
| `PTX-OPEN-002` | WIP — Which Pretalx API version, endpoints, webhook support, and remote revision fields are guaranteed by the deployed organizer instances? | Before adapter implementation. |
| `PTX-OPEN-003` | WIP — Which remote deletions may be automatically mirrored, and which require organizer review because local published or staffing data exists? | Before destructive reconciliation. |
