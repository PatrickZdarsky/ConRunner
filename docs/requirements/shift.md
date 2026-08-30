# Shift staffing requirements

Status: Planned migration target
Owner: `at.control.shiftcontrol.shift`
Last reviewed: 2026-08-30

## Purpose and scope

The shift module manages the staffing side of an event: volunteer profiles, plan access,
shift plans, staffing shifts, roles, position slots, availability, assignments, trades, attendance,
statistics, and rewards. It references identity user UUIDs and event/schedule IDs; it does not own
authentication, programme placement, Pretalx synchronization, notification delivery, or dispatch
state.

The legacy `shiftservice` contains the source behavior for most of this area. Its numeric IDs,
cross-aggregate JPA relationships, provider-backed user records, and scattered authorization are
not target requirements.

## Vocabulary

| Term | Meaning |
| --- | --- |
| Volunteer | The staffing profile for one identity user UUID. It exists to participate in plans; it is not a second identity system. |
| Shift plan | A staffing workspace for one event, with members, planners, roles, staffing policy, shifts, and incentives. |
| Staffing shift | A piece of volunteer work with a time range, optional published schedule-item reference, and position slots. It is distinct from a programme session. |
| Position slot | A named demand on a staffing shift, optionally requiring a plan role and a number of volunteers. |
| Assignment | A volunteer's relation to a position slot and its state. |

## Membership, plans, and access

| ID | Status | Requirement | Legacy/responsibility source |
| --- | --- | --- | --- |
| `SH-VOL-001` | Planned | A volunteer profile references exactly one identity UUID. The module creates or updates it only for staffing needs and does not copy OIDC issuer, subject, names, or global roles. | `AU1`, legacy `Volunteer` |
| `SH-VOL-002` | Planned | A volunteer's membership is unique per shift plan and records whether the user is a volunteer, planner, or both. A planner is also a volunteer of that plan. | `V12`, `SP1`, legacy invite join behavior |
| `SH-VOL-003` | Planned | Staffing roles are scoped to the shift plan. A volunteer can hold roles in one plan without gaining them in another plan of the same event. | `SP10`, `ADM3`, legacy `Role` |
| `SH-PLAN-001` | Planned | A shift plan belongs to one event and has a non-blank name unique within that event. It contains its own roles, staffing shifts, staffing policy, and incentive configuration. | `SP1`, `ADM1`, legacy `ShiftPlanServiceImpl` |
| `SH-PLAN-002` | Planned | A volunteer may read only plans they belong to. A planner may manage only plans they plan; a platform capability may grant a deliberate override. Every PII-bearing read and mutation applies this scope. | `SP1`, `V2`, `ADM5` |
| `SH-PLAN-003` | Planned | A plan has an explicit staffing policy at plan level. The policy controls volunteer self-assignment, volunteer withdrawal, supervised requests, trades, and auctions; individual-shift locks are additional restrictions. | `SP7`, `SP5`, legacy `LockStatus` |
| `SH-PLAN-004` | Planned | A planner or authorized administrator can create, revoke, list, and delete plan invites within their scope. Invite types distinguish volunteer and planner membership. | `SP13`, `ADM4`, legacy invite services |
| `SH-PLAN-005` | Planned | An invite may have expiry, a positive use limit, and plan-scoped roles to grant. Redeeming it is idempotent for an existing member, increments usage only for a new membership, and may extend eligible roles. | `V12`, legacy `ShiftPlanServiceImpl` |
| `SH-PLAN-006` | Planned | Invite codes and calendar-share links are bearer capabilities: they are generated with sufficient entropy, stored one-way hashed where recovery is unnecessary, shown only through an authorized issuance flow, and never included in logs, audit payloads, events, or URL paths used for administrative deletion. | Architecture P0, legacy invite/share tokens |
| `SH-PLAN-007` | Planned | Leaving a plan follows the plan staffing policy and deterministically handles active assignments, requests, trades, and rewards. Leaving never silently leaves orphaned staffing state. | `V12`, legacy `leavePlan` |
| `SH-PLAN-008` | Planned | Plan deletion or archival has an explicit data-impact policy. It must preserve required audit and reward-ledger history and must not depend on JPA cascading across other modules. | `ADM1`, legacy cascade model |

## Staffing shifts, roles, and slots

| ID | Status | Requirement | Legacy/responsibility source |
| --- | --- | --- | --- |
| `SH-SHIFT-001` | Planned | A staffing shift has a non-blank name and a valid time range within its event. If it is linked to the programme, it references a published schedule item rather than mutating catalogue sessions or schedule drafts. | `SP2`, `SP3`, legacy `ShiftServiceImpl` |
| `SH-SHIFT-002` | Planned | A staffing shift may carry descriptions, staffing-specific incentives, and a schedule-item reference. Programme room/time placement remains schedule-owned. | `SP2`, `SP3`, ownership baseline |
| `SH-SHIFT-003` | Planned | An authorized planner can lock an individual staffing shift. Its lock state is visible and prevents the defined volunteer and planner changes until an authorized unlock or documented override. | `SP5` |
| `SH-ROLE-001` | Planned | A plan role has a non-blank name unique within the plan, a description, a self-assignability policy, and optional reward-rate data. Only plan-scoped planners can manage roles and assign or remove them from plan members. | `SP4`, `SP10`, `ADM3`, legacy `RoleServiceImpl` |
| `SH-SLOT-001` | Planned | A position slot belongs to one staffing shift and has a name unique within that shift, a desired volunteer count, optional required role, optional reward override, and an explicit auto-assignment exclusion flag. | `SP4`, legacy `PositionSlotServiceImpl` |
| `SH-SLOT-002` | Planned | The desired volunteer count cannot be reduced below active accepted assignments. Deleting or materially changing a slot follows an explicit assignment-impact policy. | Legacy position-slot validation |

## Assignment, availability, trade, and attendance

| ID | Status | Requirement | Legacy/responsibility source |
| --- | --- | --- | --- |
| `SH-ASSIGN-001` | Planned | Every assignment is evaluated atomically against plan membership, required role, plan/shift policy, slot capacity, overlapping accepted assignments, and applicable unavailability. The result exposes machine-readable reasons when sign-up is unavailable. | `V5`, `SP6`, legacy eligibility states |
| `SH-ASSIGN-002` | Planned | Self-assignment is available only when the plan policy and slot allow it. It creates an accepted assignment only after all eligibility checks succeed and cannot overfill a slot under concurrent requests. | `V5`, legacy join flow |
| `SH-ASSIGN-003` | Planned | In supervised mode, volunteers can submit and withdraw assignment or unassignment requests. A scoped planner can accept or decline them; each transition is recorded and notifies the relevant people through communications. | `SP6`, `V7`, legacy request states |
| `SH-ASSIGN-004` | Planned | A scoped planner can directly assign or unassign eligible plan members. Any policy or constraint override must be explicit, authorized, recorded with a reason, and visible in the result. | `SP6`, legacy planner assignment |
| `SH-AVAIL-001` | Planned | A volunteer can declare event-scoped unavailability with a valid time range. It cannot overlap another incompatible constraint or an accepted assignment unless an explicit authorized override is recorded. | `V7`, legacy `TimeConstraintServiceImpl` |
| `SH-AVAIL-002` | Planned | Emergency unavailability is represented separately from ordinary unavailability and uses event-timezone whole-day boundaries. Its downstream notifications or reassignment workflow are durable work, not an in-transaction network call. | `SP16`, legacy emergency constraint |
| `SH-TRADE-001` | Planned | A trade or auction changes assignments atomically. All involved volunteers must remain eligible, authorized, and covered by the relevant plan policy; stale, canceled, or already-settled requests cannot be accepted again. | `V8`, `V11`, `V14`, legacy trade/auction services |
| `SH-TRADE-002` | Planned | A shift offered through an auction is reassigned without requiring a second confirmation from the offering volunteer once a valid claimant succeeds. Related open trades are closed deterministically. | `V14`, legacy auction claim |
| `SH-ATTEND-001` | Planned | A scoped planner records attendance as present, late, or absent after a staffing shift. Attendance is immutable history with a controlled correction path and is available to statistics and reliability calculations. | `SP17` |

## Volunteer views, planning support, and rewards

| ID | Status | Requirement | Legacy/responsibility source |
| --- | --- | --- | --- |
| `SH-VIEW-001` | Planned | A volunteer can view their current and upcoming assignments, including event, plan, time, staffing role, room/schedule context when available, and assignment state. | `V1`, `V2`, legacy dashboard/calendar |
| `SH-VIEW-002` | Planned | A volunteer can search their permitted plan schedule by date, role, room, tag, and availability. A full plan schedule is shown only when the plan policy grants that visibility; unpublished programme data is never treated as public. | `V2XL`, `V3`, `V4`, `V6` |
| `SH-VIEW-003` | Planned | Hours are calculated per volunteer, day, event, and plan. The calculation declares whether it uses assignments, attendance, or both; anonymous fairness views expose aggregates only. | `V9`, `V10`, `SP12`, legacy leaderboard |
| `SH-VIEW-004` | Planned | A volunteer can issue and revoke a read-only calendar-feed capability for their own assignments. The resulting feed contains only that volunteer's authorized staffing schedule and is never discoverable through a general user lookup. | Legacy volunteer calendar service |
| `SH-PREF-001` | Planned | A plan member can store a preference from -10 through +10 for an eligible position slot. Preferences influence suggestions only; they never override mandatory eligibility or capacity rules. | legacy preference service, auto-assignment backlog |
| `SH-AUTO-001` | Planned | Auto-assignment produces explainable suggestions and does not silently commit assignments. It considers hard eligibility, availability, conflicts, workload, required roles, and opt-out slots; reliability, preferences, and incentives are configurable soft inputs. | `SP11`, `S1`, `S2` |
| `SH-REWARD-001` | Planned | Points are maintained in an immutable per-volunteer ledger with event, optional plan/slot reference, type, source key, timestamp, and metadata. A source key makes earnings, reversals, and manual adjustments idempotent. | `V15`, `SP18`, legacy reward ledger |
| `SH-REWARD-002` | Planned | Expected points are shown before an assignment is accepted. If the underlying point configuration changes, the user cannot accept based on a stale estimate without reviewing the new value. | `V5`, legacy reward configuration hash |
| `SH-REWARD-003` | Planned | Authorized planners configure versioned reward rules and event/plan applicability. Volunteers see applicable rewards, can redeem only with sufficient available points, and receive an immutable redemption history and remaining balance. | `V16`, `V17`, `SP18`, `SP21` |
| `SH-REWARD-004` | Planned | A redeemed benefit is applied through an explicit, auditable rule—for example priority self-assignment—and never by mutating historic ledger entries. | `S2` |
| `SH-REPORT-001` | Planned | Scoped planners can export plan staffing data in CSV and XLSX. Exports honor supported filters, use authorized data only, and are recorded as data-access events when required by audit policy. | `SP19`, legacy event/reward exports |

## Open questions

| ID | Question                                                                                                                                                                                                                                                                                                                                      | Decision needed by |
| --- |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| --- |
| `SH-OPEN-001` | What is the exact transition matrix for `LOCKED`, `SELF_SIGNUP`, and `SUPERVISED`, especially for auctions, trades, planner overrides, and switching policy while work is in flight? => ignore for right now                                                                                                                                  | Before migrating assignment state. |
| `SH-OPEN-002` | May a trade cross shift plans in one event, or must it remain inside a plan? What cancellation policy applies when either plan changes policy? trades are definitely contained by event and also by plan, no cross plan trades.                                                                                                               | Before migrating trades. |
| `SH-OPEN-003` | When do points become earned: accepted assignment, completed attendance, or a configurable event rule? How do late/absent records affect reversals and reliability? accepted assignments for now. Tracking of attendance/marking unattended slots is not in scope for now                                                                     | Before migrating rewards or attendance. |
| `SH-OPEN-004` | What exact reliability formula, visibility, retention, and appeal/correction process is fair and lawful? What is meant by this?                                                                                                                                                                                                               | Before implementing reliability or automated allocation. |
| `SH-OPEN-005` | Which staffing shifts may exist without a published schedule item, and how are schedule changes approved when they affect assignments? Shifts are not required to have a schedule item associated. Also down the line a system to notify planners when a schedule change affects one of the shifts in their plan should be taken into account | Before schedule-to-shift integration. |
