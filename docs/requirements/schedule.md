# Programme scheduling requirements

Status: Planned
Owner: `at.control.shiftcontrol.schedule`
Last reviewed: 2026-08-30

## Purpose and scope

Schedule manages the editable programme for an event: drafts, placement of catalogue sessions in
rooms and time, constraints, validation, publication, and impact analysis. It references stable
event-catalogue IDs and produces immutable published versions. It does not own programme metadata,
Pretalx HTTP calls, volunteer assignments, or dispatch cases.

The current event-catalogue session time and room fields are short-term migration compatibility.
They do not establish permanent catalogue ownership of editable placement.

## Functional requirements

| ID | Status | Requirement | Responsibility/architecture source |
| --- | --- | --- | --- |
| `SCH-CORE-001` | Planned | Each event has a schedule workspace with a named event timezone. Schedule instants are stored unambiguously and presented in that timezone. | Architecture schedule design |
| `SCH-CORE-002` | Planned | A schedule item references one catalogue session or submission by stable UUID and records placement for one schedule draft or version. It does not duplicate the catalogue entity as a mutable JPA relationship. | `SP3`, architecture ownership |
| `SCH-DRAFT-001` | Planned | Authorized schedule editors create and edit drafts derived from a known base version. Every edit records its actor, time, and base/version context. | Architecture schedule versioning |
| `SCH-DRAFT-002` | Planned | A draft can place, move, resize, order, or remove schedule items and associate them with event-catalogue rooms. The module rejects references outside the event. | `SP2`, `SP3`, legacy activity/location behavior |
| `SCH-VALIDATE-001` | Planned | Schedule validation distinguishes hard conflicts from warnings. Hard conflicts prevent publication; warnings are visible with a documented approval/override path. | `SP14`, `SP15`, architecture schedule design |
| `SCH-VALIDATE-002` | Planned | Validation covers at least invalid time ranges, rooms outside the event, overlapping room use, and invalid or missing catalogue references. Additional organizer constraints are typed and versioned rather than arbitrary executable expressions. | `SP14`, architecture schedule design |
| `SCH-PUBLISH-001` | Planned | Publishing creates an immutable version. Published versions are readable as the event's released programme; future edits create a new draft rather than modifying the published version. | Architecture schedule versioning |
| `SCH-PUBLISH-002` | Planned | Publication produces an impact report for linked staffing shifts, assignments, planned communications, and dispatch references. No import or publication silently rewrites operational data. Approved impacts are applied through explicit commands. | Architecture schedule impact handling |
| `SCH-PUBLISH-003` | Planned | A public or broad volunteer schedule view uses only a published version and only where the relevant plan/event policy permits it. Draft visibility is limited to authorized editors. | `V2XL`, `V3`, architecture schedule design |
| `SCH-VIEW-001` | Planned | Schedule reads support calendar and table views, filtering by date, room, session metadata, role-related staffing context where authorized, and stable room columns across a requested range. Rendering concerns do not change scheduling ownership. | `V3`, `V6`, legacy schedule endpoints |
| `SCH-STAFF-001` | Planned | Staffing shifts may reference a published schedule item. Schedule does not create, delete, or directly modify shift assignments; it asks `shift` for impact data through a named application query. | `SP3`, ownership baseline |

## Interaction rules

- `eventcatalog` owns the identity and descriptive metadata of events, rooms, and sessions.
- `integrations.pretalx` fetches and normalizes remote data. Schedule decides whether a proposed
  placement becomes part of a local draft and how publication is reconciled.
- `shift` references a published schedule item for staffing context. It may continue to support
  stand-alone staffing shifts only according to `SH-OPEN-005`.
- A schedule version may be referenced by dispatch, but dispatch never updates it.

## Open questions

| ID | Question | Decision needed by |
| --- | --- | --- |
| `SCH-OPEN-001` | Is ShiftControl or Pretalx canonical for room/time placement in the first release? The answer determines whether local edits are authoritative drafts or proposals. | Before schedule implementation. |
| `SCH-OPEN-002` | Which hard and soft constraints matter to actual organizers—speaker availability, setup buffers, track ordering, accessibility, equipment, or others? | Before validation rules are implemented. |
| `SCH-OPEN-003` | Which schedule version and impact-approval workflow is required for publication to Pretalx and for changes during a live event? | Before external publishing. |
