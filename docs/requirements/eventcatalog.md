# Event catalogue requirements

Status: First non-HTTP slice implemented  
Owner: `at.control.shiftcontrol.eventcatalog`  
Last reviewed: 2026-09-01

## Purpose and scope

The event catalogue owns the stable identities and current descriptive data for events, locations, and
sessions migrated from the legacy `shiftservice`. It provides the catalogue data needed for staffing
today and for the future schedule module.

This first slice deliberately excludes REST endpoints, authorization, event publication, Pretalx
sync/mappings, venue modelling, submissions, and a data-import migration. Those capabilities gain
requirements when their first real caller is introduced.

## Vocabulary and legacy mapping

| Current term | Legacy term | Meaning |
| --- | --- | --- |
| Event | `Event` | An event with its descriptive information and time range. |
| Location | `Location` | A specific place inside an event. |
| Session | `Activity` | A scheduled programme item with its current title, description, time range, and optional location. |

## Functional requirements

| ID | Status | Requirement | Evidence |
| --- | --- | --- | --- |
| `EC-EVENT-001` | Implemented | An event must have a non-blank name and a time range whose end is strictly after its start. | [Event service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/event/EventServiceTest.java) |
| `EC-EVENT-002` | Implemented | Event names must be unique without regard to case. Event links must be absolute URLs and duplicate type/URL pairs are retained once. | [Event service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/event/EventServiceTest.java) |
| `EC-EVENT-003` | Implemented | An event is open at a supplied instant when its end is at or after that instant. | [Event service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/event/EventServiceTest.java) |
| `EC-EVENT-004` | Planned | Authorized event-catalogue administration can create, change, archive, and delete events. Archive/delete behavior must be explicit about dependent catalogue records and must not cascade into shift, schedule, audit, or dispatch data. | `ADM1`, legacy event CRUD |
| `EC-EVENT-005` | Planned | Event cloning is an explicit cross-module workflow. It creates a new event identity and may ask owning modules to copy selected configuration; it never copies volunteers, assignments, invites, audit history, credentials, or dispatch records by default. | `ADM2`, legacy event clone |
| `EC-EVENT-006` | Planned | A structured event-setup import validates its input and reports item-level failures before applying typed catalogue and staffing setup commands. It does not bypass module validation or create cross-module JPA coupling. | Legacy workbook import, platform migration direction |
| `EC-LOCATION-001` | Implemented | A location belongs to exactly one existing event and its name is unique within that event, without regard to case. The same location name is allowed in another event. | [Location service and persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/location/LocationServiceTest.java) and [catalogue persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/EventCatalogPersistenceTest.java) |
| `EC-LOCATION-002` | Implemented | An imported read-only location cannot be changed or deleted. But it can still be linked to from a session. | [Location service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/location/LocationServiceTest.java) and [session service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/session/SessionServiceTest.java) |
| `EC-LOCATION-003` | Implemented | Deleting a mutable location clears its location reference from existing sessions rather than deleting those sessions. | [Catalogue persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/EventCatalogPersistenceTest.java) |
| `EC-SESSION-001` | Implemented | A session must have a non-blank name and a time range whose end is strictly after its start and fully lies within its event time range. | [Session service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/session/SessionServiceTest.java) |
| `EC-SESSION-002` | Implemented | A session can reference only a location belonging to the same event. | [Session service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/session/SessionServiceTest.java) |
| `EC-SESSION-003` | Implemented | Session names must be unique within an event without regard to case. | [Session service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/session/SessionServiceTest.java) |
| `EC-SESSION-004` | Implemented | Session suggestions may filter by case-insensitive name and by an overlapping time range with a non-negative tolerance. | [Session service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/session/SessionServiceTest.java) |
| `EC-SESSION-005` | Planned | Authorized catalogue administration can manage mutable event, location, and session metadata. Imported read-only records remain integration-managed; their update/delete rules are enforced by the responsible owner module. | `ADM1`, `ADM6`, legacy activity/location CRUD |

## Ownership and interactions

Events, locations, sessions, and event social links have UUID identities owned by `eventcatalog`. The
module's persistence types and repositories are internal. Other modules will receive stable UUIDs
and purpose-named immutable values through a named module API only when they become real consumers.

The initial migration retains a session's time and location reference for legacy behavioral parity. The
future `schedule` module owns editable placement, schedule drafts, validation, and published
versions. When it is introduced, it will reference event-catalogue session and location UUIDs rather
than take over their catalogue identity.

## Compatibility and migration decisions

- The catalogue retains the legacy `Location` name. Pretalx room values map to locations at the
  integration boundary.
- Legacy numeric identities are intentionally replaced with generated UUIDs.
- Case-insensitive uniqueness and same-event location assignment are deliberate safety improvements.
- The legacy `additionalProperties` map on `Location` is not part of the initial model: no current
  behavior consumes it. A future data migration must explicitly decide whether fields such as
  capacity, floor, or equipment become typed location capabilities.
- The legacy activity's time and location fields remain temporarily. They are not an agreement that
  the event catalogue permanently owns editable schedule placement.
- The current `rewardPointsRedeemUrl` event field is legacy descriptive compatibility metadata. It
  is not a reward redemption workflow, balance authority, or authorization mechanism. Future
  redemption rules and history are owned by `shift.reward` under ADR-0001.

## Open questions

| ID | Question                                                                                                                                                                                           | Decision needed by |
| --- |----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| --- |
| `EC-OPEN-001` | Future location capabilities must be introduced as typed, migration-backed fields or values when a real use case exists. Keep that evolution possible; do not restore the legacy unstructured `additionalProperties` map. | Before importing legacy location data or building venue management. |
| `EC-OPEN-002` | Pretalx source-of-truth decisions are deferred and must not block the current catalogue or shift migration. | WIP and out of scope until Pretalx work starts. |
| `EC-OPEN-003` | A synchronous named API means a deliberately small catalogue service/query—for example, an immutable location or session summary for an event—rather than another module importing catalogue entities or repositories. No such API is needed until `shift` requires it. | When the first consuming use case is migrated. |
