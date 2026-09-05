# ADR-0002: Compatibility-first shift migration

- Status: accepted
- Date: 2026-09-05
- Owners: project maintainer

## Context

The legacy `shiftservice` has a cohesive and exercised workflow for plans, shifts, position slots,
assignments, time constraints, auctions, trades, and rewards. Its entity relationships, numeric
IDs, provider-backed users, and scattered authorization are not target architecture. Its observable
workflow and its service/integration tests are, however, the primary migration baseline.

The first shift foundation introduced a different assignment lifecycle: one persistent row per
`(position slot, volunteer)`, terminal workflow states, immutable assignment-transition rows, a
separate availability concept, and plan-mode transition rows. That is a redesign before the legacy
workflow has been ported. It makes auctions, trades, emergency absence, assignment deletion, and
future rewards harder to migrate correctly.

## Decision

### Compatibility baseline and approved improvements

For the shift migration, legacy behavior and tests are the default specification. A behavior may
depart from that baseline only when an explicit requirement or ADR records the reason and its test
expectation.

The following improvements are retained:

- UUID identities and module-owned persistence, with identity and event-catalogue data referenced
  through stable IDs rather than foreign JPA entities;
- centralized `PlanAccess` authorization based on the authenticated identity, plan membership,
  planner membership, and an explicit platform-administrator override;
- transactional assignment and plan-mode handling with a consistent plan, slot/shift, assignment,
  and volunteer lock order; time-constraint mutations serialize on the volunteer lock, so mode
  changes, capacity, overlapping work, and absence cannot race;
- direct shift locations and session-derived locations, where a session location is authoritative;
- redisplayable invite codes for authorized planners, as supported by the legacy feature; and
- the approved atomic policy for entering `SELF_SIGNUP`: accept all valid pending joins and complete
  pending leave requests, or fail the whole transition.

These improvements do not authorize a new domain split or a changed workflow unless separately
recorded.

### Cohesive shift implementation

`shift` remains one application module. It does not gain separately owned reward, trade,
availability, or assignment mini-modules. Package organization may keep directly related files
close together, but it must not obscure the established workflow or force cross-package orchestration
for ordinary shift operations.

The migration follows the proven service responsibilities from the legacy implementation:
`ShiftPlanService`, `ShiftService`, `PositionSlotService`, `PlannerPositionSlotService`,
`AssignmentService`, `EligibilityService`, `TimeConstraintService`, and
`AssignmentSwitchRequestService`. `PlanAccess` is a first-phase cross-cutting improvement shared by
these services.

The domain type is named `Shift`, not `StaffingShift`. “Staffing” describes the module context; it
does not belong in the core entity or public operation name.

### Assignment and audit model

An `Assignment` is a live relation and uses the legacy workflow states:

- `ACCEPTED`;
- `AUCTION`;
- `AUCTION_REQUEST_FOR_UNASSIGN`; and
- `REQUEST_FOR_ASSIGNMENT`.

Leaving a slot, withdrawing or declining a join request, and planner unassignment remove the live
assignment record. A later join creates a new assignment. Assignment history is captured by the
audit subsystem through semantic actions, not by terminal assignment states or a parallel
assignment-transition table.

Each state-changing command records an audit action with actor, subject, prior and resulting state
where applicable, reason/override where applicable, and the command outcome. A plan-mode change
records one plan action and individual assignment actions for every automatic resolution. The
current phase does not persist a `PlanModeTransition` entity.

### Time constraints and eligibility

The shift feature retains the legacy `TimeConstraint` concept with `UNAVAILABLE` and `EMERGENCY`
types. Emergency absence represents an unplanned convention-time absence, such as illness. It keeps
the legacy operational result: conflicting self-sign-up assignments are removed, while conflicting
supervised assignments require planner resolution through the assignment workflow. Audit records
the resulting actions; durable notifications remain separate follow-up work.

`PositionSignupState` remains the canonical compatibility result and workflow decision. A
machine-readable detail result may supplement it with all applicable causes, capacity information,
and effective mode; it must not replace or change legacy signup-state outcomes.

### Rewards

Reward work remains deferred, but it stays inside the cohesive `shift` module when resumed. It is
not extracted into a separate application module or mini-module. ADR-0001 continues to govern the
global wallet, immutable award terms, transfer preservation, and authorized reporting.

The current live-assignment deletion rule is intentional. Future reward/audit storage must retain
its own immutable source and attribution data; it must not depend on preserving a mutable live
assignment row or on cascade deletion.

### Test-first migration control

Before a legacy workflow is considered migrated, its legacy unit/integration behavior is ported or
recreated as an equivalent test in this repository. A migration matrix records every intentional
exception and points to the requirement or ADR that authorizes it. A green test suite for a newly
invented workflow is not sufficient evidence of compatibility.

## Consequences

- The existing terminal assignment states and assignment-transition persistence are temporary
  migration errors to remove before further assignment, auction, or trade work.
- `Availability` is not a replacement for `TimeConstraint`; ordinary and emergency absence are
  restored under the latter concept.
- `PlanModeTransition` persistence is removed, while the approved atomic transition policy and
  audit evidence remain.
- Future work keeps the UUID, authorization, location, and concurrency improvements without
  requiring a broad architectural rewrite.
- Migration documentation and tests must be updated before implementation changes so the same
  drift cannot recur.

## Alternatives considered

### Continue the redesigned lifecycle and adapt legacy features to it

Rejected. It creates a parallel workflow model before auctions, trades, emergency absence, and
rewards are migrated, increasing both implementation and compatibility risk.

### Copy the legacy persistence graph and authorization unchanged

Rejected. Cross-module JPA ownership, scattered authorization, and provider-backed user entities
are implementation debt rather than required behavior.

### Extract rewards now

Rejected. Rewards are deferred product work. Premature extraction would add another artificial
boundary while assignment and trade behavior is still being ported.

## Validation

The migration is on course only when tests demonstrate:

1. scoped `PlanAccess` for member, planner, and platform-administrator actions;
2. assignment deletion and recreation after leave, request withdrawal, decline, and unassignment;
3. legacy signup, auction, trade, and planner-assignment behavior, except documented changes;
4. ordinary and emergency time-constraint behavior and its audit actions;
5. atomic approved plan-mode transitions with both plan and assignment audit entries; and
6. future reward tests preserve award terms through auction claims and trades without a separate
   reward module.
