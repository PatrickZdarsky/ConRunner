# ShiftControl

## API client generation

The frontend uses generated TypeScript Angular clients for backend APIs.

### Shiftservice client

Generate the Shiftservice client with:

```bash
npm run generate-shiftservice-client
```

This writes the generated client to:

```text
src/shiftservice-client
```

The generated client is development output and is already gitignored, so it should not be committed.

The script expects the Shiftservice OpenAPI endpoint to be reachable at:

```text
http://shiftservice.127.0.0.1.nip.io/v3/api-docs
```

In local development that usually means:

1. start `shiftservice`
2. verify that `/v3/api-docs` is reachable
3. run `npm run generate-shiftservice-client`

### Alternative CI/container-based generation

If you want to use the repository's Docker-based generation flow instead, run:

```bash
npm run generate-shiftservice-client-ci
```

### Other generated clients

The frontend also contains scripts for other backend clients:

```bash
npm run generate-auditservice-client
npm run generate-notificationservice-client
```
