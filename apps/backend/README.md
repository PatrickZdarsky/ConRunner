# ShiftControl backend

This directory contains the single Spring Boot deployable. Domain separation is package-based so
Spring Modulith can discover and verify it without introducing a Maven module, network protocol, or
release pipeline for every domain.

## Application modules

All modules are direct children of `at.control.shiftcontrol`:

| Module | Ownership |
| --- | --- |
| `identity` | Local profiles, external identities, and capability evaluation |
| `eventcatalog` | Event, venue, room, session, and submission identities |
| `schedule` | Drafts, constraints, validation, publication, and immutable schedule versions |
| `workforce` | Shift planning, roles, positions, assignments, trades, and rewards |
| `dispatch` | Live operational cases/tasks, resources, assignments, escalation, and timeline |
| `communications` | Notification preferences, records, templates, and delivery attempts |
| `audit` | Sanitized audit records, retention, export, and query support |
| `risk` | Trust/risk policies, evaluation state, and alerts |
| `integrations` | External mappings, sync runs, credentials, and anti-corruption adapters |
| `platform` | Strictly technical shared facilities without business ownership |

The Pretalx adapter starts under `integrations.pretalx`. Pretalx transport types stay there; the
event catalog and schedule modules define their own models.

## Package convention

Most business modules use this starting convention; omit a layer when the module does not need it:

```text
<module>/
├── package-info.java        # module contract and public API package
├── application/             # commands, queries, use cases, transactions
├── domain/                  # aggregates, values, policies, domain events
└── infrastructure/          # persistence, HTTP, messaging, jobs, framework adapters
```

Public types needed by another module belong in the module root package. Everything in a subpackage
is internal by default. This follows Spring Modulith's default package model and avoids a broad
shared library. `platform` deliberately has no `domain` package so it cannot become a home for shared
business concepts.

Tests should be placed alongside the owning module under the matching test package. Cross-module
boundary verification belongs in `at.control.shiftcontrol.architecture`; full-stack journeys belong
in the repository-level `e2e` directory.

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
```

`allowed-issuers` defaults to `issuer-uri`. OIDC subjects and issuers remain internal to the identity
module; APIs and other modules use ShiftControl's UUID user ID.

## Initializr baseline

The generated dependencies and application/test bootstrap are intentionally unchanged. `HELP.md`
contains the original Initializr reference links. Add configuration and implementation only as the
corresponding migration phase begins.
