# Local backend development

Start PostgreSQL and a development Keycloak from the repository root:

```sh
docker compose -f ops/compose/compose.yaml up -d
./mvnw -pl apps/backend spring-boot:run -Dspring-boot.run.profiles=local
```

Java 25 and Docker Compose are required. The backend listens on `http://localhost:8081`.
Alternatively, run `ShiftcontrolApplication` in your IDE with the `local` Spring profile.
The local profile runs the audit migration and lets Hibernate update the remaining schema because
the legacy entity mappings and current migrations overlap. This schema is for development only
and persists in the `postgres-data` volume; it does not validate the production migrations.

Keycloak runs at `http://localhost:8180`, with admin console credentials `admin` / `admin`.
The imported `shiftcontrol` realm contains the public client `shiftcontrol-local` and a platform
administrator user `dev` / `dev`. Obtain a development access token with:

```sh
curl -s -X POST http://localhost:8180/realms/shiftcontrol/protocol/openid-connect/token \
  -d 'grant_type=password' -d 'client_id=shiftcontrol-local' -d 'username=dev' -d 'password=dev'
```

Use the returned `access_token` as a Bearer token. Keycloak may take a little time to start.
These credentials and settings are for local development only. The Keycloak development database
is stored in the container; recreating it reimports the realm and resets changes made in its console.

Stop the services with `docker compose -f ops/compose/compose.yaml down`. Add `-v` to also delete
all local PostgreSQL data.
