# ShiftControl

ShiftControl is being rebuilt as a monorepo containing a modular-monolith backend, one web client,
and the operational material needed to release them together. The backend currently contains the
identity foundation and architectural module scaffolding; `shiftservice` domain behavior is migrated
incrementally from the existing system.

## Repository layout

```text
.
├── apps/
│   ├── backend/             # one Spring Boot/Spring Modulith deployable
│   └── web/                 # future Angular application
├── contracts/               # contracts for genuinely external consumers only
├── docs/
│   ├── adr/                 # architecture decision records
│   ├── architecture/        # current architecture and module rules
│   └── requirements/        # module behavior and migration specifications
├── e2e/                     # full-stack acceptance journeys
├── ops/
│   ├── compose/             # local and small-production topology
│   ├── helm/                # Kubernetes packaging, if retained
│   └── runbooks/            # operational and recovery procedures
├── .mvn/                    # shared Maven wrapper configuration
└── pom.xml                  # repository build aggregator
```

## Build

Java 25 is the generated project's baseline.

```shell
./mvnw test
```

The root build aggregates the backend. Future applications should be added to the repository build
only when they have a real build configuration; placeholder directories are deliberately not fake
Maven modules.

## Architectural direction

- One backend process contains independently owned Spring Modulith application modules.
- A module exposes only deliberate named `api` or `events` interfaces; its root package is module
  metadata, not a dumping ground.
- A module owns its entities, repositories, migrations, and lifecycle.
- Cross-module calls use application APIs, stable identifiers, and immutable values—not another
  module's JPA entities or repositories.
- In-process domain events coordinate committed internal behavior. External side effects will use a
  transactional outbox when that infrastructure is introduced.
- Shift staffing, programme scheduling, and dispatch remain separate domain modules even though they share one
  deployable and database.
- External integrations live behind anti-corruption adapters; Pretalx models do not become core
  schedule models.

See [the backend structure](apps/backend/README.md) and
[the architecture guide](docs/architecture/README.md) before adding code. The detailed migration
convention is [Backend code structure and module interaction](docs/architecture/code-structure.md).
Module behavior and migration decisions live in [requirements specifications](docs/requirements/).
