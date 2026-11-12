# ConRunner web

This Angular app provides the event, shift planning, and volunteer interfaces. It signs users in
through Keycloak and calls the ConRunner backend through the generated Angular HTTP client.

## Develop locally

Run `./dev up` from the [repository root](../../README.md#local-development) and open
[the web app](http://localhost:4200). If PostgreSQL, Keycloak, and the backend are already running,
use `npm start` from this directory. The Angular dev server forwards `/api` and WebSocket connections
to `http://localhost:8081` using [`proxy.conf.json`](proxy.conf.json).

Regenerate the client with `npm run generate-client` after backend endpoint or DTO changes. The
generator installs npm dependencies when needed, runs an isolated backend to export its OpenAPI
description, and writes the client to `src/api-client`. Make API changes in the backend and regenerate
the client so the generated code matches the API.

## Build and test

From this directory, after generating the client:

```sh
npm run build
npm run lint
npm run test-headless
```

The headless unit tests use Chrome. Use `npm test` for watch mode. To use the optional Cypress suite,
run `npx cypress install`, then `npm run cypress:open` or `npm run cypress:run`. See the
[Playwright guide](../../e2e/README.md) for the full-stack local acceptance test.

## Container build

From the repository root:

```sh
./dev client
docker build -t conrunner-web apps/web
```

The image serves the app on container port 8080. Its entrypoint injects these variables into
`env.js` when the container starts.

| Variable | Value |
| --- | --- |
| `KEYCLOAK_URL` | Browser-accessible Keycloak base URL |
| `KEYCLOAK_REALM` | Keycloak realm name |
| `KEYCLOAK_CLIENT_ID` | Keycloak client ID configured for the web app's origin |
| `API_BASE_PATH` | Browser-accessible API origin, with no `/api` suffix |

For same-origin hosting, set `API_BASE_PATH` to the public web origin and route `/api` to the backend
at the reverse proxy, including WebSocket upgrades. For a separate API origin, configure the web
origin in both backend properties `shiftcontrol.http.cors.allowed-origins` and
`shiftcontrol.platform.realtime.allowed-origins`. Both accept comma-separated origins.
