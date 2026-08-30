# Audit requirements

Status: Planned migration target
Owner: `at.control.shiftcontrol.audit`
Last reviewed: 2026-08-30

## Purpose and scope

Audit provides a durable, sanitized explanation of important state-changing activity. It owns audit
entries, their search/export behavior, and their retention/redaction policy. It is not a raw event
archive, a general application log, or a store for third-party payloads and credentials.

## Functional requirements

| ID                | Status | Requirement | Responsibility/architecture source |
|-------------------| --- | --- | --- |
| `AUD-RECORD-001`  | Planned | Every auditable successful state-changing command creates one sanitized audit entry in the same database transaction as its state change. Rejected commands may be recorded separately as security telemetry but never masquerade as completed changes. | `A1`, architecture P1 |
| `AUD-RECORD-002`  | Planned | An entry records timestamp, actor identity or explicit system actor, action, module, subject/aggregate reference, result, correlation/trace ID, and an approved before/after delta or reason. | `A1`, `A2`, architecture P1 |
| `AUD-RECORD-003`  | Planned | Audit entries are append-only. Corrections are represented by a later correction entry rather than changing or deleting historic entries. | `A1`, architecture audit design |
| `AUD-ACCESS-001`  | Planned | Only users with a dedicated audit-view capability can search or export audit entries. Search supports at least time range, actor, action, module, and subject; query indexes are designed for those reads. | `A3`, architecture P1 |
| `AUD-ACCESS-002`  | Planned | Audit exports apply the requester's scope and redaction policy, are bounded/paginated, and are themselves auditable data-access actions where required. | `A3` |
| `AUD-PRIVACY-001` | Planned | Audit payloads never contain credentials, bearer links, full notification bodies, raw integration payloads, or other prohibited secret material. Personal data is minimized and follows a defined retention/redaction policy. | Architecture P0/P1 |
| `AUD-OPER-001`    | Planned | Audit writes and queries are observable. A failed required audit write aborts the associated transaction rather than silently producing an unaudited state change. | Architecture audit/outbox design |

## Open questions

| ID | Question                                                                                                                                                                                                                                | Decision needed by |
| --- |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| --- |
| `AUD-OPEN-001` | Which state changes are legally/business-critical enough to be auditable, and which are ordinary operational telemetry? => Every change which affects more than the user himself (e.g. preferences) are critical enough to be auditable | Before auditing each migrated command. |
| `AUD-OPEN-002` | What retention, subject-access, export, legal hold, and deletion/redaction rules apply to audit data?  => choose a default retention of 2 years, configureable via app config and only applied on system startup                        | Before production retention is configured. |
