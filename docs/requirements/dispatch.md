# Dispatch requirements

Status: Discovery and constraints only
Owner: `at.control.shiftcontrol.dispatch`
Last reviewed: 2026-08-30

## Purpose and scope

Dispatch is the future operational-event module intended to replace L.A.S.S.I.E. It will own
operational cases/tasks, resource/responder state, acknowledgement, escalation, and the immutable
operational timeline. It may reference identity, event, published schedule, and shift IDs but must
never modify their tables or entities.

No detailed L.A.S.S.I.E. domain behavior was available in the reviewed source. The requirements
below are mandatory safety constraints, not a final operational data model.

## Functional requirements

| ID | Status | Requirement | Architecture source |
| --- | --- | --- | --- |
| `DSP-CMD-001` | Planned | Each dispatch command has a client-generated idempotency key. Retrying the same accepted command returns the original outcome and does not duplicate operational work. | Architecture dispatch constraints |
| `DSP-STATE-001` | Planned | Operational records use an explicit state machine; arbitrary status strings and undocumented transitions are rejected. | Architecture dispatch constraints |
| `DSP-STATE-002` | Planned | Commands that update current operational state use optimistic concurrency. A client that acts on a stale version receives a resolvable conflict rather than overwriting another operator's change. | Architecture dispatch constraints |
| `DSP-TIME-001` | Planned | Every accepted change appends an immutable timeline entry with actor, timestamp, reason, correlation ID, and resulting state. Current state remains queryable without replaying the timeline. | Architecture dispatch constraints |
| `DSP-REF-001` | Planned | References to people, events, schedule items, shifts, and assignments are stable IDs resolved through named module queries. Dispatch never imports or writes foreign JPA entities. | Architecture ownership baseline |
| `DSP-AUTH-001` | Planned | Dispatch authorization combines dispatch capabilities with event scope. A global platform administrator is not the only implicit dispatch authority. | Architecture P0 |
| `DSP-NOTIFY-001` | Planned | Escalations and notifications are durable outbox work with visible delivery state. A notification failure does not erase an accepted dispatch command. | Architecture dispatch constraints |
| `DSP-REALTIME-001` | Planned | Real-time dispatch views are based on committed state and can recover after reconnect. UTC is stored; the event timezone is used for display. | Architecture dispatch constraints |

## Discovery required before implementation

| ID | Status | Question |
| --- | --- | --- |
| `DSP-OPEN-001` | Open | Define the terms case, request, unit/resource, responder, assignment, status, escalation, and handover with L.A.S.S.I.E. operators. |
| `DSP-OPEN-002` | Open | Establish peak concurrency, acceptable outage, mobile/offline needs, data classification, retention, and legal obligations. |
| `DSP-OPEN-003` | Open | Decide which schedule and staffing facts dispatch needs live, through a shift query or a durable read-only snapshot. |
