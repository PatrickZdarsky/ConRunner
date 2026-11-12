# ConRunner backend

ConRunner runs the backend as one Spring Boot application under `at.conrunner`. Domain modules
share the deployment and database and own their own persistence and workflows.

## Run and test

Use JDK 25 and the Maven wrapper at the repository root. The [local infrastructure guide](../../ops/compose/README.md)
shows how to start PostgreSQL and Keycloak and run the backend with the `local` profile. The local
API exposes [Swagger UI](http://localhost:8081/swagger-ui.html) and
[OpenAPI JSON](http://localhost:8081/v3/api-docs).

```sh
./mvnw test
```

Docker is required for PostgreSQL tests that use Testcontainers. For browser tests, see
[the Playwright guide](../../e2e/README.md). If you change an HTTP endpoint or DTO, regenerate the
Angular client with `./dev client` from the repository root.

## Application modules

| Package | Ownership |
| --- | --- |
| `identity` | Local user UUIDs, external identity mappings, authentication, and platform roles |
| `eventcatalog` | Events, locations, and programme sessions |
| `shift` | Volunteers, plan membership, invites, shifts, roles, assignments, trades, and rewards |
| `communications` | Notification requests, preferences, history, and email delivery |
| `audit` | Audit entries, search, and retention |
| `platform` | Shared technical facilities, including WebSocket connections |

The shift HTTP API still serves event management requests and delegates catalogue operations to
`eventcatalog`. See the [architecture guide](../../docs/architecture/README.md) for module boundaries,
[ADR-0002](../../docs/adr/0002-compatibility-first-shift-migration.md) for shift migration decisions,
and the [Liquibase guide](src/main/resources/db/changelog/README.md) for schema ownership.

Configuration examples use the property keys supported by the application. The `shiftcontrol.*`
prefix still applies to several runtime settings.

## OIDC configuration

The backend is an OAuth 2.0 resource server. Production configuration must provide the trusted issuer,
its JWK set URI, and the access-token audience expected by this API. The realm and audience
below are example deployment values.

```yaml
oidc:
  provider:
    issuer-uri: https://identity.example/realms/conrunner
    jwk-set-uri: https://identity.example/realms/conrunner/protocol/openid-connect/certs
    audience: conrunner-backend
    username-claim: preferred_username
```

`allowed-issuers` defaults to `issuer-uri`. The identity module owns OIDC subjects and issuers.
APIs and other modules use ConRunner's local user UUID. `username-claim` selects the token claim
used for the local username and defaults to `preferred_username`. The local account also stores
email, email verification state, and avatar information when available.

Bootstrap administrators by exact issuer and subject. For a machine client, list the authorities
the client needs, such as `audit.read` for a client that queries the audit timeline.

```yaml
identity:
  bootstrap-platform-admins:
    - issuer: https://identity.example/realms/conrunner
      subject: 8c4d9d54-6b2a-4b42-a285-1bf1d5494881
  machine-clients:
    - client-id: audit-reader
      authorities:
        - audit.read
```

ConRunner grants and persists the local `PLATFORM_ADMIN` role when that exact issuer and
subject sign in. A machine client receives only its configured authorities, and
only when all of those authorities are also present in its token's `scope` or `scp` claims.

## Notifications

### Email and preferences

Email delivery is disabled by default. Configure the SMTP host, port, and credentials under
`spring.mail`, then set the following properties.

| Property | Value |
| --- | --- |
| `shiftcontrol.communications.email.enabled` | `true` to start the email worker |
| `shiftcontrol.communications.email.from` | Sender address |
| `shiftcontrol.communications.email.frontend-base-url` | HTTPS URL of the web app |

Email requires opt-in for the notification type and a verified email claim from the identity
provider. SMTP timeouts are defined in [`application.yaml`](src/main/resources/application.yaml).
Delivery uses at-least-once retries. If the process fails after SMTP accepts a message, a retry
can send a duplicate.

Notification history is available at `GET /api/v1/me/notifications` and its
read state can be changed with `PATCH /api/v1/me/notifications/{id}` using `{"read": true}` or
`{"read": false}`. `GET /api/v1/me/notification-preferences` lists supported notification types
with a display label and current in-app and email settings. Use the returned type IDs to read or
update a setting with `GET` or `PUT` on
`/api/v1/me/notification-preferences/{in-app|email}/{type}`, using `{"enabled": true}` or
`{"enabled": false}` for PUT. In-app defaults to enabled and email defaults to disabled. ConRunner
captures settings when it creates a notification request and rechecks email opt-out and address
verification before delivery. Read or update the notification language with `GET` or `PUT` on
`/api/v1/me/notification-preferences/language`. PUT takes a body such as `{"language": "en"}`.
See the [legacy notification migration notes](../../docs/architecture/legacy-notifications.md)
for retired endpoints and the previous delivery path.

### WebSocket updates

An authenticated user calls `POST /api/v1/realtime/ticket`, opens a WebSocket to
`/api/v1/realtime/ws`, then sends the returned `ticket` as its first text message within
10 seconds. The server replies with `{"type":"ready"}` and emits
`{"type":"notifications.changed"}` whenever the user's history changes. Fetch notification
history on connection and whenever that message arrives. Reconnect with a new ticket after a
disconnect. Connections expire when the access token expires or after 15 minutes, whichever comes
first. Tickets are single-use and expire after at most 60 seconds.

The backend accepts same-origin connections by default. Set
`shiftcontrol.platform.realtime.allowed-origins` to a comma-separated list of additional trusted
frontend origins.
Use HTTPS/WSS in deployment. Instances poll a shared database revision, so a connection on any
instance can receive changes committed by another instance.

## Audit history

`GET /api/v1/audit` returns a bounded, newest-first audit timeline for platform administrators
and principals with `audit.read`. Use `page` (zero-based), `size` (1–100), and optional filters
`from` (inclusive) and `until` (exclusive) ISO instants, `actorUserId`, `module`, `action`,
`subjectType`, and `subjectId`. Text filters are exact. The response contains actor and subject
labels, a readable summary, stable identifiers, and approved before/after field changes. The UUID
tie-breaker makes ordering deterministic. Page offsets can shift when new records arrive.

New entries are written through `audit::api` in the caller's transaction. Producers explicitly
select safe fields and omit credentials, bearer links, raw payloads, and unnecessary personal data.
Changes use scalar snapshots and a schema version to support later interpretation. Audit history covers
event catalogue changes and staffing assignment activity, including joins, leaves, requests and
decisions, auctions, trades, and removals. Bulk operations can produce one entry per affected
assignment.

At startup, ConRunner deletes entries older than two years by default. Configure an ISO-8601 period
such as `shiftcontrol.audit.retention=P3Y` to change retention on the next startup. Cleanup runs
in batches and stops startup if it fails. The legacy RabbitMQ `log_entry` archive
requires an inventory and sanitization before any historical import because it may contain bearer
tokens.

Audit producers can use `AuditRecorder` to add authenticated actor and trace context automatically.
Feature helpers own action identifiers, summaries, and approved fields. Assignment callers use
methods such as `joined(assignment)` or `leaveRequested(assignment, before)`. Take the immutable
`before` snapshot before mutating the entity. Background producers can use
`recordAs` with an explicit system identity.

## Legacy RabbitMQ events

Legacy event publishing is disabled by default. To enable it, set
`shiftcontrol.events.rabbitmq.enabled=true` or `SHIFTCONTROL_EVENTS_RABBITMQ_ENABLED=true` and
configure the broker through Spring Boot's `spring.rabbitmq` properties. The publisher uses the
`shiftcontrol` topic exchange and includes trace IDs when a Micrometer tracer is configured.

Legacy event payloads can include sensitive credentials, and a broker or process failure can lose
events. Address those limitations before enabling the integration in production.
