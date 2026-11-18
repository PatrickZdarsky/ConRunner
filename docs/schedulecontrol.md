ScheduleControl TM Requirements
===

So we gotta expand the eventcatalog. Maybe even just rename it to schedule?

Basically it should be able to have 2 Modes: Primary and Synced. Where the first just controls the schedule completely and the other syncs changes from Pretalx (or another source but that's for later).

These modes are always per-event. 


## Primary
In the primary event mode (idk the name is odd I wanna express that we are the source of truth), the schedule control has full control over the schedule. This includes the ability to add, edit, and delete sessions and locations.

An operator can activate Live operations to freeze the schedule baseline. Returning to Planning is outside the current scope.



### Synced
In the synced event mode, the schedule control is synced with Pretalx (or another source). Changes made in Pretalx are reflected in the schedule control, not the other way around.
After the first successful import, an operator can activate Live operations. Activation disables sync and freezes the latest successfully imported schedule as the baseline. See the [current lifecycle requirements](schedulecontrol-requirements.md#two-separate-settings-where-the-schedule-comes-from-and-how-we-edit-it).

## Locked
Locked means the primary data is locked and cannot be edited. This mode is used for during the event when the schedule has already been printed for conbooks etc. So any changes are basically delays or changed rooms because something happened.
Here any changes to sessions will each create an entry denoting the type of change, eg.
- Delay
- Rescheduled
- Room Change
- Cancellation
- New Session?? (For last minute new sessions?)

Also we should also have the ability to still add locations but in case of a pretalx synced event those are just local then and we can just use them for manual sessions which were created as such a locked mode session? idk.
The main goal is it should be flexible!

Also for a single session, there can be many entries for eg. delays. This makes it clear what happened when and by who.
Anothergoal would be in the UI that one can make these entries and those are only applied once the user has commited them giving them a brief overview of the changes.
Cause these changes could trigger notifications to attendees.

Also we will probably, definitely need to properly upgrade the calendar view to make it properly usable.
Also even with synced locations the ordering in the UI should be configureable. Question would be if this should be saved in the database or just in the session.

Also we should have the ability to add new locations even if the schedule is locked. This is useful for last minute changes or new sessions that were not planned before the event.

## Pretalx Sync
Regarding the Pretalx sync here are some raw notes from my meeting with koboltwuff from NFC who also had some issues with that:

```
Self-Written Middleware Backend API “schedule-api”
Acts as cache for schedule requests coming from website/app/etc. since pretalx cripples under heavy API load
Rearranges fields, adds info etc.
fixes timeslot-ids changing upon new schedule release
Possibility to overwrite event slot time/date/location
“ScheduleOps” Frontend for Conops to enter slot changes
OpenAPI Spec
Issue with newly generated timeslotids on schedule publishing (Closed Github Issue)
Middleware for Pretalx API to give proper API
Schedule OPS for time/date/duration overrides during the convention
Biggest shortcomings of pretalx: Timeslots for when it’s scheduled, the timeslot id is not persistent each new schedule release generates new timeslot ids (It create a complete copy of the events/slots etc.)
They added a suffix for the slots. Previously app favourites used the slot id which was changing each time the schedule was re-released
This problem mainly consists when when an event has many occurrences (eg ConOps event)
Pretalx cannot handle many API requests at once, the requests are very resource hungry
Pretalx new releases are not always painless, compare API outputs from previous version
Some fields are not updated in the API endpoint without creating a new schedule (fore example the normal pretalx fields)
Add possibility to create different schedules with extra info for different teams
Eg. Security wants to know when things change to re-evaluate the event
Currently submissions can just go ahead and change stuff
Basically a proper notification system of changes for the different teams
```
The above also kinda denotes some things cause they have the schedule on their website rendered for attendees to se as well as in their App. Also ScheduleOps is their Middleware (what we are building rn).
Also would later on wanna add the possibility to have custom fields with info's for different teams such a s security, stage etc.
Also keep i18n in mind. Pretalx for example can give multi language infos. But for the beginning we ignore it. Sincve most cons just use english.
