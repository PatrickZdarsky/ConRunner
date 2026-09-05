# Architecture guide

ShiftControl is a modular monolith optimized for one maintainer. Deployment simplicity does not
remove domain ownership: package modules are strong boundaries and possible future extraction seams.

The authoritative guide for laying out migrated backend code and integrating modules is
[Backend code structure and module interaction](code-structure.md). It defines the feature-first
package convention, DAO/repository responsibilities, internal Specifications, named module APIs,
and event publication rules.

At a high level:

- A module owns its persistence model, repositories, migrations, state transitions, and endpoints.
- JPA entities, repositories, DAOs, HTTP types, and Specifications never cross module boundaries.
- Cross-module interaction uses stable IDs, a small named `api` contract when an immediate answer is
  needed, or a named `events` contract for asynchronous facts.
- `identity` provides the stable authenticated user ID; `shift` owns volunteers, plan membership,
  plans, shifts, and staffing behavior.
- `platform` contains technical mechanisms only; it must not become a shared business model.

Record material choices in an ADR before implementation. Module behavior and compatibility choices
belong in [requirements specifications](../requirements/). The current migration focus is identity,
event catalogue, and shift; dispatch package design is intentionally deferred.

The shift migration follows [ADR-0002](../adr/0002-compatibility-first-shift-migration.md): legacy
workflow behavior and tests are the default baseline, while UUID ownership, `PlanAccess`, atomic
mutation handling, and other approved improvements remain in place.
