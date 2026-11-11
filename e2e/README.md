# End-to-end tests

Playwright runs full-stack acceptance tests against Angular, Spring Boot, PostgreSQL, and Keycloak.
Start the local development stack from the repository root with `./dev up`. Once it is ready, run:

```sh
cd e2e
npm ci
npx playwright install chromium
npm test
```

The admin journey signs in with the local Keycloak development account. Each run creates a
uniquely named event and leaves the event, its shift plan, and its role in the local database.
To watch the journey in Chromium, run `npm test -- --headed`.
