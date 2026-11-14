# ScheduleControl — what we want it to do

This expands the [original notes](schedulecontrol.md) into a description we can build from. It describes desired behavior, not what already exists. Where the notes leave something open, **proposed defaults** below give us a starting point rather than pretending the decision has already been made.

## The basic idea

Expand the event catalog into the place where convention staff manage the program before and during an event. It should know both what was originally scheduled and what is happening now, including the history of how we got there.

An event can either own its schedule in ConRunner or import it from Pretalx. During the convention, staff need to react quickly: delay a session, move it to another room, cancel it, or add something last minute. Those changes should be deliberate, traceable, and available to the website, app, and eventually team notifications.

Use **Schedule** as the user-facing name and **ScheduleControl** for the management area. Renaming the internal `eventcatalog` module is a separate implementation choice; it is not necessary to deliver this behavior. This is the convention program. Volunteer shift planning depends heavily on it, so stable links between shifts and session occurrences are part of the core design.

## Two separate settings: where the schedule comes from, and how we edit it

The source mode belongs to each event, not to the whole installation. Two events can use different modes at the same time.

| Source mode | What it means |
| --- | --- |
| **Managed** (the notes' “Primary”) | ConRunner is the source of truth. Staff create and maintain sessions and locations here. |
| **Synced** | Pretalx supplies the source schedule. ConRunner imports changes, but never writes local changes back to Pretalx. Other providers can come later. |

Separately, use **Planning** and **Live operations** instead of “unlocked” and “locked.” Live operations means that the baseline is protected from ordinary local edits, not that nothing can change.

| State | Editing behavior |
| --- | --- |
| Managed + Planning | Add, edit, and delete sessions and locations directly. |
| Managed + Live operations | Preserve the baseline; make operational changes through recorded, committed updates. |
| Synced + awaiting first import | Show that no successful import exists yet. A failed import must not look like a ready schedule. |
| Synced + Live operations | Enter this state automatically after the first successful import. Imported data is locally read-only; operational changes remain possible. |

For a managed event, entering Live operations is an explicit action, typically when the program has been printed or otherwise finalized. Show what that action means before applying it.

A managed event can return from Live operations to Planning. Consolidate the committed operational changes into the editable baseline: the current effective times, rooms, content, additions, and cancellation state become the new plan. Do not apply the old overrides a second time. Keep occurrence identities, previous baselines, and the complete audit trail.

The UI must explain this consolidation and show a summary before the user confirms it. Record the transition itself. Entering Live operations again protects the newly consolidated baseline. Uncommitted drafts are not included; **proposed default:** require them to be committed or discarded before this transition so they cannot silently become stale.

Switching between Managed and Synced is a separate migration question, not part of returning to Planning.

## Keep the original plan and the current reality

We need to distinguish three things:

- **Baseline:** the managed schedule when Live operations began, or the latest successfully imported source schedule.
- **Operational changes:** committed local changes made by staff.
- **Effective schedule:** the result attendees and staff should currently follow, combining the baseline with those changes.

For synced events, keep enough source history to compare against the initial import or a previous imported release. A new import must not erase our ability to explain what changed from an earlier published or printed plan.

Every session occurrence needs a stable ConRunner identity. If the same talk or activity runs three times, those are three independently changeable occurrences. Moving one occurrence to another day or room does not create a new identity.

Sessions need a name, description, abstract where available, start and end time, and location, with a clear “unassigned” state where appropriate. Times must be presented in the event's timezone, including for events spanning midnight or several days.

## What staff can change during Live operations

Each committed update records the affected occurrence, type of change, previous and resulting values, who made it, when they committed it, and an optional reason. A combined update can affect several fields without losing that detail.

| Change | Expected behavior |
| --- | --- |
| **Delay** | Move an occurrence later. Preserve its duration by default and show the resulting start and end times. |
| **Reschedule** | Set a new date or time explicitly, including moving earlier or to another day. |
| **Duration change** | Change the end time or duration without requiring a move of the start time. |
| **Room change** | Move an occurrence to another location, including a locally added location. |
| **Cancellation** | Mark the occurrence as cancelled while retaining its identity and history. |
| **Content correction** | Override titles, descriptions, abstracts, and supported content fields while retaining source values and history. |
| **New session** | Add an occurrence that was not in the baseline, clearly marked as a local addition. |

A session can have any number of changes. If it is delayed twice and then moved, staff should see all three steps and the final result.

**Proposed defaults:**

- “Delay by 10 minutes” starts from the current effective time. If a 14:00 session is delayed by 10 minutes and then by another 5, it now starts at 14:15. The preview shows both the additional delay and total difference from the baseline.
- Committed time changes store the intended resulting times, so a later source update cannot accidentally apply the same delay again.
- Fix a mistaken committed change with a new correction or reversal entry. Do not silently rewrite or delete the old history. Restoring a cancelled session follows the same rule.
- Changing one session does not automatically move every later session in that room. Any future “shift the rest of the day” feature needs an explicit preview of all affected occurrences.
- Title and description overrides are allowed, including on imported sessions. Use a modular field-level correction mechanism so additional content fields can use the same preview, commit, audit, and source-override behavior.

### Content fields should be extensible

Description and abstract should fit an extensible content-field model rather than a collection of special-case correction paths. Decide during backend design whether they are built-in fields in a custom-field system from the start; the correction mechanism must support that direction either way. Fields need stable keys and defined value types so changes remain understandable as the model grows. A full custom-field editor, team-specific fields, and field visibility controls can follow later.

## Preview first, commit once

Staff should be able to prepare a set of changes without immediately affecting the live schedule or sending notifications.

The first live change automatically creates a shared draft batch together with its entry. Further changes join the event’s current draft batch. Commit or discard closes it, and the next live edit starts a fresh batch. Operators do not need to create an empty draft first.

1. Select one or more occurrences and prepare changes.
2. Show a review with before/after times, rooms, cancellations, additions, and any warnings.
3. Let the user adjust or discard the draft.
4. Commit the reviewed changes with one explicit action.
5. Update the effective schedule and make the committed changes available to downstream consumers.

Each committed wave has a common, stable batch ID linking all its individual changes, including changes across multiple occurrences. Preserve that grouping in history and the committed change feed. The whole reviewed batch should succeed together or remain unapplied. Discarding a draft has no effect on the live schedule. Retrying a commit after a connection problem must not duplicate changes or notifications.

If another operator or a sync changes the relevant data while a draft is open, require a refreshed review before committing. Do not silently apply an outdated preview.

Reject invalid data such as an end time before the start time or a location belonging to another event. Warn about room overlaps or sessions outside the event's dates, but allow an authorized operator to proceed with a warning where real-world circumstances require it.

Drafts and their proposed changes must be saved in the database from the start. Authorized operators for the event can see and collaborate on what is being prepared, including who last changed it. Refreshing or reconnecting must not lose the saved draft. Draft data stays separate from the effective schedule and public feeds.

Provide live draft updates to other operators, preferably through the existing WebSocket infrastructure. The backend comes first, including persistence, permissions, version checks, and update events; the later UI pass exposes this collaboration. A reconnect reloads authoritative state from the backend.

A review is tied to a particular draft revision and effective schedule revision. If a collaborator changes that draft, refresh the review before committing. Concurrent edits must not silently overwrite each other; full character-by-character collaborative text editing is not required. Record the commit actor as well as draft contributors. Committing or discarding a shared draft updates its status for everyone.

## Locations should stay flexible

Staff can add local locations even during Live operations, including for synced events. Clearly distinguish imported locations from local ones.

**Proposed default:** any occurrence can be moved to a local location, including an imported occurrence. Local sessions can also use imported locations. Restricting local rooms to local sessions would prevent a useful emergency room move.

Local locations and local sessions must survive subsequent syncs. A sync must not mistake them for source records that have disappeared.

Do not delete a location in a way that breaks existing sessions or their history. During Live operations, retire an unused location from selection instead of erasing it. Imported location details remain source-owned; local display ordering does not.

Location order must be configurable even for imported rooms. Store a shared order per event in the database so operators see a consistent layout after reload and on other devices. Per-operator room layouts are not needed. Importing a new schedule must not reset the shared order.

## Pretalx sync should be dependable

ConRunner acts as the schedule backend for its consumers. Website and app requests read locally stored schedule data instead of causing a Pretalx request each time. If Pretalx is unavailable, the last successfully imported schedule remains usable, including local operational changes.

Each synced event has its own source configuration. Sync is an ongoing process, with scheduled runs, a manual “sync now” action, and pause/resume controls. Staff can see whether it is running or paused, the last successful sync, the imported release/version when available, and errors or matching warnings. The refresh interval is still to be decided.

**Pause behavior:** pausing stops scheduled runs; an explicit manual run remains available without resuming the schedule. Pausing does not remove imported data or stop local operational changes.

Sync continues during Live operations, both on schedule and when triggered manually. Local operational overrides take priority over incoming source values. Live operations protects local decisions and records changes; it does not freeze the external schedule. Pause/resume is controlled per event.

Import a complete, validated snapshot before making it visible. A failed or partial fetch must not leave half the event updated or be interpreted as mass cancellation. Repeating the same import must not create duplicates or spurious change notifications.

### Stable identities across releases

In [Pretalx issue #2256](https://github.com/pretalx/pretalx/issues/2256), closed as not planned, the maintainer explains that submission IDs identify content while slot IDs belong to a particular schedule version and are recreated for new releases. ConRunner therefore needs its own occurrence identities; a stable content ID alone does not distinguish repeated occurrences. Validate matching against the supported source endpoints and versions.

- Keep stable ConRunner IDs across imports, so favorites, staffing references, operational history, and external links keep pointing at the same occurrence.
- Match repeated occurrences independently. Matching only by submission/talk ID is insufficient when the same activity runs several times.
- Do not rely solely on time or room for matching, since those are precisely the fields that can change.
- Use deterministic best-effort matching for the first version, combining available source identifiers with occurrence details and previous mappings. Do not require an operator resolution queue before imports can proceed.
- Record uncertain matches and the matching basis for diagnosis. Best effort cannot guarantee identity continuity in every ambiguous case. **Proposed fallback:** when no credible match exists, retain the old record/history and create a new identity rather than transferring local changes arbitrarily. Prevent multiple imported occurrences from mapping to the same local occurrence. A dedicated reconciliation workflow can come later.
- Verify the integration against representative source responses, including republishing, repeated occurrences, removals, and API version differences. The exact matching strategy depends on what the supported source API reliably provides.

### What happens when source updates meet local changes

The following policy applies to source updates, including during Live operations:

- Import source changes into the baseline while preserving committed local overrides for the fields they affect.
- Source changes to fields without a local override flow through to the effective schedule.
- If Pretalx changes an overridden field, retain the local result automatically and record the source difference. This applies to content fields as well as time and room. Staff can clear an override to follow the source again through a recorded change, but a mandatory conflict-resolution step is not required.
- Treat start/end times as a coherent pair so merging changes cannot produce an invalid duration.
- When the source removes or cancels an occurrence with local changes, preserve its last usable effective state and show a prominent warning in the sync summary. Do not block the entire sync on operator resolution. The warning must make clear that the local schedule may now differ from the source.
- For an unmodified occurrence removed from a complete source schedule, mark it withdrawn/cancelled and retain its identity and history. Do not hard-delete it.
- Show a sync summary covering additions, updates, withdrawals, and best-effort matching or source-disagreement warnings. Imported changes must be traceable as source changes rather than attributed to an operator's manual edit.
- If an import affects an open draft, mark its review as stale and notify collaborators through the live update mechanism. Require a refreshed preview before committing.
- Each import that changes the effective schedule creates a committed batch attributed to the source provider, linked to its sync run. Baseline-only updates remain auditable without producing misleading effective-schedule changes.

The notes also mention fields that may only appear updated after publishing a new source schedule. The UI should identify which source release it imported; it must not suggest that unpublished Pretalx edits are already visible. **Proposed default:** import the published schedule. Confirm the endpoint and release semantics during integration work.

### A proper audit log for every sync provider

Sync auditing is part of the first backend version, with a common model for Pretalx and future providers. It must answer: what ran, why, who initiated it, what source data it used, what changed, what stayed locally overridden, and what went wrong. Ordinary application logs alone are not sufficient.

Persist a record for every sync run, including successful runs with no changes and failed attempts. Each record contains:

- A stable run ID, event ID, provider, and source configuration reference. Retain a non-secret snapshot of the relevant configuration/version so later configuration changes do not make the record misleading.
- Trigger type (scheduled, manual, or retry), the initiating user for manual runs or system identity for automatic runs, and links between retry attempts.
- Start and finish timestamps, status, and the source release/version or other source revision identifier when available. Distinguish running, completed, completed with warnings, failed, interrupted, and skipped runs, with reasons where applicable.
- Counts of records examined, added, updated, withdrawn, unchanged, and warned about. Distinguish baseline changes from changes to the effective schedule.
- Sanitized error details and the stage that failed, such as fetch, validation, matching, or applying changes. Show whether anything was applied; failed atomic imports must explicitly show that the existing schedule was retained.
- Links to the resulting committed batch and individual changes when the effective schedule changed. A no-change run still has an audit record but does not create an empty change batch.

For each changed record, retain the local identity, relevant source identifiers, before/after baseline values, before/after effective values, and the decision taken. Record preserved local overrides, source removals, uncertain occurrence matches and their matching basis, and fallback identity creation. This must make it possible to explain a session's history across source releases without relying on the current source response still being available.

Audit pause/resume, source configuration changes, and manual sync requests as well, including who performed them and when. Never store credentials, authorization headers, or secrets in audit details. Access to sync history is event-scoped and restricted to authorized staff; it is not part of the public schedule API.

Persist applied schedule changes and their audit records together so a successful import cannot leave unrecorded mutations. Run failures must remain recorded even when the import transaction rolls back. Recover runs left unfinished by a crash as interrupted rather than leaving them permanently shown as running. Retries get their own linked attempt records and must not duplicate committed changes.

Completed audit records are not editable through normal application workflows. Keep them when a source configuration is replaced or removed; retention must be explicit rather than tied to deleting the connection. A configurable retention/archive policy can be decided separately.

Provide a paginated backend query for run history and run details, filterable by event, provider, time range, status, and affected occurrence. During the UI pass, add a readable sync history with run summaries, errors/warnings, per-record differences, and links to committed batches and session history. The durable committed change feed serves downstream consumers; the sync audit log also explains attempts and source changes that never altered the effective schedule.

## The calendar needs to be useful at the event

The calendar is a core operating tool, not just a way to display records. Staff should be able to:

- Move between event days and see rooms in the configured order.
- Read session names, effective times, and locations without opening every item.
- Find a session by name and filter by location or change status.
- Distinguish delayed, moved, cancelled, and locally added sessions using labels as well as color.
- Inspect the baseline, current effective values, and chronological change history for an occurrence.
- Prepare changes from the calendar and see their draft status before committing.
- Notice overlaps and sync warnings without hunting through unrelated pages.
- See saved drafts and live updates from other operators, clearly separated from committed changes.

If drag-and-drop or resizing is offered, it prepares a draft and uses the same review/commit flow. Provide ordinary form controls as well. Keep a usable list view for smaller screens and cases where the calendar is too dense.

### Give the Activity modal room to breathe

Rework the Activity modal during the UI pass. Session descriptions are currently too cramped, and abstract, custom content fields, baseline/effective values, and change history will need more space. Long content must be readable without being squeezed into a tiny description area.

Tabs are a candidate, not a settled requirement: for example, Overview, Content, and Changes. Choose the structure after reviewing the existing modal with realistic long descriptions and many changes. Keep the session's effective time, room, and status easy to find, and reuse a tab component if that layout proves useful elsewhere.

## Consumers, permissions, and notifications

Provide a documented API, with an OpenAPI contract, for the effective schedule and its stable occurrence identities. Consumers should be able to distinguish cancellations and local additions and detect a new schedule revision. Public responses must exclude credentials and private operational or future team notes.

Schedule reads and edits are event-scoped. Distinguish viewing from operational editing, source configuration, and event-level state changes using the application's permission model. Record who committed changes, including corrections and conflict resolutions.

Drafts never generate attendee notifications. A committed batch should provide a structured description of the changes so notification consumers can act on the final result. A failed notification delivery must not undo the schedule change; retries must not send duplicates.

The committed change feed is required in the first backend version. Persist it with stable change IDs, the common batch ID, affected occurrence IDs, actor/source, commit time, and before/after values. Consumers must be able to resume from a cursor or revision and recognize retries. Draft collaboration events are separate from this feed. Actual attendee delivery, subscriptions, and team-specific routing are follow-up work. Source updates that change the effective schedule should use the same change mechanism; a sync that changes nothing effective should not notify people as though the program moved.

Shift planning depends on the schedule: preserve links from shifts to their session occurrences across imports, operational changes, and consolidation back to Planning. Down the line, volunteers assigned to related shifts should receive notifications when a session is delayed, moved, cancelled, or otherwise changed in a relevant way. The committed feed must carry enough occurrence identity and change detail to support that lookup. Automatically moving volunteer shifts or assignments is outside this scope and needs a separate decision.

## Delivery order

The [phase 1 backend architecture](schedulecontrol-phase1-architecture.md) expands this scope into entities, services, transaction flows, APIs, and implementation slices.

1. **Backend first:** event modes and state transitions, baseline/history and consolidation, extensible content corrections, persisted collaborative drafts, atomic commits and batch IDs, effective schedule queries, shared room ordering, continuous/manual/pausable Pretalx sync including Live operations, provider-independent sync auditing, best-effort identity matching, staffing references, and the durable committed change feed. Provide backend events for live UI updates and document the APIs.
2. **UI pass next:** review the calendar and Activity modal against those capabilities. Add draft collaboration, preview/commit, mode-transition explanations, sync controls/status and audit history, readable content, and shared room ordering. Decide on tabs and other layout details here.
3. **Future features:** track the list below separately from the initial backend and UI work.

## Future feature list

- Actual attendee and team notification delivery, subscriptions, and routing, including notifications to volunteers assigned to shifts related to changed sessions.
- Signage REST endpoints that return the effective schedule for a supplied list of rooms. Potentially add an SSE feed so signs can react to committed changes without repeated polling. These are separate from internal WebSocket draft updates.
- A dedicated operator workflow for resolving ambiguous source matches and reconciling source/local disagreements. The first version uses best effort and diagnostic warnings.
- Additional schedule providers beyond Pretalx.
- Full custom-field configuration and private operational information for teams such as security and stage crew, building on the modular content/override foundation.
- Team-specific views of the same underlying schedule, with appropriate access controls.
- Multilingual content. The first UI can use a single language, normally English; leave room for language-tagged source content and a defined fallback later.
- Bulk operations such as shifting all remaining sessions in a room, with an explicit preview.

## A few examples that should work

- A managed event edits its program freely, enters Live operations, then records two delays on one session. Both entries remain visible, while the public schedule shows the final effective time.
- An operator drafts a room move and cancellation, reviews both, then discards them. Nothing changes for attendees and no notification is generated.
- An imported session moves to a newly added local room. A later applied Pretalx release changes its description, which has no local override. The description updates, the local room move remains, and the occurrence keeps its identity.
- Pretalx republishes three occurrences of the same activity with different timeslot IDs. Existing favorites and local changes still refer to the correct individual occurrences, using best-effort matching without requiring an operator resolution queue. Uncertain matches are recorded, and unmatchable occurrences use the documented fallback.
- A source outage leaves the last good schedule available. Staff can still record operational changes, and a later successful sync preserves them.
- A saved draft survives a refresh and is visible to another authorized operator. Their edits update the shared draft without changing the public schedule.
- A committed wave changes three sessions. Its individual changes share one batch ID in history and the change feed.
- A managed event returns to Planning. Its effective values become the new baseline, its audit history remains available, and re-entering Live operations does not reapply old delays.
- A description override survives an applied source update; clearing that override explicitly restores the latest imported description.
- Pausing sync stops scheduled imports while keeping the existing schedule and manual sync available without resuming scheduled runs.
- Two operators edit the same session. The second commit requires a new review if the first commit made its preview stale.
- A live sync changes an unmodified session and preserves a local room override on another. Its audit record explains both outcomes and links to the batch containing the effective changes.
- A failed import and a successful no-change import each appear in sync history. Neither creates a schedule-change batch, and a later retry is linked without duplicating changes.
- A mistaken cancellation is reversed with another recorded action, restoring the occurrence without losing the cancellation history.

## Decisions still worth checking

The source modes and state names, returning managed events to Planning, shared room ordering, collaborative persisted drafts, content overrides, continued live sync with local override precedence, pause/manual-run behavior, provider-independent sync auditing, and deferred notification delivery are agreed. The remaining choices are narrower:

- What scheduled sync interval is appropriate?
- What retention/archive policy should apply to sync audit history?
- What source evidence is sufficient for best-effort occurrence matching, and is creating a new identity the right fallback when no credible match exists?
- Should description and abstract be built-in custom fields immediately, and what minimal field schema is needed for the backend foundation?
- Is requiring shared drafts to be committed or discarded before returning to Planning the right transition behavior?
- Do we need source-mode migration between Managed and Synced soon, or can it remain outside the initial scope?

The Activity modal layout, including whether to use tabs, is deliberately left for the UI pass.
