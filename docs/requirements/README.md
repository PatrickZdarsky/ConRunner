# Requirements specifications

Requirements specifications describe the intended, observable behavior of a module. They are kept
with the code so a migration does not accidentally treat the legacy implementation as the only
source of truth.

They are deliberately lean:

- A requirement states a stable business rule, a scope boundary, or an intentional compatibility
  decision.
- Tests are the executable evidence for a requirement; link them instead of duplicating their full
  arrangements and assertions here.
- An ADR records an architectural or product decision when alternatives need to be preserved. A
  requirement records the resulting behavior.
- Design details such as private method names, JPA annotations, and REST routing do not belong here.

Create a module specification from [the template](0000-module-template.md) when its first behavior
is migrated. Do not pre-fill specifications for placeholder modules.

## Requirement IDs and status

Each module chooses a short, stable prefix—for example `EC` for the event catalogue. Requirement
IDs are never reused. A requirement is one of:

- **Implemented**: behavior and at least one test exist in this repository.
- **Planned**: agreed direction without an implementation yet.
- **Open**: an intentional unresolved decision that blocks behavior or data migration.

Current specifications:

| Module | Specification | Status |
| --- | --- | --- |
| Event catalogue | [Event catalogue requirements](eventcatalog.md) | First non-HTTP slice implemented |
