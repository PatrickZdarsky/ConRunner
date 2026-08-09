# Architecture guide

ShiftControl is a modular monolith optimized for one maintainer. Deployment simplicity does not
remove domain ownership: the package modules are intended to be strong boundaries and possible
future extraction seams.

## Dependency rules

1. A module owns its persistence model, repositories, migrations, and state transitions.
2. Another module may depend only on deliberate API types in the owning module's root package.
3. JPA entities and repositories never cross module boundaries.
4. Domain modules do not depend on HTTP, JSON, or generated types from an external integration.
5. `platform` contains technical mechanisms only; it must not become a shared business model.
6. Cross-module cycles are not allowed.
7. Domain events, audit records, and durable outbox work are different concepts and will be modeled
   separately.

These rules should become automated Spring Modulith/architecture tests before migrated domain code
is accepted.

## Domain relationships

- `eventcatalog` provides stable event/session/room identities.
- `schedule` owns draft placement and immutable published versions.
- `workforce` may reference published schedule item identifiers but owns volunteer shifts and
  assignments.
- `dispatch` reads stable schedule/workforce identifiers through module APIs and owns its operational
  state and timeline.
- `integrations.pretalx` translates remote data into integration-neutral commands and values.
- `communications`, `audit`, and `risk` react through explicit APIs/events; they do not query another
  module's tables.

Record material choices in an ADR before implementation. Start with module boundaries, event/outbox
semantics, authorization, Pretalx field ownership, schedule versioning, and dispatch terminology.
