# Requirements specifications

Requirements specifications describe the intended, observable behavior of ShiftControl modules.
They are kept with the code so the migration does not accidentally treat the legacy implementation
as the only source of truth.

They are deliberately lean:

- A requirement states a stable business rule, a scope boundary, or an intentional compatibility
  decision.
- Tests are the executable evidence for a requirement; link them instead of duplicating their full
  arrangements and assertions here.
- An ADR records an architectural or product decision when alternatives need to be preserved. A
  requirement records the resulting behavior.
- Design details such as private method names, JPA annotations, and REST routing do not belong here.

The initial event-catalogue slice used [the template](0000-module-template.md). The documents now
also establish a draft target baseline for modules that are not yet implemented, so migration
work has a requirement to implement rather than merely a legacy service to copy.

## Sources and precedence

[Product baseline and traceability](product-baseline.md) records the responsibility-sheet coverage,
legacy evidence, and the ownership decisions that intentionally replace legacy design. When sources
disagree, apply this order:

1. A newer explicit product or ownership decision in the architecture plan or project context.
2. These requirements specifications, once reviewed.
3. The responsibility sheet for product intent.
4. The old `shiftservice` code and tests as behavioral evidence only.

In particular, legacy package/entity ownership and implementation shortcuts are not requirements.

## Requirement IDs and status

Each module chooses a short, stable prefix—for example `EC` for the event catalogue. Requirement
IDs are never reused. A requirement is one of:

- **Implemented**: behavior and at least one test exist in this repository.
- **Planned**: agreed direction without an implementation yet.
- **Open**: an intentional unresolved decision that blocks behavior or data migration.
- **WIP / out of scope**: documented future direction that must not expand the current migration.
- **Retired**: a legacy behavior intentionally excluded from the target system.

Current specifications:

| Area | Specification | Status |
| --- | --- | --- |
| Product baseline | [Sources, reconciliation, and traceability](product-baseline.md) | Draft baseline for review |
| Identity | [Identity requirements](identity.md) | Foundation implemented |
| Event catalogue | [Event catalogue requirements](eventcatalog.md) | First non-HTTP slice implemented |
| Shift staffing | [Shift requirements](shift.md) | Planned migration target |
| Programme scheduling | [Schedule requirements](schedule.md) | Stage 1 migration support; Stage 2 WIP/out of scope |
| Pretalx integration | [Pretalx integration requirements](integrations-pretalx.md) | WIP/out of scope |
| Communications | [Communications requirements](communications.md) | Planned migration target |
| Audit | [Audit requirements](audit.md) | Planned migration target |
| Risk | [Risk requirements](risk.md) | WIP/out of scope |
| Dispatch | [Dispatch requirements](dispatch.md) | WIP/out of scope |
| Platform | [Platform requirements](platform.md) | Cross-cutting planned target |
