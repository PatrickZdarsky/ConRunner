# ShiftControl web

The Angular application uses the HTTP API of `apps/backend`. See the [root development guide](../../README.md#local-development) for setup and commands.

The generated client in `src/api-client` is ignored by Git. Run `../../dev client` after changing backend HTTP endpoints or DTOs; it generates the OpenAPI description from this checkout using an isolated temporary database and the pinned OpenAPI Generator version. Do not edit generated files.

Run `npm run build` from this directory after generating the client. The local dev server proxies `/api` and WebSocket requests to the backend. Pretalx synchronization is not available until its backend integration is implemented.

For a container build, first run `./dev client` from the repository root, then `docker build -t shiftcontrol-web apps/web`. Supply `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, and `API_BASE_PATH` at runtime. `API_BASE_PATH` is the browser-accessible backend origin; configure a reverse proxy for `/api` (including WebSocket upgrades) or allow the web origin through the backend properties `shiftcontrol.http.cors.allowed-origins` and `shiftcontrol.platform.realtime.allowed-origins`. Both accept comma-separated origins.

Cypress is optional during setup; run `npx cypress install` in this directory before using its interactive or end-to-end commands.
