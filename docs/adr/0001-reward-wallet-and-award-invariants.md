# ADR-0001: Reward wallet and assignment-award invariants

- Status: accepted
- Date: 2026-09-01
- Owners: project maintainer

## Context

The legacy reward-points feature combines rate calculation, assignment side effects, ledger
posting, reporting, public bearer-token sharing, and an external redemption URL. Its point
configuration is mutable across plans, roles, shifts, and slots, while assignment trades and
auctions have special reward arithmetic.

Rewards are deliberately deferred from the current migration. The surrounding shift model is
being implemented now, however, so it must not make a later safe, auditable reward implementation
unnecessarily difficult.

## Decision

### One global wallet

Each volunteer has exactly one globally spendable point balance. A reward-ledger entry records its
event and, when applicable, plan, slot, and assignment attribution for audit and reporting. Those
attributes do not create separate event or plan wallets and do not limit where the points may be
redeemed.

Future redemptions check and debit this global available balance atomically. A benefit grant is a
separate, explicit, auditable outcome; it never rewrites ledger history.

### Immutable award terms and transfers

An initial acceptance creates immutable award terms: the point amount, policy revision,
calculation version and breakdown, and the originating accepted assignment. Later changes to
reward rules, role rates, shift incentives, slot overrides, or time details do not change those
terms.

An unassignment reverses the relevant prior earning entry. An auction claim or accepted trade
reverses the departing volunteer's earning and grants the incoming volunteer the exact same stored
award terms. It does not calculate a new amount from the current configuration. This prevents a
volunteer from gaining points by moving an assignment back and forth after a rate increase.

Assignment ownership changes are explicit application use cases. They must not be implemented as
arbitrary entity/repository updates. Assignment identities and any future reward-origin references
must remain stable enough to connect an earning, reversal, and transfer audit trail.

### Future reward boundary

When the feature is implemented, `shift.reward` owns:

- versioned reward-policy configuration and reward quotes;
- append-only ledger posting and balance queries;
- immutable award terms and reversals; and
- reward redemption and benefit-grant records.

The assignment use case owns its transaction boundary and invokes the reward feature as part of the
same transaction. A ledger is the only source of balance changes; no user, event, plan, slot, or
assignment table is a mutable point-balance store.

Ledger entries use a durable operation/source identity that is enforced as unique by the database.
Reversals reference their original earning entry. Retrying a completed acceptance, removal, or
transfer must therefore be harmless. Reward policy evaluation returns a visible policy revision;
volunteer-driven acceptance supplies the revision it reviewed and receives a structured stale-quote
conflict when it no longer matches.

### Reporting and access

Reward reporting is an authorized read. Only a scoped planner or a platform administrator may
receive reward-report data, subject to normal audit policy. The legacy public reward-share endpoint
and reward-share bearer tokens are retired: the target system must not add a public reward report,
raw share-token storage, or a reward bearer token in a URL, log, audit record, or event payload.

The legacy event-level reward redemption URL is descriptive compatibility metadata only. It is not
a redemption contract and must not become the authorization or accounting mechanism for future
rewards.

### Current-stage constraints

This ADR does not authorize implementing reward tables, endpoints, exports, or redemption flows
in the current stage. Until that work begins, migration work must:

- use stable UUIDs for shift-owned identities and retain historical references without cross-module
  cascade deletion;
- centralize accepted-assignment, unassignment, auction, and trade transitions in application
  services;
- avoid a mutable point balance or a temporary point-accounting shortcut in existing aggregates;
- keep reward-related reporting out of public/bearer-link endpoints; and
- keep future reward configuration separate from generic, unversioned CRUD fields where its
  historic meaning would otherwise be lost.

## Consequences

- Event and plan point totals remain useful reports, but are not independently spendable balances.
- A transferred assignment deliberately may retain a historic lower or higher amount than a new
  assignment for the same slot would receive today.
- The future ledger needs an immutable award/origin reference, a unique idempotency key, and a
  reversal link; copying the legacy `source_key` column alone is insufficient.
- The future export implementation is a read-side concern and must not inherit from or mutate the
  reward writer.
- Existing generic event metadata can retain the legacy redemption URL temporarily, but no later
  reward implementation may depend on it.

## Alternatives considered

### Event- or plan-scoped wallets

Rejected. The product decision is that points are globally spendable; event and plan references are
for attribution and reporting only.

### Recalculate awards whenever an assignment is transferred

Rejected. It would allow rate changes to create a transfer incentive and would make the same
accepted work worth different points without a new original acceptance.

### Public reward reports protected by share links

Rejected. Reward reports contain sensitive volunteer and financial-like information. Normal scoped
authorization is clearer, auditable, and avoids bearer-capability handling.

## Validation

Future reward work is complete only when tests demonstrate that:

1. a volunteer can spend a balance earned across multiple events;
2. a policy change affects a new acceptance but not an existing assignment's transfer;
3. duplicate acceptance/removal/transfer operations create no duplicate ledger entries;
4. a reversal is traceable to its original earning entry and historic entries remain unchanged;
5. unauthorized users cannot obtain reward reports; and
6. a failed assignment transition cannot commit a ledger change, or vice versa.
