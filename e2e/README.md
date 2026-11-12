# ConRunner end-to-end tests

Playwright exercises ConRunner through the Angular UI, Spring Boot backend, PostgreSQL, and
Keycloak. The [admin setup test](tests/admin-event-setup.spec.ts) signs in as the local platform
admin, creates an event and shift plan, and verifies that a new volunteer role persists after a
page reload.

## Run locally

Start the [local stack](../README.md#local-development) with `./dev up` and keep it running. In a
second terminal, from the repository root:

```sh
cd e2e
npm ci
npx playwright install chromium
npm test
```

The [Playwright config](playwright.config.ts) targets `http://localhost:4200` and keeps traces for
failed tests. Run `npm test -- --headed` to watch the browser. Each run leaves its uniquely named
event, shift plan, and role in the local database.
