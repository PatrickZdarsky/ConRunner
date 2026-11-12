# ConRunner external contracts

This directory is reserved for versioned schemas consumed by systems outside ConRunner. Internal
backend modules exchange application APIs and domain events as described in the
[architecture guide](../docs/architecture/README.md).

For the current HTTP API description, run `./dev client` from the repository root. It exports
`apps/backend/target/openapi.json` from the running backend and generates the Angular client in
`apps/web/src/api-client`. See the [web guide](../apps/web/README.md) for the generation workflow.
