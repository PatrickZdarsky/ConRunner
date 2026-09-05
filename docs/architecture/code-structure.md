# Backend code structure and module interaction

This guide is the convention for code migrated from the legacy `shiftservice`. It deliberately uses
familiar Spring types (`endpoint`, request/response DTOs, mappers, services, DAOs, repositories) and
does not require an `application` / `domain` / `infrastructure` split.

The architectural boundary that matters is the application module. Inside a module, organize code
by feature so the files that change together are close together.

## Start with a module, then a feature

Every direct child of `at.control.shiftcontrol` is a Spring Modulith application module. It owns its
tables, Liquibase changes, state transitions, and HTTP endpoints. It is not a dumping ground for
unrelated implementation classes.

The existing empty `application`, `domain`, and `infrastructure` directories are transitional
scaffolding from the initial project setup. Do not add new migrated code to them. Remove an empty
scaffold package when its module receives real feature packages. Delete comment-only
`package-info.java` files; retain one only where it supplies real module metadata, such as a
Spring Modulith named interface.

The `shift` module is the first substantial migration target. It owns volunteer profiles, plan
membership/access, plans, shifts, positions/slots, assignments, trades, time constraints, and
rewards. A volunteer references identity's stable UUID user ID; it is not an identity entity.

The shift migration is compatibility-first under
[ADR-0002](../adr/0002-compatibility-first-shift-migration.md). Keep its established workflows
cohesive: a feature package is an organizational aid, not a submodule or a reason to redesign a
legacy workflow before its behavior and tests have been ported.

Use feature packages within `shift` as code is migrated:

```text
shift/
├── ShiftService.java
├── TimeConstraintService.java
├── plan/
├── positionslot/
└── assignment/
```

Do not create packages in advance and do not create `trade`, `reward`, or `availability`
mini-modules merely to anticipate future work. `assignment` is an organizational package for the
established live-assignment workflow, not a submodule. The established `ShiftPlanService`,
`ShiftService`, `PositionSlotService`, `AssignmentService`, `AssignmentEligibilityService`, and
`TimeConstraintService` may work across directly related shift persistence types while remaining
inside this one application module.

For example, the files for migrating position-slot preferences belong together:

```text
shift/positionslot/
├── PositionSlotEndpoint.java
├── PositionSlotRequest.java
├── PositionSlotResponse.java
├── PositionSlotMapper.java
├── PositionSlotService.java
├── PositionSlotDao.java
├── PositionSlotRepository.java
├── PositionSlotPreferenceRepository.java
└── query/
    └── PositionSlotSpecifications.java
```

Do not create every folder in advance. Create a package only when a concrete, cohesive workflow
needs it. A crowded feature is not by itself a reason to introduce nested technical-layer packages
such as `positionslot.web` or `positionslot.persistence`; first keep the workflow together and
extract only a genuinely independent concern. Do not introduce a global `dto`, `repo`, or `mapper`
package for the entire application.

The same convention applies elsewhere: `eventcatalog.event`, `eventcatalog.venue`,
`eventcatalog.session`, `schedule.planning`, `schedule.publication`, and `schedule.constraint` are
better starting points than a technical layer spanning all modules.

## Responsibility flow inside a feature

The usual write flow is:

```text
HTTP endpoint → service → DAO or repository → database
```

- **Endpoint**: HTTP mapping, request validation, current-actor extraction, response status, and
  conversion to/from HTTP request/response types. It contains no business decision or persistence
  query.
- **Service**: use-case orchestration, authorization, business validation, transaction boundary,
  and event publication. A write use case is normally `@Transactional`; read use cases can be
  `@Transactional(readOnly = true)` where that adds value.
- **Repository**: a Spring Data repository for one persistence type. It is internal to the feature
  and module.
- **DAO**: an optional, feature-specific persistence facade. Use it when a use case needs complex
  query composition, controlled fetch strategy, native/query projections, or coordinated access to
  multiple repositories. The service then injects the DAO, not the repositories hidden behind it.
- **Mapper**: an explicit conversion at a boundary, usually HTTP ↔ feature value/entity or a
  persistence projection ↔ read response. Mappers must not load data, decide authorization, or
  reach into another module.

### Application data and failures

Use a small feature-local `*Data` record when a service needs a group of inputs. It is an
application value, not an HTTP DTO: an endpoint can map `CreateShiftRequest` to `ShiftData`, and an
importer can construct the same `ShiftData`. Do not call every data record `*Command`; reserve that
name for a genuinely distinct action such as `PublishSchedule` or `AcceptAssignment`.

An input record may normalize its own intrinsic values (for example trimming a name, validating an
absolute URL, or ensuring an end time is after a start time). This keeps that low-level work out of
the service. The service still owns rules requiring existing state: uniqueness, whether a referenced
record belongs to the same event, whether a record is mutable, authorization, and persistence.

Use module-specific exception types for not-found, validation, and state-conflict outcomes. When an
HTTP feature is added, a `@RestControllerAdvice` at the web boundary maps those types to RFC 9457
`ProblemDetail` responses (normally 404, 400/422, and 409). Do not add a global exception handler
before an HTTP endpoint needs it, and do not use HTTP status concerns inside services.

Either of these is valid; choose one per feature:

```text
Simple CRUD:       service → repositories
Complex persistence: service → feature DAO → repositories
```

Never inject both a DAO and one of the repositories it wraps. Do not add a generic `BasicDao` or a
DAO that only proxies `findById`, `save`, and `delete`; those operations are already provided by a
Spring Data repository.

`PositionSlotPreference` is a good example of a justified DAO concern. Updating a volunteer's
preference is an upsert over the preference relation and may involve slot lookup/fetch behavior. A
`PositionSlotDao` (or a narrower `PositionSlotPreferenceDao`) may hide both repositories and expose
`setPreference(userId, slotId, value)` and `preferenceOf(userId, slotId)`. The service keeps the
authorization and preference-range rule.

## Queries and JPA Specifications

JPA `Specification` objects, entity field paths, joins, and fetch graphs are persistence details.
They remain inside the owning feature, usually in a `query` package or as private helpers in a DAO.

An endpoint must not pass an HTTP DTO into a Specification, and a caller must not construct a
`Specification` itself. Map the HTTP filter to a feature-local criteria value, then call a
purpose-named query method:

```text
ShiftSearchRequest → ShiftSearchCriteria → ShiftDao.search(criteria)
                                      → internal ShiftSpecifications → repository
```

For example, `ShiftSearchCriteria` can contain plan IDs, role IDs, location IDs, day, and a name
term. `ShiftSpecifications` composes those criteria with internal JPA joins. If a different module
later needs the result, expose a dedicated module API such as `ShiftCalendar.findAssignedShifts(...)`;
do not export the criteria's JPA implementation or the Specification.

## Module boundaries and contracts

The default rule is to pass stable IDs (`UserId`, `PlanId`, `ShiftId`, represented by UUIDs until
dedicated value types earn their keep). Do not pass JPA entities, repositories, HTTP request/response
types, mapper classes, DAOs, or JPA Specifications across a module boundary.

When a module genuinely needs a synchronous answer from another module, create a small immutable
contract in the provider's named `api` interface:

```text
shift/api/
├── package-info.java                  # @NamedInterface("api")
├── ShiftEligibility.java              # exported Spring bean interface
└── VolunteerEligibility.java           # immutable record
```

The consumer explicitly allows only that named interface in its module metadata:

```java
@ApplicationModule(allowedDependencies = "shift::api")
package at.control.shiftcontrol.someconsumer;
```

This keeps the module root free of contract classes. It also makes a cross-module dependency visible
in one place. Do not create an `api` package until a real consumer exists.

## Events

Publish an event only for a meaningful completed fact that another module needs to react to, such
as `PlanPublished`, `AssignmentAccepted`, or `VolunteerEligibilityChanged`. Do not publish one event
per CRUD method.

Published integration event records live in the provider's named `events` interface:

```text
shift/events/
├── package-info.java                  # @NamedInterface("events")
└── AssignmentAccepted.java
```

The producer publishes through `ApplicationEventPublisher` inside its service transaction. A
consumer listens from its own internal package with `@ApplicationModuleListener`. Spring Modulith
then treats the listener as an asynchronous transactional listener running in its own transaction.

Use a direct `api` call when the current use case needs an immediate answer. Use an event when the
producer should not know which modules react. Before an event becomes a durable business guarantee,
make the Spring Modulith event-publication schema, retry/recovery behavior, and retention explicit
through the project's Liquibase and operational configuration.

## Persistence and migration rules

- Each module owns `src/main/resources/db/changelog/<module>/` and includes its changes from the
  application master changelog.
- Migrate a legacy entity, its repositories/DAOs, services, endpoints, DTOs, mappers, and tests as
  one feature slice. Do not first copy every entity into a new shared model.
- For `shift`, legacy behavior and tests are the default specification. Record every intended
  behavioral change before coding it, with its requirement/ADR and replacement test. Do not replace
  a live workflow record with a new history model solely to prepare for a future feature.
- Prefer a new UUID-backed schema designed for its owner module over preserving an old entity shape
  solely because it was shared by the legacy services.
- Keep compatibility adapters only at an API edge and delete them once callers have migrated.
- Test packages mirror production feature packages. Unit-test services/DAOs, repository-test query
  behavior, and add endpoint/integration tests for important authorization and state transitions.

### Deferred reward guardrails

Reward work is deferred inside the cohesive `shift` module and is constrained by
[ADR-0001](../adr/0001-reward-wallet-and-award-invariants.md). Do not add mutable point balances to
an existing aggregate as a shortcut. When reward work starts, its append-only ledger is the only
balance authority, and ledger mutations occur in the same transaction as the explicit assignment
operation that caused them. Do not extract a reward mini-module as a prerequisite.

Do not let callers change an accepted assignment's volunteer through a repository or entity setter.
The assignment feature owns explicit accept, unassign, auction-claim, and trade operations so a
future reward feature can preserve immutable award terms through transfers. Preserve stable UUID
references and do not use deletion cascades that would erase ledger attribution.

Reward exports are authorized read-side behavior, not part of the ledger writer. Do not introduce a
public reward-report endpoint, bearer share link, or reward-share token while the feature is
deferred.

## Naming and anti-patterns

Use names that reveal the feature and intent: `ShiftDao.searchAssigned`,
`PositionSlotPreferenceDao.setPreference`, and `PlanMembershipService.grantPlannerAccess` are good.

Avoid `common`, `shared`, `util`, generic `manager`, generic `BasicDao`, and a module-wide global
`dto`/`mapper` directory. If a helper has a real business name, place it in that feature. If it is
technical and used across modules, evaluate whether it belongs in `platform`; do not move business
concepts there.

Dispatch is deliberately outside this initial coding convention. Define its package and interaction
rules when dispatch migration begins, using the same named-contract and no-shared-persistence rules.
