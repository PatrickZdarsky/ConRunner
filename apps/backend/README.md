# ShiftControl backend

## Notification email

Email delivery is disabled by default. Configure `spring.mail.host`, the SMTP port and credentials,
then set `shiftcontrol.communications.email.enabled=true` and
`shiftcontrol.communications.email.from` to a sender address and
`shiftcontrol.communications.email.frontend-base-url` to the HTTPS application URL. Each user must opt in for the
notification type and have a verified email claim from the identity provider. SMTP timeouts are
configured in `application.yaml`. Delivery is retried and is at least once: a process failure after
SMTP accepts a message can result in a duplicate.

Notification history is available at `GET /api/v1/me/notifications` and its
read state can be changed with `PATCH /api/v1/me/notifications/{id}` using `{"read": true}` or
`{"read": false}`. `GET /api/v1/me/notification-preferences` lists supported notification types
with a display label and current in-app and email settings. Clients should use these returned type
identifiers instead of maintaining their own list. Manage a setting with `GET` or `PUT` on
`/api/v1/me/notification-preferences/{in-app|email}/{type}`, using `{"enabled": true}` or
`{"enabled": false}` for PUT. In-app defaults to enabled and email defaults to disabled. Settings
are captured when the notification request is created; email opt-out and address verification are
also rechecked before delivery. The former shift profile notification-settings and recipient endpoints
have been removed; the legacy notification service must not be used as a delivery consumer.

## Notification WebSocket

An authenticated user calls `POST /api/v1/realtime/ticket`, opens a WebSocket to
`/api/v1/realtime/ws`, then sends the returned `ticket` as its first text message within
10 seconds. The server replies with `{"type":"ready"}` and emits
`{"type":"notifications.changed"}` whenever the user's history changes. Fetch notification
history on connection and whenever that message arrives. Reconnect with a new ticket after a
disconnect; connections expire when the access token expires or after 15 minutes, whichever comes
first. Tickets are single-use and expire after at most 60 seconds.
Same-origin connections are allowed by default; set the comma-separated
`shiftcontrol.platform.realtime.allowed-origins` for explicitly trusted separate frontend origins.
Use HTTPS/WSS in deployment. Instances poll a shared database revision, so a connection on any
instance can receive changes committed by another instance.

## Legacy RabbitMQ events

Legacy event publishing is disabled by default. To enable it, set
`shiftcontrol.events.rabbitmq.enabled: true` in application configuration (or set
`SHIFTCONTROL_EVENTS_RABBITMQ_ENABLED=true`) and configure the broker using Spring Boot's
`spring.rabbitmq` properties. The publisher sends to the `shiftcontrol` topic exchange.
An optional Micrometer tracer supplies trace IDs when one is configured.

The legacy events include sensitive credentials and publishing is not durable across broker or
process failures. Address those limitations before using this integration in production.

This directory contains the single Spring Boot deployable. Domain separation is package-based so
Spring Modulith can discover and verify it without introducing a Maven module, network protocol, or
release pipeline for every domain.

## Application modules

All modules are direct children of `at.control.shiftcontrol`:

| Module | Ownership |
| --- | --- |
| `identity` | Stable local user IDs, external identities, authentication, and global capabilities |
| `eventcatalog` | Event, venue, location, session, and submission identities |
| `schedule` | Drafts, constraints, validation, publication, and immutable schedule versions |
| `shift` | Volunteers, plan membership/access, shift plans, shifts, roles, assignments, trades, and rewards |
| `dispatch` | Live operational cases/tasks, responder/resource state, escalation, and timeline |
| `communications` | Notification preferences, records, templates, and delivery attempts |
| `audit` | Sanitized audit records, retention, export, and query support |
| `risk` | Trust/risk policies, evaluation state, and alerts |
| `integrations` | External mappings, sync runs, credentials, and anti-corruption adapters |
| `platform` | Strictly technical shared facilities without business ownership |

The Pretalx adapter starts under `integrations.pretalx`. Pretalx transport types stay there; the
event catalog and schedule modules define their own models.

`shift` owns the staffing domain as one cohesive module. Its volunteer profiles and plan membership
records reference identity's UUID user ID. `dispatch` may reference that same ID, but owns live
operational responder state and must not access shift persistence directly.

## Package convention

Use module-first packages—not an `application` / `domain` / `infrastructure` split. Create a shift
subpackage only for an established, cohesive workflow; it is an organizational aid, never another
application module. Under [ADR-0002](../../docs/adr/0002-compatibility-first-shift-migration.md),
the established shift workflows remain cohesive rather than being divided into trade, availability,
or reward mini-modules. The `assignment` package is an organizational home for the live-assignment
workflow, not a submodule.

The existing empty layer directories are transitional scaffolding; do not add new migrated code to
them. Delete comment-only `package-info.java` files; retain one only where it supplies actual
module metadata. A subpackage becomes cross-module-visible only when it is deliberately annotated
as a Spring Modulith named `api` or `events` interface. The full rules and examples are in [the
backend code-structure guide](../../docs/architecture/code-structure.md).

Tests mirror their production feature package. Cross-module boundary verification belongs in
`at.control.shiftcontrol.architecture`; full-stack journeys belong in the repository-level `e2e`
directory.

Liquibase changes will be grouped under `src/main/resources/db/changelog/<module>`. Each module owns
its change sets and includes them from a single application-level master changelog once persistence
work begins.

## OIDC configuration

The backend is an OAuth 2.0 resource server. Production configuration must provide the trusted issuer,
its JWK set URI, and the access-token audience expected by this API:

```yaml
oidc:
  provider:
    issuer-uri: https://identity.example/realms/shiftcontrol
    jwk-set-uri: https://identity.example/realms/shiftcontrol/protocol/openid-connect/certs
    audience: shiftcontrol-backend
    username-claim: preferred_username
```

`allowed-issuers` defaults to `issuer-uri`. OIDC subjects and issuers remain internal to the identity
module; APIs and other modules use ShiftControl's UUID user ID. `username-claim` is the one token
claim copied to the local username; it defaults to `preferred_username` and can instead be a claim
such as `nickname`. First and last names are intentionally not stored.

For an initial platform administrator or a still-external transition client, configure only the
specific external identity or client capability that is needed:

```yaml
identity:
  bootstrap-platform-admins:
    - issuer: https://identity.example/realms/shiftcontrol
      subject: 8c4d9d54-6b2a-4b42-a285-1bf1d5494881
  machine-clients:
    - client-id: notification-worker
      authorities:
        - notifications.recipients.read
```

A bootstrap entry grants the local `PLATFORM_ADMIN` role when that exact issuer/subject signs in;
the role is then persisted locally. A machine client receives only its configured authorities, and
only when all of those authorities are also present in its token's `scope` or `scp` claim. These
temporary machine-client entries should disappear as callers move into this application.

## Initializr baseline

The generated dependencies and application/test bootstrap are intentionally unchanged. `HELP.md`
contains the original Initializr reference links. Add configuration and implementation only as the
corresponding migration phase begins.

## Audit history

`GET /api/v1/audit` returns a bounded, newest-first audit timeline for platform administrators
and principals with `audit.read`. Parameters are `page` (zero-based), `size` (1–100), optional
`from` (inclusive) and `until` (exclusive) ISO instants, `actorUserId`, `module`, `action`,
`subjectType`, and `subjectId`. Text filters are exact. The response contains actor and subject
labels, a readable summary, stable identifiers, and approved before/after field changes. The UUID
tie-breaker makes ordering deterministic; page offsets can still shift when new records arrive.

New entries are written through `audit::api` in the caller's transaction. Audit input is trusted
internal code, not a generic event or request serializer: producers must explicitly select safe
fields, and must not include credentials, bearer links, raw payloads, or unnecessary personal data.
Changes use scalar snapshots and a schema version to support later interpretation. There is no
rollback endpoint or general-purpose restoration guarantee. Assignment records currently cover
join/leave, requests and their decisions, auction and trade changes, and removals when their
containing slot, shift, plan, or event is deleted. Other business features are not yet instrumented.
Bulk operations can produce one entry per affected assignment.

Entries expire at startup after two years by default. Configure an ISO-8601 period such as
`shiftcontrol.audit.retention=P3Y`; configuration changes apply on the next startup. Startup cleanup
is performed in batches, and a failure stops startup. Ordinary application APIs cannot edit or delete
entries, although database privileges should be restricted separately in production. The legacy
RabbitMQ `log_entry` archive is not imported: it may contain bearer tokens and must be inventoried
and sanitized before any historical migration.

Audit producers can use `AuditRecorder` to add authenticated actor and trace context automatically.
Feature helpers own action identifiers, summaries, and approved fields; assignment callers use named
methods such as `joined(assignment)` or `leaveRequested(assignment, before)`. Take the immutable
`before` snapshot before mutating the entity. Missing authentication is rejected rather than silently
attributed to a system actor; background producers can use `recordAs` with an explicit system identity.
