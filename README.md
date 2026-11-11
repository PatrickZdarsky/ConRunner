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
│   └── web/                 # Angular web application
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

For a local backend with PostgreSQL and Keycloak, see [local development](ops/compose/README.md).

The root Maven build aggregates the backend; the Angular application uses npm from `apps/web`.

## Local development

Prerequisites: Java 25, Node.js and npm, Python 3, curl, Docker and Docker Compose. From the repository root:

```sh
./dev client   # first setup and after backend HTTP API/DTO changes
./dev up       # start PostgreSQL, Keycloak, backend and Angular
```

Open http://localhost:4200 and sign in as `dev` / `dev`. The dev command installs frontend dependencies on first client generation. The Angular dev server reloads frontend changes and proxies `/api` (including WebSockets) to the backend on port 8081. Press Ctrl+C to stop the application processes; `./dev down` stops the infrastructure and keeps the database volume. The client generator uses a separate temporary PostgreSQL container and backend instance, so it does not modify the development database or rely on a running backend. It writes the OpenAPI artifact to `apps/backend/target/openapi.json` and the ignored TypeScript client to `apps/web/src/api-client`.

Run `npm run build` in `apps/web` to verify the frontend after generating the client. If a backend endpoint or DTO changes, rerun `./dev client`; frontend-only edits do not require regeneration. Keycloak realm import happens when its container is created; an existing Keycloak container must be recreated to pick up realm configuration changes. See [local backend development](ops/compose/README.md) for details.

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
