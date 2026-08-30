# Pretalx integration requirements

Status: Planned
Owner: `at.control.shiftcontrol.integrations.pretalx`
Last reviewed: 2026-08-30

## Purpose and scope

The Pretalx integration owns credentials, remote identity mappings, API adaptation, synchronization
runs, checkpoints, and publish/reconciliation work. It converts Pretalx responses into normalized
values for event catalogue and schedule; it never becomes the canonical owner of a local schedule
decision or staffing assignment.

The legacy synchronizer is evidence of required room/session import behavior, but its matching by
event name, room name, and session title is explicitly retired.

## Functional requirements

| ID | Status | Requirement | Responsibility/architecture source |
| --- | --- | --- | --- |
| `PTX-SEC-001` | Planned | Each connection credential has an internal UUID, encrypted recoverable secret material, label, host, masked presentation, creation time, and health/status. Full secrets are never returned in lists or placed in URLs, logs, audit records, events, or error messages. | Architecture P0, legacy Pretalx key API |
| `PTX-MAP-001` | Planned | Every imported external object is mapped by provider, Pretalx instance, event slug, object type, and external identifier. Name/title matching is not an identity strategy. | Architecture P1, legacy sync review |
| `PTX-PULL-001` | Planned | The adapter fetches all required pages and normalizes remote event, room, submission, and slot values into a stable internal contract before any owner module persists them. | `S3`, architecture adapter flow |
| `PTX-PULL-002` | Planned | Remote timestamps are interpreted with an explicit source/event timezone and converted without using the application server's default timezone. | Architecture P1 |
| `PTX-SYNC-001` | Planned | A sync run records its connection, event scope, start/end time, checkpoint, per-object outcome, detected remote revision/hash, and failure details. One failed object does not roll back unrelated events or objects. | `S3`, architecture adapter flow |
| `PTX-SYNC-002` | Planned | Synchronization reconciles remote create, change, rename, cancellation, and deletion by stable mapping. It is idempotent and safe when rerun after a partial failure. | Architecture P1 |
| `PTX-SYNC-003` | Planned | A detected conflict or operational impact is placed in a reviewable queue or explicit decision flow. The integration never silently overwrites local schedule decisions or staffing assignments. | Architecture schedule impact handling |
| `PTX-PUBLISH-001` | Planned | A supported local publication is emitted as durable outbox work with an idempotency key, remote response/version recording, retry policy, and a reconciliation fetch. | Architecture outbox and Pretalx design |
| `PTX-OPER-001` | Planned | Requests use configured timeouts, rate limits, retry/backoff with jitter, and bounded failure handling. Concurrent application instances must not duplicate the same scheduled run. | Architecture P1/P0 operational findings |
| `PTX-TEST-001` | Planned | Adapter contract tests use recorded or synthetic Pretalx responses for the supported API version, including pagination, rename, deletion, timezone, partial failure, retry, and republish. | Architecture phase 5 exit condition |

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
| `PTX-OPEN-001` | What is the field-level source-of-truth matrix for event metadata, rooms/capabilities, sessions/submissions, and room/time placement? | Before the first sync migration. |
| `PTX-OPEN-002` | Which Pretalx API version, endpoints, webhook support, and remote revision fields are guaranteed by the deployed organizer instances? | Before adapter implementation. |
| `PTX-OPEN-003` | Which remote deletions may be automatically mirrored, and which require organizer review because local published or staffing data exists? | Before destructive reconciliation. |
