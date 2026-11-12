# ConRunner

ConRunner helps convention and event teams organize volunteer staffing. Administrators set up events,
planners manage shift plans and staffing roles, and volunteers join plans and sign up for shifts.
The application also handles assignment requests and trades, volunteer rewards, in-app
notifications, and audit history.

The repository contains an Angular web app and a single Java backend built with Spring Boot and
Spring Modulith. PostgreSQL stores application data, and Keycloak provides sign-in for local
development.

## Local development

Install JDK 25, Node.js and npm (CI uses Node.js 24), Python 3, curl, and Docker with the Compose plugin.
The development scripts use Bash and `setsid`. From the repository root, run:

```sh
./dev up
```

Open [the web app](http://localhost:4200) and sign in as `dev` with password `dev`. This local account
has platform administrator access. To try the setup workflow, create an event, add a shift plan,
then open the plan's **Roles** tab and create a volunteer role.

On first run, `./dev up` installs the web dependencies and generates the Angular API client if
needed. It then starts PostgreSQL and Keycloak in Docker and runs the backend and Angular dev server
on the host. Angular forwards `/api` requests, including WebSocket connections, to port 8081.

Press Ctrl+C to stop the web and backend processes. Run `./dev down` to stop the containers while
keeping the PostgreSQL volume. See the [Compose guide](ops/compose/README.md) for backend-only
startup, local service details, and how to reload the Keycloak realm.

## Build and test

| Task | Command | Run from |
| --- | --- | --- |
| Run backend tests | `./mvnw test` | Repository root, with Docker available for Testcontainers |
| Generate the Angular API client | `./dev client` | Repository root |
| Build the web app | `npm run build` | `apps/web`, after generating the client |
| Lint the web app | `npm run lint` | `apps/web` |
| Run Angular unit tests | `npm run test-headless` | `apps/web`, with Chrome available |
| Run full-stack browser tests | [Playwright setup and commands](e2e/README.md) | `e2e`, with the local stack running |

Rerun `./dev client` when backend HTTP endpoints or DTOs change. It runs the backend against a
temporary PostgreSQL container, writes the OpenAPI description to `apps/backend/target/openapi.json`,
and regenerates the Git-ignored TypeScript client at `apps/web/src/api-client`.

## Repository guide

| Path | Contents |
| --- | --- |
| [`apps/backend`](apps/backend/README.md) | HTTP API, domain modules, identity, notifications, and audit |
| [`apps/web`](apps/web/README.md) | Angular development, generated client, and container configuration |
| [`e2e`](e2e/README.md) | Playwright tests against the full local stack |
| [`docs/architecture`](docs/architecture/README.md) | Module boundaries and backend implementation conventions |
| [`docs/requirements`](docs/requirements/README.md) | Product behavior, migration scope, and requirement status |
| [`docs/adr`](docs/adr/README.md) | Architecture decisions and their rationale |
| [`contracts`](contracts/README.md) | Home for versioned external contracts |
| [`ops`](ops/README.md) | Local infrastructure and deployment documentation |
