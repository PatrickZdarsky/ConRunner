# ConRunner architecture

ConRunner uses a Spring Boot modular monolith and an Angular web client. Backend modules live
under `at.conrunner`, share a deployment and PostgreSQL instance, and own their workflows,
persistence, and schema changes. The [backend guide](../../apps/backend/README.md#application-modules)
lists the modules in the current application.

## Module boundaries

- Use stable identifiers and immutable values in module contracts. Keep JPA entities, repositories,
  DAOs, HTTP DTOs, and Specifications inside their owning module.
- Use a named `api` interface for cross-module calls that need an immediate result. Use a named
  `events` interface for asynchronous reactions to completed business actions.
- Use identity's local UUID to reference a user. The event catalogue owns events, locations, and
  programme sessions. The shift module owns volunteers, shift plans, and assignments.
- Keep shared technical infrastructure in `platform`. External adapters own their transport
  models and mappings.

[Backend code structure and module interaction](code-structure.md) covers feature packages,
persistence, named interfaces, and event publication. For shift behavior, follow
[ADR-0002](../adr/0002-compatibility-first-shift-migration.md), which uses legacy workflows and
tests as the compatibility baseline and records approved changes.

Use the [requirements index](../requirements/README.md) for expected module behavior, the
[decision records](../adr/README.md) for architectural choices, and the
[notification migration notes](legacy-notifications.md) for details of the previous delivery path.
