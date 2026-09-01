# Shift staffing requirements

Status: Implemented through the plan, membership, and staffing-shift foundation; later workflow slices remain planned
Owner: `at.control.shiftcontrol.shift`
Last reviewed: 2026-09-01

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
| Staffing mode | The plan's default volunteer-change policy: `SELF_SIGNUP`, `SUPERVISED`, or `LOCKED`. |
| Shift mode override | `INHERIT`, or an explicit staffing mode for one staffing shift. `INHERIT` uses the plan's mode; persistence may represent it as `null`, but APIs expose its meaning explicitly. |
| Staffing shift | A piece of volunteer work with a time range, an optional direct catalogue-location reference when it is not tied to a programme session, an optional Stage-1 catalogue-session reference (later a published-schedule-item reference), and position slots. When a session is present, its current catalogue location is the shift's location context. A staffing shift is distinct from a programme session. |
| Position slot | A named demand on a staffing shift, optionally requiring a plan role and a number of volunteers. |
| Assignment | A volunteer's accepted relation to a position slot. A pending join request is not an accepted assignment and does not reserve capacity; a pending leave request and an auction offer retain the current assignment until resolved. |
| Auction offer | An accepted assignment offered by its holder for atomic replacement. It remains occupied until a qualified claimant replaces the holder, the holder withdraws it, or a plan-state transition resolves it. |
| Trade | A proposed exchange between exactly two accepted assignments. It is not an auction and never involves a pending or auctioned assignment. |
| Global reward wallet | The one spendable points balance of a volunteer. Event, plan, slot, and assignment references attribute ledger entries but do not create separate wallets. |
| Award terms | Immutable point amount and calculation provenance created when an assignment is first accepted. Transfers preserve these terms even after reward-policy changes. |

## Membership, plans, and access

| ID | Status | Requirement | Legacy/responsibility source |
| --- | --- | --- | --- |
| `SH-VOL-001` | Implemented | A volunteer profile references exactly one identity UUID. The module creates or updates it only for staffing needs and does not copy OIDC issuer, subject, names, or global roles. | [Shift plan tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/plan/ShiftPlanServiceTest.java) |
| `SH-VOL-002` | Implemented | A volunteer's membership is unique per shift plan and records whether the user is a volunteer, planner, or both. A planner is also a volunteer of that plan. | [Plan invite and persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/plan/PlanInviteServiceTest.java) and [shift persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/ShiftPersistenceTest.java) |
| `SH-VOL-003` | Implemented | Staffing roles are scoped to the shift plan. A volunteer can hold roles in one plan without gaining them in another plan of the same event. | [Shift role tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/plan/ShiftRoleServiceTest.java) |
| `SH-PLAN-001` | Implemented | A shift plan belongs to one event and has a non-blank name unique within that event. It contains its own roles, staffing shifts, and staffing policy. Incentive configuration is deliberately deferred to `shift.reward`. | [Shift plan and persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/plan/ShiftPlanServiceTest.java) and [shift persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/ShiftPersistenceTest.java) |
| `SH-PLAN-002` | Implemented | A volunteer may read only plans they belong to. A planner may manage only plans they plan; a platform capability may grant a deliberate override. Every PII-bearing read and mutation applies this scope. | [Plan access tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/plan/PlanAccessServiceTest.java) |
| `SH-PLAN-003` | Implemented | A plan has a default staffing mode of `SELF_SIGNUP`, `SUPERVISED`, or `LOCKED`; new plans default to `SELF_SIGNUP`. The effective mode controls volunteer assignment changes and is not an authorization substitute for plan scope. | [Shift plan tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/plan/ShiftPlanServiceTest.java) |
| `SH-POLICY-001` | Implemented | A staffing shift has an `INHERIT` mode override by default, or one explicit staffing mode. Its effective mode is its explicit override when present, otherwise the plan's current default mode. An explicit override deliberately takes precedence over the plan mode for that shift. | [Staffing shift tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/positionslot/StaffingShiftServiceTest.java) |
| `SH-POLICY-002` | Planned | The volunteer actions permitted by each effective mode follow the staffing-mode matrix below. A volunteer may perform an action only when they have plan scope, are not individually restricted from volunteer changes, and satisfy the action's eligibility rules. | Product decision, legacy policy review |
| `SH-POLICY-003` | Planned | A scoped planner or platform administrator may directly assign, unassign, and accept or decline existing requests in every staffing mode, including `LOCKED`. Such intervention remains subject to authorization and validation; an override of an ordinary volunteer constraint requires an explicit reason and audit record. | Product decision, legacy planner behavior |
| `SH-POLICY-004` | Planned | A staffing-mode change is one atomic, auditable command. It records the actor, old and new modes, every automatically resolved workflow item, and the effective application transition policy. Required affected-volunteer notifications are requested durably after commit. | Product decision, audit/outbox baseline |
| `SH-POLICY-005` | Planned | Entering plan-level `LOCKED` cancels every open trade and closes every open auction in the plan. Closing an auction restores the offering volunteer's assignment to accepted; accepted assignments and pending join/leave requests otherwise remain unchanged for planner review. This cleanup applies even where an individual shift has an explicit mode override. | Product decision |
| `SH-POLICY-006` | Planned | Entering plan-level `SELF_SIGNUP` resolves current supervised workflows using application configuration evaluated for that command: join requests default to `ACCEPT_ALL_IF_VALID` (alternative `DECLINE_ALL`), leave requests default to `ACCEPT_ALL` (alternative `DECLINE_ALL`), and open auctions default to `UNASSIGN_OFFERER` (alternative `KEEP_OPEN`). Existing open trades remain open because trade is permitted in both modes. | Product decision |
| `SH-POLICY-007` | Planned | `ACCEPT_ALL_IF_VALID` accepts every pending join request only when all remain valid and capacity exists for all of them; otherwise the mode change fails without partial resolution. `KEEP_OPEN` retains an auction as an occupied assignment that can be claimed or canceled in `SELF_SIGNUP`; ordinary self-sign-up cannot bypass it. A later configuration change never changes a completed transition retroactively. | Product decision |
| `SH-PLAN-004` | Implemented | A planner or authorized administrator can create, revoke, list, and delete plan invites within their scope. Invite types distinguish volunteer and planner membership. | [Plan invite tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/plan/PlanInviteServiceTest.java) |
| `SH-PLAN-005` | Implemented | An invite may have expiry, a positive use limit, and plan-scoped roles to grant. Redeeming it is idempotent for an existing member, increments usage only for a new membership, and may extend eligible roles. | [Plan invite tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/plan/PlanInviteServiceTest.java) |
| `SH-PLAN-006` | WIP | Invite codes and calendar-share links are bearer capabilities: they are generated with sufficient entropy and never included in logs, audit payloads, events, or URL paths used for administrative deletion. By approved product decision, each invite's raw code is the stored redemption key and is returned only to an authorized plan planner so they can redisplay its invite link after creation; this deliberately increases the impact of a database compromise. The invite-code portion is implemented and covered by [plan invite tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/plan/PlanInviteServiceTest.java); calendar links remain with `SH-VIEW-004`. | Architecture P0, product decision 2026-09-01 |
| `SH-PLAN-007` | Planned | Leaving a plan follows the plan staffing policy and deterministically handles active assignments, requests, trades, and rewards. Leaving never silently leaves orphaned staffing state. | `V12`, legacy `leavePlan` |
| `SH-PLAN-008` | Planned | Plan deletion or archival has an explicit data-impact policy. It must preserve required audit and reward-ledger history and must not depend on JPA cascading across other modules. | `ADM1`, legacy cascade model |

## Staffing-mode matrix

`SH-POLICY-002` will be implemented using this matrix when the assignment workflow is added. An
explicit shift override already supplies the effective mode; the action rules and plan-level
transition cleanup remain deferred until that workflow exists.

| Volunteer action | `SELF_SIGNUP` | `SUPERVISED` | `LOCKED` |
| --- | --- | --- | --- |
| Directly join or leave a slot | Allowed | Not allowed | Not allowed |
| Submit or withdraw a join/leave request | Not allowed | Allowed | Not allowed |
| Create an auction | Not allowed | Allowed | Not allowed |
| Claim an auction | Only an auction retained by `KEEP_OPEN` | Allowed | Not allowed |
| Create or accept a trade | Allowed | Allowed | Not allowed |
| Decline/cancel a trade or cancel an auction | Allowed where such a workflow exists | Allowed | Not applicable: plan entry has already closed them |
| Planner/admin direct assignment or request decision | Allowed | Allowed | Allowed |

## Staffing shifts, roles, and slots

| ID | Status | Requirement | Legacy/responsibility source |
| --- | --- | --- | --- |
| `SH-SHIFT-001` | Implemented | A staffing shift has a non-blank name and a valid time range within its event. In Stage 1, an optional programme link references a session from the same event and never mutates its catalogue data. A later Stage 2 schedule item replaces that reference when live schedule management is introduced. | [Staffing shift tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/positionslot/StaffingShiftServiceTest.java) |
| `SH-SHIFT-002` | Implemented | A staffing shift may carry descriptions, an optional direct catalogue-location reference when no session is linked, and an optional Stage-1 session reference. A direct location must belong to the plan event. When a session is linked, its current catalogue location is used instead and a direct location is not allowed. Current programme location/time context remains catalogue-owned; future editable placement remains schedule-owned. Reward incentives remain deferred with `shift.reward`. | [Staffing shift and persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/positionslot/StaffingShiftServiceTest.java) and [shift persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/ShiftPersistenceTest.java) |
| `SH-SHIFT-003` | Implemented | An authorized planner can set, change, or clear a staffing shift's mode override. Its displayed effective mode is derived from that override and the plan mode. A shift override controls volunteer actions for that shift; it never removes planner/admin authority defined in `SH-POLICY-003`. | [Staffing shift tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/positionslot/StaffingShiftServiceTest.java) |
| `SH-ROLE-001` | Implemented | A plan role has a non-blank name unique within the plan, a description, and a self-assignability policy. Optional reward-rate data is introduced only by the deferred versioned `shift.reward` feature. Only plan-scoped planners can manage roles and assign or remove them from plan members. | [Shift role tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/plan/ShiftRoleServiceTest.java) |
| `SH-SLOT-001` | Implemented | A position slot belongs to one staffing shift and has a name unique within that shift, a desired volunteer count, optional required role, and an explicit auto-assignment exclusion flag. Its optional reward override is introduced only by the deferred versioned `shift.reward` feature. | [Position slot and persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/positionslot/PositionSlotServiceTest.java) and [shift persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/shift/ShiftPersistenceTest.java) |
| `SH-SLOT-002` | Planned | The desired volunteer count cannot be reduced below active accepted assignments. Deleting or materially changing a slot follows an explicit assignment-impact policy. | Legacy position-slot validation |

## Assignment, availability, trade, and attendance

| ID | Status | Requirement | Legacy/responsibility source |
| --- | --- | --- | --- |
| `SH-ASSIGN-001` | Planned | Every assignment decision is evaluated atomically against plan membership, individual volunteer restrictions, required role, effective shift mode, existing assignment state, slot capacity, overlapping accepted assignments, and applicable unavailability. The result exposes machine-readable reasons when volunteer sign-up is unavailable. | `V5`, `SP6`, legacy eligibility states |
| `SH-ASSIGN-002` | Planned | In effective `SELF_SIGNUP`, a volunteer may directly join or leave an eligible slot. Joining creates an accepted assignment only after all eligibility checks succeed and cannot overfill a slot under concurrent requests. | `V5`, legacy join flow |
| `SH-ASSIGN-003` | Planned | In effective `SUPERVISED`, a volunteer may submit and withdraw a join or leave request. A join request does not reserve capacity; a leave request retains its accepted assignment. Only a scoped planner/admin accepting the request changes the number of filled positions. Each transition is recorded and notifies the relevant people through communications. | `SP6`, `V7`, product decision |
| `SH-ASSIGN-004` | Planned | A scoped planner can directly assign or unassign eligible plan members. Any policy or constraint override must be explicit, authorized, recorded with a reason, and visible in the result. | `SP6`, legacy planner assignment |
| `SH-AVAIL-001` | Planned | A volunteer can declare event-scoped unavailability with a valid time range. It cannot overlap another incompatible constraint or an accepted assignment unless an explicit authorized override is recorded. | `V7`, legacy `TimeConstraintServiceImpl` |
| `SH-AVAIL-002` | Planned | Emergency unavailability is represented separately from ordinary unavailability and uses event-timezone whole-day boundaries. Its downstream notifications or reassignment workflow are durable work, not an in-transaction network call. | `SP16`, legacy emergency constraint |
| `SH-TRADE-001` | Planned | A trade is contained within one event and one shift plan and may be initiated only between two accepted assignments. Both volunteers must remain eligible, authorized, and in an effective mode other than `LOCKED`; pending requests and auction offers cannot be traded. | `V8`, `V11`, legacy trade service, product decision |
| `SH-TRADE-002` | Planned | A trade recipient explicitly accepts or declines an open trade. An inverse trade request never auto-executes a trade. Acceptance atomically revalidates both volunteers and swaps the assignments; it cancels related open trades deterministically. | `V8`, `V11`, legacy trade service, product decision |
| `SH-TRADE-003` | Planned | In effective `SUPERVISED`, a volunteer may offer one accepted assignment as an auction. The assignment remains occupied. An eligible plan member may atomically claim it without a second confirmation from the offering volunteer because the number of filled positions does not change. A carried-over `KEEP_OPEN` auction may also be claimed in `SELF_SIGNUP`; a new auction may not be created there. | `V14`, legacy auction service, product decision |
| `SH-TRADE-004` | Planned | Canceling an auction restores its offering assignment to accepted. An accepted trade, direct unassignment, assignment invalidation, or entry into plan-level `LOCKED` closes affected open trade/auction workflows deterministically and requests affected-volunteer notifications. | Product decision, legacy cleanup behavior |
| `SH-ATTEND-001` | WIP | A scoped planner records attendance as present, late, or absent after a staffing shift. Attendance is immutable history with a controlled correction path and is available to statistics and reliability calculations. This is out of scope for the current migration. | `SP17` |

## Volunteer views, planning support, and rewards

| ID | Status | Requirement | Legacy/responsibility source |
| --- | --- | --- | --- |
| `SH-VIEW-001` | Planned | A volunteer can view their current and upcoming assignments, including event, plan, time, staffing role, location/schedule context when available, and assignment state. | `V1`, `V2`, legacy dashboard/calendar |
| `SH-VIEW-002` | Planned | A volunteer can search their permitted plan schedule by date, role, location, tag, and availability. A full plan schedule is shown only when the plan policy grants that visibility; unpublished programme data is never treated as public. | `V2XL`, `V3`, `V4`, `V6` |
| `SH-VIEW-003` | Planned | Hours are calculated per volunteer, day, event, and plan. The calculation declares whether it uses assignments, attendance, or both; anonymous fairness views expose aggregates only. | `V9`, `V10`, `SP12`, legacy leaderboard |
| `SH-VIEW-004` | Planned | A volunteer can issue and revoke a read-only calendar-feed capability for their own assignments. The resulting feed contains only that volunteer's authorized staffing schedule and is never discoverable through a general user lookup. | Legacy volunteer calendar service |
| `SH-PREF-001` | Planned | A plan member can store a preference from -10 through +10 for an eligible position slot. Preferences influence suggestions only; they never override mandatory eligibility or capacity rules. | legacy preference service, auto-assignment backlog |
| `SH-AUTO-001` | Planned | Auto-assignment produces explainable suggestions and does not silently commit assignments. It considers hard eligibility, availability, conflicts, workload, required roles, and opt-out slots; preferences and incentives are configurable soft inputs. Reliability is deferred. | `SP11`, `S1`, `S2` |
| `SH-REWARD-001` | Planned | Points are maintained in an immutable per-volunteer ledger. A volunteer has one globally spendable wallet; event, optional plan/slot/assignment references are attribution for audit and reporting, not separate balances. Every ledger entry has a durable source identity enforced as unique by the database, so earnings, reversals, and manual adjustments are idempotent. In the initial scope, points are earned when an assignment is accepted; attendance does not alter them. | `V15`, `SP18`, legacy reward ledger; ADR-0001 |
| `SH-REWARD-002` | Planned | Expected points are shown before a volunteer-driven assignment acceptance as a quote with a visible reward-policy revision and calculation breakdown. If that revision changes, the volunteer cannot accept based on the stale estimate and receives the current quote to review. | `V5`, legacy reward configuration hash; ADR-0001 |
| `SH-REWARD-003` | Planned | Authorized planners configure versioned reward rules and event/plan applicability. Volunteers see applicable rewards, can redeem only with sufficient global available points, and receive an immutable redemption history and remaining balance. | `V16`, `V17`, `SP18`, `SP21`; ADR-0001 |
| `SH-REWARD-004` | Planned | A redeemed benefit is applied through an explicit, auditable rule—for example priority self-assignment—and never by mutating historic ledger entries. The benefit grant is distinct from the redemption ledger entry. | `S2`; ADR-0001 |
| `SH-REWARD-005` | Planned | Initial acceptance creates immutable award terms containing the awarded amount, policy/calculation provenance, and originating accepted assignment. A later rule, role-rate, shift-incentive, slot-override, or time change never alters them. | Product decision; ADR-0001 |
| `SH-REWARD-006` | Planned | Unassignment reverses the applicable prior earning entry. An auction claim or accepted trade atomically reverses the departing volunteer's earning and grants the incoming volunteer the exact same stored award terms; it never re-evaluates the current reward policy. | Product decision; ADR-0001 |
| `SH-REWARD-007` | Planned | Reward reports are available only to a planner authorized for the relevant scope or a platform administrator, and are handled as authorized data access. The target system has no public reward report, reward-share bearer capability, or raw reward-share token in a URL, log, audit record, or event payload. | Product decision; ADR-0001 |
| `SH-REPORT-001` | Planned | Scoped planners can export plan staffing data in CSV and XLSX. Exports honor supported filters, use authorized data only, and are recorded as data-access events when required by audit policy. | `SP19`, legacy event/reward exports |

### Deferred reward implementation guardrails

Rewards are intentionally not part of the current feature slice. Until the reward feature is
implemented, current shift work must retain the constraints in ADR-0001: do not add mutable point
balances to volunteer, event, plan, slot, or assignment state; keep assignment ownership changes as
explicit application use cases; retain stable UUID references; and do not introduce public or
bearer-link reward reporting. These are design constraints, not authorization to add reward
runtime behavior early.

## Legacy compatibility assessment — foundation stages

The legacy `ShiftPlanIT`, `PositionSlotIT`, role-service, and planner-slot tests were reviewed
while implementing the first three stages. The following behaviours are retained in the new domain
model: a newly created plan has a default staffing mode; planner membership also grants volunteer
membership; roles are plan-scoped; self-assignable roles may be self-managed; volunteer and planner
invites can grant roles; a planner invite upgrades an existing volunteer; slots are scoped to one
shift and may require a role; and shift/slot names retain their local uniqueness rules.

The following are intentional, documented corrections rather than accidental incompatibilities:

- Invite codes are random bearer capabilities retained as the redeemable value and returned only
  in planner-authorized invite management so a planner can redisplay the invite link. This is a
  deliberate reduction in database-compromise resistance, not an accidental leak.
- All shift-plan, role, invite, shift, and slot service operations enforce plan membership or
  planner scope, with a deliberate platform-administrator override. This closes legacy paths that
  relied on scattered or absent authorization checks.
- A staffing shift must be within its event and an optional direct location must be from that event.
  One legacy integration fixture persisted a January shift under a February event directly through
  the repository; it did not establish a service-level contract and is rejected by the new required
  validation.
- The new shift may be standalone at a catalogue location. If it is linked to a catalogue session,
  that session's current location is authoritative; the schema and command validation prohibit a
  conflicting direct location.
- Legacy role reward rates, plan default rates, shift bonuses, and slot overrides are intentionally
  not represented. ADR-0001 requires a later versioned reward policy and immutable award terms, so
  preserving those mutable fields here would make the future implementation less safe.

Legacy assignment, request, auction, trade, attendance, notification-event, and reward assertions
are not implemented in these stages. Their target behaviour remains planned in the requirements
above; this foundation avoids an entity graph or cascade design that would constrain those later
workflows.

## Open questions

| ID | Question                                                                                                                                                                                                                                                                                                                                      | Decision needed by |
| --- |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| --- |
| `SH-OPEN-004` | Reliability means a non-public indicator intended to help planners judge likely attendance and later inform suggestions. Its formula, visibility, retention, and correction process are not designed yet. | WIP and out of scope for the current migration. |
