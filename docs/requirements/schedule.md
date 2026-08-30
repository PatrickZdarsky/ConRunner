# Programme scheduling requirements

Status: Stage 1 planned; Stage 2 WIP and out of scope
Owner: `at.control.shiftcontrol.schedule`
Last reviewed: 2026-08-30

## Purpose and scope

Schedule is deliberately staged. The current migration needs only enough programme context for
staffing shifts to refer to event sessions. It does not need a schedule editor, schedule versions,
or Pretalx synchronization.

Stage 1 therefore reuses the event catalogue's current event, room, and session data. The current
catalogue session time and room are the schedule context available to the rest of the application.
This is a migration bridge, not a commitment to make `eventcatalog` the long-term editor of a live
programme.

## Stage 1 — staffing schedule context

Stage 1 is the only schedule scope that may be implemented during the current staffing migration.

| ID | Status | Requirement |
| --- | --- | --- |
| `SCH1-REF-001` | Planned | A staffing shift may optionally reference a session from the same event. A staffing shift is valid without a session reference. |
| `SCH1-REF-002` | Planned | The optional session reference supplies programme context only. `shift` does not mutate the session, its room, or its time range. |
| `SCH1-READ-001` | Planned | Shift-facing reads can obtain the current session name, time range, and room for the referenced session through a small named catalogue read API or an equivalent immutable value. No catalogue entity or repository crosses the module boundary. |
| `SCH1-READ-002` | Planned | Volunteer and planner views show only schedule context the requester is already authorized to see through their event/plan access. |
| `SCH1-CHANGE-001` | Planned | Until Stage 2 begins, event-catalogue administration is the only local way to change current session/room/time data. There is no parallel schedule draft, local publication, or impact workflow. |

## Stage 2 — live schedule management and Pretalx integration

Status: WIP and out of scope. Do not begin this stage while migrating the shift module.

Stage 2 will add the ability to manage programme schedule data while an event is in progress. It is
intentionally deferred because the design is coupled to Pretalx source-of-truth rules and the
impact of a schedule change on staffing work.

| ID | Status | Future requirement |
| --- | --- | --- |
| `SCH2-DRAFT-001` | WIP | Authorized editors can manage schedule placement as drafts, with explicit event timezone, room, time, ordering, validation, and author/version information. |
| `SCH2-PUBLISH-001` | WIP | Publishing creates an immutable released version; later edits create another draft rather than mutating the released version. |
| `SCH2-IMPACT-001` | WIP | A live schedule change identifies affected staffing shifts and gives planners a deliberate notification/approval path. It never silently rewrites assignments. |
| `SCH2-PRETALX-001` | WIP | Schedule publication and reconciliation with Pretalx use the later Pretalx integration contract, stable external mappings, durable work, and a field-level source-of-truth decision. |

## Later decision

Before Stage 2, record an ADR that defines the source of truth for room/time placement, the live
change approval flow, hard/soft validation rules, and the Pretalx reconciliation behavior. Those
choices are not needed to migrate the first staffing slice.
