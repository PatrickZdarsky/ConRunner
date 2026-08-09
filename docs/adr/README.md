# Architecture decision records

Use four-digit sequential names such as `0001-modular-monolith.md`. Copy `0000-template.md`, describe
the actual decision and constraints, and keep superseded records rather than rewriting history.

Initial decisions to record:

1. Modular monolith and service extraction criteria
2. Repository, versioning, and generated-contract strategy
3. Domain events, audit, outbox, retries, and idempotency
4. Identity and capability model
5. Pretalx/schedule field ownership and synchronization direction
6. Schedule versioning and operational impact handling
7. Dispatch domain terminology, consistency, realtime, and offline needs
8. Personal-data retention and deletion
9. Production platform, backup/restore, and recovery objectives
