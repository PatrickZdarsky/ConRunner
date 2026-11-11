# Legacy notification migration

The old notification service consumed `shiftcontrol.#` and mapped events to recipient queries,
messages, and links. In the modular monolith the corresponding behavior is split deliberately:
shift selects recipients and creates a notification request in the domain transaction;
communications owns preferences, templates, history, email work, and realtime delivery.

The current requests cover the legacy processor families: trade requests and outcomes, auctions,
assignment requests and outcomes, role and membership changes (including bulk operations), volunteer
lock/unlock/reset, plan joins, and trust alerts. Planner assignment requests and volunteer joins now
carry plan IDs so communications can restore the legacy plan-specific links; planner joins link to
the volunteer. Existing queued requests with only an event ID retain their event link. The old registration accidentally swapped
the unlock and reset processors; the current types follow the actual action.

Do not subscribe communications to the existing Rabbit events as a second notification source.
Those requests already exist and would duplicate deliveries, and the optional Rabbit publisher sends
after commit without a transactional outbox or stable event ID. A future event-driven replacement
would need durable publication, stable occurrence IDs, idempotent consumption, and an explicit
recipient contract before removing the transactional request path.

Versioned, bounded request context retains the relevant slot, role, and plan names for templates;
old unversioned queued requests fall back to generic text. The legacy service included volunteers'
first and last names; those are intentionally absent from the current identity model, so messages
refer to volunteers generically. In-app history supports listing, read state, marking all read,
deleting an item, and clearing the current user's history. The old push channel was SignalR plus
persisted history; the current equivalent is the realtime endpoint and persisted history.

The new web application is not yet implemented, so legacy-shaped links must be reconciled with its
eventual routes. Raw Rabbit payloads are not retained in notification requests.
