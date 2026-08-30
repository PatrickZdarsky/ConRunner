# Platform requirements

Status: Cross-cutting planned target
Owner: `at.control.shiftcontrol.platform` with the owning domain module
Last reviewed: 2026-08-30

## Purpose and scope

Platform requirements apply across the modular monolith. They define the application guarantees
that individual business modules must use: authorization, API behavior, durable side effects,
privacy, testing, observability, and recoverable operations. `platform` is not a dumping ground
for business entities; the module that owns a state change remains accountable for applying these
requirements.

## Functional and operational requirements

| ID | Status | Requirement | Source |
| --- | --- | --- | --- |
| `PLT-ID-001` | Planned | New domain identities exposed across modules and APIs use stable UUIDs. Compatibility adapters may translate legacy numeric IDs during migration but must not leak internal entities. | Architecture ownership, legacy mixed-ID issue |
| `PLT-AUTHZ-001` | Planned | Every protected mutation and PII-bearing read is authorized at the application-service boundary using explicit capabilities plus event/plan scope where applicable. Endpoint annotations are defense in depth, not the only control. | Architecture P0, security backlog |
| `PLT-AUTHZ-002` | Planned | The project maintains an authorization matrix and negative tests covering anonymous users, volunteers, planners, platform administrators, and each transitional machine or worker capability. | Architecture P0 |
| `PLT-AUTHZ-003` | Planned | The initial human access model has three roles: a volunteer with access to joined plans, a planner with management access to planned plans, and a platform administrator with global authority. Future module-specific capabilities are added only when their use case exists. | Product decision |
| `PLT-API-001` | Planned | HTTP adapters use API-specific request/response types and map module validation, missing-resource, conflict, and authorization failures through a global `@RestControllerAdvice` to consistent RFC 9457 `ProblemDetail` responses. Internal application types and exception messages are not HTTP contracts. | Exception-handling decision, legacy error backlog |
| `PLT-API-002` | Planned | HTTP routes are versioned and documented from checked-in source. Generated clients, if used, are generated from the same commit rather than a mutable external latest release. | Legacy OpenAPI/client workflow, architecture P1 |
| `PLT-SIDE-001` | Planned | A state-changing command that requires an audit record or external side effect appends the domain change, sanitized audit record, and outbox work in one transaction. No required event is published only after commit without durable recovery state. | Architecture P0 |
| `PLT-SIDE-002` | Planned | Outbox workers claim work safely across replicas, use retry/backoff and idempotency, record attempts and terminal failures, and never include secrets in payloads or logs. | Architecture P0 |
| `PLT-DATA-001` | Planned | Module-owned database migrations are the only schema-evolution mechanism. Modules do not access another module's repositories/entities; cross-module references use stable IDs and named APIs. | Architecture P1, code-structure guide |
| `PLT-PRIV-001` | Planned | Credentials, bearer capabilities, personal data, and operational data are classified. Secrets are excluded from URLs, logs, trace attributes, responses, events, and audit payloads; recoverable credentials are encrypted and one-way tokens are hashed. | Architecture P0 |
| `PLT-PRIV-002` | WIP | Personal-data deletion/anonymization for staffing data is a later, low-priority capability. It is not part of the current migration. | Product decision |
| `PLT-TIME-001` | Planned | Instants are stored in UTC. Event-local presentation and day-based rules use an explicit event timezone, never the application server's default timezone. | Architecture schedule/Pretalx design |
| `PLT-QUALITY-001` | Planned | Every migrated behavior receives focused unit/integration coverage. Current golden end-to-end journeys cover login, event/plan/shift setup, join/leave, assignment/trade, notification history, and audit access. A Pretalx journey is added when that WIP integration enters scope. | Architecture phase 0 |
| `PLT-OPER-001` | Planned | Releases are reproducible from versioned artifacts and checked-in configuration. The application exposes meaningful readiness/liveness signals and uses structured, secret-safe logs with correlation identifiers. | Architecture P0/P1 |
| `PLT-OPER-002` | Planned | Production data has documented encrypted backup, restore, upgrade, and rollback procedures, exercised before schedule or dispatch operational data is relied upon. | Architecture P0 |
| `PLT-REALTIME-001` | Planned | WebSockets are the primary future realtime transport. Reconnection, authorization, and delivery/recovery behavior remain to be designed with the first realtime use case. | Product decision |

## Interaction rules

- A module may publish a domain fact for in-process consumers, but domain events, audit records, and
  outbox jobs are distinct concepts with distinct retention and delivery rules.
- The current backend deliberately has no HTTP error advice until it exposes its first REST endpoint.
  `PLT-API-001` becomes implementation work with that endpoint; it is not permission to add an
  unused global web layer today.
- External-service authorization is transitional. Once a consumer moves into this backend, it uses a
  named application interface rather than service-to-service HTTP or broad machine credentials.

## Open questions

| ID             | Question                                                                                                                                                                                                                                                                  | Decision needed by                                 |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| `PLT-OPEN-001` | Which additional module-specific capabilities are needed beyond the initial volunteer, planner, and platform-admin roles? | When the first such module use case is implemented. |
| `PLT-OPEN-002` | What deletion/anonymization rules apply once the deferred staffing-data deletion capability is designed? | Before deletion is offered. |
| `PLT-OPEN-003` | What WebSocket authorization, reconnection, deployment, and external-worker design is justified by the first realtime use case? | Before realtime implementation. |
