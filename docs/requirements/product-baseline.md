# ShiftControl product baseline and traceability

Status: Draft baseline for review
Last reviewed: 2026-09-01

## Purpose

This document turns the existing responsibility sheet into a usable draft product baseline without
preserving obsolete service boundaries. It covers the whole application: volunteer staffing,
planning, programme scheduling, integrations, notifications, audit, risk, dispatch, and the
cross-cutting platform rules needed to operate them safely.

The responsibility sheet remains a valuable catalogue of user outcomes. It is not the target data
model or ownership map. The current target ownership is the architecture review and the local project
context: `identity` owns authentication and the local user ID; `shift` owns volunteer staffing and
plan access; `eventcatalog` owns event, location, and session identity. The current schedule stage
provides only staffing-facing context; its later live-placement and publication capability, together
with `integrations.pretalx` synchronization, is WIP and out of scope for this migration.

## Source material

| Source | Role in this baseline |
| --- | --- |
| [Responsibilities sheet](../ASE%20QSE02%20Responsibilities%20-%20Google%20Sheets.pdf) | Product outcomes and acceptance intent. |
| `ARCHITECTURE_REVIEW_AND_EXPANSION_PLAN.md` in the sibling legacy workspace | Target ownership, operational constraints, and migration direction. |
| `PROJECT_CONTEXT.md` in this workspace | Current decisions made during this migration; local-only working context. |
| `/home/patrick/Projects/ShiftControl-Multimodule/shiftcontrol-java-backend/shiftservice/` | Legacy behavior and tests to preserve or consciously change. |
| Current `apps/backend` code and tests | Evidence for requirements marked **Implemented**. |

## Responsibility-sheet coverage

| Responsibility-sheet area | Target requirement area | Treatment |
| --- | --- | --- |
| `AU1`, authentication/security follow-ups, application user views | [Identity](identity.md), [Shift](shift.md), [Platform](platform.md) | Local OIDC identity is retained; plan access is shift-owned. |
| `ADM1`, `ADM2`, `ADM6`, location/activity CRUD, import/export | [Event catalogue](eventcatalog.md), [Schedule](schedule.md), [Shift](shift.md), [Platform](platform.md) | Split by lifecycle instead of retaining the legacy `Event` aggregate. |
| `V1`–`V17` volunteer dashboard, schedule, sign-up, availability, trades, hours, points, rewards | [Shift](shift.md), [Communications](communications.md) | Staffing behavior remains in `shift`; delivery is communications-owned. |
| `SP1`–`SP21` planning, plans, roles, slots, assignments, attendance, exports, incentives | [Shift](shift.md), [Schedule](schedule.md), [Communications](communications.md) | Planner workflows are split between staffing and programme placement. |
| `S1`, `S2`, leaderboard | [Shift](shift.md) | Reliability and incentive inputs are staffing concepts; scoring details remain open. |
| `S3`, Pretalx sync, activity/location import | [Pretalx integration](integrations-pretalx.md), [Event catalogue](eventcatalog.md), [Schedule](schedule.md) | WIP/out of scope. Stable external mappings and explicit reconciliation are preserved as later constraints. |
| `N1`–`N6`, notification settings/history/email/push/realtime | [Communications](communications.md), [Platform](platform.md) | Durable outbox delivery replaces direct RabbitMQ publication. |
| `A1`–`A3`, simple audit service | [Audit](audit.md), [Platform](platform.md) | Atomic sanitized records replace catch-all raw-event persistence. |
| Trust alerts and volunteer lock/reset ideas | [Risk](risk.md), [Shift](shift.md) | WIP/out of scope. Risk detects and records; staffing applies approved volunteer actions when later introduced. |
| Future dispatch | [Dispatch](dispatch.md) | WIP/out of scope. No detailed domain requirements were found; discovery is a prerequisite. |
| Endpoint, error handling, tests, CI, OpenAPI, UI guards, data setup, operations | [Platform](platform.md) | These are application-quality requirements, not ownership of a business aggregate. |

`MR`, setup, mock-up, cleanup, and presentation items in the sheet are delivery tasks rather than
product requirements. They are represented only where they establish a durable application
constraint, for example testability, API errors, security, or operations.

## Intentional changes from the legacy model

| Legacy approach | Target requirement |
| --- | --- |
| One shared `Event` aggregate cascades to locations, activities, and shift plans. | Event catalogue, schedule, and shift own separate lifecycles and exchange IDs or named application values. |
| `Location` is a generic event child and `Activity` stores editable time/location placement. | A `Location` and a `Session` are catalogue identities. Stage 1 retains current catalogue placement as schedule context for staffing. A later WIP schedule stage owns live editable placement and immutable published versions. |
| User identity is a provider subject/string and `Volunteer` is used as a general person record. | `identity` exposes a local UUID. `shift` creates a volunteer staffing profile and plan memberships only when needed; it does not own external identity mappings. |
| Rooms and sessions are found during Pretalx sync by names/titles. | The deferred Pretalx integration will use stable provider, instance, event, object, and external identifiers; rename and deletion must be reconcilable. |
| RabbitMQ events are sent after commit and consumers independently infer recipients or persist raw payloads. | State-changing commands create sanitized audit records and durable outbox work in the same transaction. Communications receives deliberate recipient/delivery work. |
| Pretalx API keys and reward-share tokens are exposed or persisted as ordinary values. | Integration credentials are encrypted and masked; capability tokens are one-way hashed and never appear in URLs, logs, audit records, or event payloads. Reward reports use scoped authorization rather than a reward-share capability. |
| Legacy `LockStatus` behavior is spread through assignment services and leaves stale auction/trade edge cases. | `shift` defines a plan staffing-mode matrix, explicit per-shift overrides, atomic transition cleanup, and application-configured transition resolution. Trades require two accepted assignments in one plan and never auto-execute an inverse request. |
| Dispatch has no detailed domain model in the reviewed code. | Dispatch is WIP/out of scope and starts only after domain discovery, with idempotency, concurrency control, and an append-only operational timeline as non-negotiable constraints. |

## Legacy evidence consulted

The review covered the legacy `shiftservice` endpoints, services, DAOs/repositories, and unit/
repository tests, plus entities in its sibling `shiftcontrol-lib`. Important behavior sources include
`ShiftPlanServiceImpl`, `ShiftServiceImpl`, `PositionSlotServiceImpl`,
`PlannerPositionSlotServiceImpl`, `AssignmentServiceImpl`,
`AssignmentSwitchRequestServiceImpl`, `TimeConstraintServiceImpl`, reward-points services,
`PretalxSyncService`, notification services, and their corresponding tests. Requirements below cite
legacy behavior only as a migration reference; they do not grant legacy source precedence.
