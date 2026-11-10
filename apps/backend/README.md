# ShiftControl backend

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
