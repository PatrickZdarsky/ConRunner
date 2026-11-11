# Communications requirements

Status: Planned migration target
Owner: `at.control.shiftcontrol.communications`
Last reviewed: 2026-08-30

## Purpose and scope

Communications owns notification preferences, in-app notification records, templates, delivery jobs,
delivery attempts, and real-time delivery. It receives a deliberate notification request containing
authorized recipients and safe context; it does not infer plan access, inspect other modules'
entities, or recompute domain authorization from raw tables.

## Functional requirements

| ID | Status | Requirement | Responsibility/architecture source |
| --- | --- | --- | --- |
| `COM-PREF-001` | Planned | A user can view and change notification channels per supported notification type. Changes take effect for subsequently created delivery work and are confirmed to the user. | `N2`, legacy notification settings |
| `COM-PREF-002` | Planned | Preference resolution is deterministic for every type/channel/user combination, including a user who has no stored preference row. In-app notifications are enabled by default; external channels use their own explicit preference. | `N2`, product decision |
| `COM-PREF-003` | Planned | Enabling email delivery requires a verified email from the identity provider. If no email is available, the preference change fails with a clear validation error. | Product decision |
| `COM-REQUEST-001` | Planned | A domain module requests notification work with a stable trigger ID, recipient IDs, notification type, safe template data, and link target. Recipient selection is made by the owning domain or a named read API before the request reaches communications. | Architecture module ownership, legacy recipient endpoint |
| `COM-REQUEST-002` | Planned | Assignment, trade, schedule-change, risk, and feedback events create notification requests when those features exist. Channel delivery still respects each recipient's preferences and available contact data. | Product decision |
| `COM-DELIVERY-001` | Planned | A notification record and any email, push, or real-time delivery work are created durably and idempotently. The same trigger must not create duplicate deliveries for the same recipient and channel. | `N1`, `N4`, architecture P0 |
| `COM-DELIVERY-002` | Planned | Email is attempted only for a valid authorized address and enabled email preference. It contains a useful subject, event context, and an authorized link without embedding secrets. | `N1` |
| `COM-DELIVERY-003` | Planned | Push is attempted only for an opted-in, registered device. When the active client can show the same event in-app/browser notification, the product applies a documented de-duplication policy. | `N4` |
| `COM-DELIVERY-004` | Planned | Delivery processing acknowledges work only after durable notification and attempt state is committed. Transient failure retries with bounded backoff; permanent or exhausted failure is observable in a dead-letter/failure state. | `N5`, architecture P0 |
| `COM-HISTORY-001` | Planned | A user can list only their own notification history, filter by type/date/read state, and mark an in-app notification read or unread. | `N3` |
| `COM-FEEDBACK-001` | Planned | A plan may expose an optional validated feedback URL. After the event ends, eligible plan members receive at most one feedback notification for the relevant plan/version. | `N6`, `SP20` |
| `COM-REALTIME-001` | Planned | Real-time UI updates are emitted from committed notification state through the application realtime endpoint. Reconnecting clients can recover from persisted history rather than relying on lossy live messages. | `N3`, legacy SignalR behavior |
| `COM-TEMPLATE-001` | Planned | Notification messages and template data are defined in one communications-owned place rather than scattered through domain modules. The structure should permit localization later without requiring localization work now. | Product decision |
| `COM-PRIVACY-001` | Planned | Notification records, templates, and delivery errors store only data needed to deliver and diagnose the message. They do not retain bearer tokens, raw event payloads, or unnecessary personal data. | Architecture P0/P1 |

## Interaction rules

- Domain modules own whether an event merits a notification and who is entitled to receive it.
- `communications` owns channel preference and delivery outcome, but does not change a shift
  assignment, plan membership, or risk alert as a side effect of failed delivery.
- External email/push work is an outbox job. A request is committed atomically with the domain
  change that requires it.

## Notification language

Notification text is stored in resource bundles owned by communications. The notification language
preference is available at `GET` and `PUT /api/v1/me/notification-preferences/language`; the PUT
body is `{"language":"en"}`, `{"language":"de"}`, or `{"language":"en-x-uwu"}` for the
furry convention variant. Users without a preference receive English.
Requests are rendered using the recipient's language when the worker processes them; the resulting
history entry and any queued email retain that language even if the preference later changes.
Existing history remains unchanged, and old queued requests still render with their available context.
Preference labels use the same language setting. Adding a language requires a complete
`notification-messages_<language>.properties` bundle and adding it to the supported-language set;
the `en-x-uwu` variant uses `notification-messages-uwu.properties`.

## Open questions

| ID | Question                                                                                                                                                                                                                                                                                                | Decision needed by |
| --- |---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| --- |
| `COM-OPEN-001` | Which email/push providers, retention periods, unsubscribe behavior, and other legal requirements apply to external delivery? | Before external delivery rollout. |
| `COM-OPEN-002` | Telegram is a future notification channel. What account-linking, consent, template, and delivery rules are required? | WIP and out of scope for the current migration. |
