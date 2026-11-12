# ConRunner local infrastructure

[`compose.yaml`](compose.yaml) starts PostgreSQL and a development Keycloak. For the web app and
backend together, follow the [root quickstart](../../README.md#local-development).

## Run the backend by itself

Use JDK 25 and Docker with the Compose plugin. From the repository root:

```sh
docker compose -f ops/compose/compose.yaml up -d --wait
./mvnw -pl apps/backend spring-boot:run -Dspring-boot.run.profiles=local
```

You can also run `at.conrunner.ConRunnerApplication` in your IDE with the `local` Spring profile.
Wait for Keycloak's realm import to finish before signing in.

| Service | Local address | Development configuration |
| --- | --- | --- |
| Backend | `http://localhost:8081` | [OpenAPI JSON](http://localhost:8081/v3/api-docs) |
| PostgreSQL | `localhost:5432` | Database, user, and password are all `shiftcontrol` |
| Keycloak | [Admin console](http://localhost:8180/admin/) | Console login `admin` / `admin` |
| Keycloak application client | `shiftcontrol-local` in the `shiftcontrol` realm | Application login `dev` / `dev` |

The local Spring profile applies the audit Liquibase changelog and uses Hibernate to update the
rest of the development schema. PostgreSQL data persists in the `postgres-data` volume. See the
[database migration guide](../../apps/backend/src/main/resources/db/changelog/README.md) for the
master changelog.

## Call the local API

The development realm supports the password grant for manual API testing. Request an access token
and send its `access_token` value in the `Authorization: Bearer <token>` header.

```sh
curl -fsS -X POST http://localhost:8180/realms/shiftcontrol/protocol/openid-connect/token \
  -d 'grant_type=password' -d 'client_id=shiftcontrol-local' \
  -d 'username=dev' -d 'password=dev'
```

## Reload or stop local services

To apply changes to [`realm.json`](keycloak/realm.json), recreate the Keycloak container. This
reimports the realm and resets changes made in the Keycloak admin console.

```sh
docker compose -f ops/compose/compose.yaml up -d --force-recreate keycloak
```

Run `./dev down` to stop and remove the containers while keeping PostgreSQL data.
`docker compose -f ops/compose/compose.yaml down -v` also deletes the PostgreSQL volume and its data.
