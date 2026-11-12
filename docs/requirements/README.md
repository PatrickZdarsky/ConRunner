# ConRunner requirements

The specifications describe observable behavior and intentional compatibility decisions.
Each requirement has a stable module-prefixed ID. Link tests that exercise implemented behavior
and use the [module template](0000-module-template.md) for new specifications. Record design
decisions and their rationale in an [ADR](../adr/README.md).

## Sources and precedence

The [product baseline](product-baseline.md) traces requirements to the responsibility sheet,
legacy behavior, and ownership decisions. Resolve disagreements using this order:

1. Newer explicit product or ownership decisions in the architecture plan or project context.
2. Reviewed module requirements.
3. The responsibility sheet for product intent.
4. The old `shiftservice` code and tests as behavioral evidence.

For shift staffing, [ADR-0002](../adr/0002-compatibility-first-shift-migration.md) makes legacy
workflow behavior and tests the default baseline. Explicit requirements and ADRs record approved
changes.

## Module specifications

| Area | Specification | Current scope |
| --- | --- | --- |
| Product baseline | [Sources and traceability](product-baseline.md) | Draft baseline for review |
| Identity | [Identity](identity.md) | OIDC sign-in, local accounts, and platform capabilities |
| Event catalogue | [Event catalogue](eventcatalog.md) | Event management and catalogue persistence for locations and sessions |
| Shift staffing | [Shift staffing](shift.md) | Shift plans, staffing, and rewards under the compatibility-first migration |
| Communications | [Communications](communications.md) | In-app history and preferences, realtime updates, and opt-in email |
| Audit | [Audit](audit.md) | Catalogue and assignment audit records, search, and retention |
| Programme scheduling | [Schedule](schedule.md) | Optional session context for staffing in Stage 1, with live editing deferred to Stage 2 |
| Pretalx integration | [Pretalx](integrations-pretalx.md) | Deferred |
| Risk | [Risk](risk.md) | Deferred |
| Dispatch | [Dispatch](dispatch.md) | Deferred |
| Platform | [Platform](platform.md) | Cross-cutting technical requirements |

## Requirement status

- **Implemented** means behavior and a test are present in this repository.
- **Planned** describes an agreed target awaiting implementation.
- **Open** identifies an unresolved behavior or migration decision.
- **WIP / out of scope** marks future work outside the current migration.
- **Retired** marks a legacy behavior excluded from the target system.
