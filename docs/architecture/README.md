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

Record material choices in an ADR before implementation. The current migration focus is identity and
shift; dispatch package design is intentionally deferred.
