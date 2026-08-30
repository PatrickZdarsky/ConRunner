# Event catalogue requirements

Status: First non-HTTP slice implemented  
Owner: `at.control.shiftcontrol.eventcatalog`  
Last reviewed: 2026-08-30

## Purpose and scope

The event catalogue owns the stable identities and current descriptive data for events, rooms, and
sessions migrated from the legacy `shiftservice`. It provides the catalogue data needed for staffing
today and for the future schedule module.

This first slice deliberately excludes REST endpoints, authorization, event publication, Pretalx
sync/mappings, venue modelling, submissions, and a data-import migration. Those capabilities gain
requirements when their first real caller is introduced.

## Vocabulary and legacy mapping

| Current term | Legacy term | Meaning |
| --- | --- | --- |
| Event | `Event` | An event with its descriptive information and time range. |
| Room | `Location` | A specific place inside an event. The legacy Pretalx synchronizer already treated locations as rooms. |
| Session | `Activity` | A scheduled programme item with its current title, description, time range, and optional room. |

## Functional requirements

| ID | Status | Requirement | Evidence |
| --- | --- | --- | --- |
| `EC-EVENT-001` | Implemented | An event must have a non-blank name and a time range whose end is strictly after its start. | [Event service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/event/EventServiceTest.java) |
| `EC-EVENT-002` | Implemented | Event names must be unique without regard to case. Event links must be absolute URLs and duplicate type/URL pairs are retained once. | [Event service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/event/EventServiceTest.java) |
| `EC-EVENT-003` | Implemented | An event is open at a supplied instant when its end is at or after that instant. | [Event service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/event/EventServiceTest.java) |
| `EC-ROOM-001` | Implemented | A room belongs to exactly one existing event and its name is unique within that event, without regard to case. The same room name is allowed in another event. | [Room service and persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/room/RoomServiceTest.java) and [catalogue persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/EventCatalogPersistenceTest.java) |
| `EC-ROOM-002` | Implemented | An imported read-only room cannot be changed or deleted. But it can still be linked to from a session. | [Room service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/room/RoomServiceTest.java) and [session service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/session/SessionServiceTest.java) |
| `EC-ROOM-003` | Implemented | Deleting a mutable room clears its room reference from existing sessions rather than deleting those sessions. | [Catalogue persistence tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/EventCatalogPersistenceTest.java) |
| `EC-SESSION-001` | Implemented | A session must have a non-blank name and a time range whose end is strictly after its start and fully lies within its event time range. | [Session service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/session/SessionServiceTest.java) |
| `EC-SESSION-002` | Implemented | A session can reference only a room belonging to the same event. | [Session service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/session/SessionServiceTest.java) |
| `EC-SESSION-003` | Implemented | Session names must be unique within an event without regard to case. | [Session service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/session/SessionServiceTest.java) |
| `EC-SESSION-004` | Implemented | Session suggestions may filter by case-insensitive name and by an overlapping time range with a non-negative tolerance. | [Session service tests](../../apps/backend/src/test/java/at/control/shiftcontrol/eventcatalog/session/SessionServiceTest.java) |

## Ownership and interactions

Events, rooms, sessions, and event social links have UUID identities owned by `eventcatalog`. The
module's persistence types and repositories are internal. Other modules will receive stable UUIDs
and purpose-named immutable values through a named module API only when they become real consumers.

The initial migration retains a session's time and room reference for legacy behavioral parity. The
future `schedule` module owns editable placement, schedule drafts, validation, and published
versions. When it is introduced, it will reference event-catalogue session and room UUIDs rather
than take over their catalogue identity.

## Compatibility and migration decisions

- The legacy `Location` name is intentionally replaced by `Room`, because existing Pretalx sync code
  uses locations as rooms.
- Legacy numeric identities are intentionally replaced with generated UUIDs.
- Case-insensitive uniqueness and same-event room assignment are deliberate safety improvements.
- The legacy `additionalProperties` map on `Location` is not part of the initial model: no current
  behavior consumes it. A future data migration must explicitly decide whether fields such as room
  capacity, floor, or equipment become typed room capabilities.
- The legacy activity's time and location fields remain temporarily. They are not an agreement that
  the event catalogue permanently owns editable schedule placement.

## Open questions

| ID | Question | Decision needed by |
| --- | --- | --- |
| `EC-OPEN-001` | Which room capabilities from legacy `additionalProperties` are required, and which belong in the future venue/room model? | Before importing legacy room data or building venue management. |
| `EC-OPEN-002` | Which source is canonical for event, room, submission, and session data once Pretalx synchronization is introduced? | Before building the Pretalx adapter. |
| `EC-OPEN-003` | Which catalogue reads need synchronous named APIs for `shift` and `schedule`? | When the first consuming use case is migrated. |
