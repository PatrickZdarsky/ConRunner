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
| `COM-PREF-002` | Planned | Preference resolution is deterministic for every type/channel/user combination, including a user who has no stored preference row. The product default is centrally defined and tested. | `N2`, legacy default-settings gap |
| `COM-REQUEST-001` | Planned | A domain module requests notification work with a stable trigger ID, recipient IDs, notification type, safe template data, and link target. Recipient selection is made by the owning domain or a named read API before the request reaches communications. | Architecture module ownership, legacy recipient endpoint |
| `COM-DELIVERY-001` | Planned | A notification record and any email, push, or real-time delivery work are created durably and idempotently. The same trigger must not create duplicate deliveries for the same recipient and channel. | `N1`, `N4`, architecture P0 |
| `COM-DELIVERY-002` | Planned | Email is attempted only for a valid authorized address and enabled email preference. It contains a useful subject, event context, and an authorized link without embedding secrets. | `N1` |
| `COM-DELIVERY-003` | Planned | Push is attempted only for an opted-in, registered device. When the active client can show the same event in-app/browser notification, the product applies a documented de-duplication policy. | `N4` |
| `COM-DELIVERY-004` | Planned | Delivery processing acknowledges work only after durable notification and attempt state is committed. Transient failure retries with bounded backoff; permanent or exhausted failure is observable in a dead-letter/failure state. | `N5`, architecture P0 |
| `COM-HISTORY-001` | Planned | A user can list only their own notification history, filter by type/date/read state, and mark an in-app notification read or unread. | `N3` |
| `COM-FEEDBACK-001` | Planned | A plan may expose an optional validated feedback URL. After the event ends, eligible plan members receive at most one feedback notification for the relevant plan/version. | `N6`, `SP20` |
| `COM-REALTIME-001` | Planned | Real-time UI updates are emitted from committed notification state through the application realtime endpoint. Reconnecting clients can recover from persisted history rather than relying on lossy live messages. | `N3`, legacy SignalR behavior |
| `COM-PRIVACY-001` | Planned | Notification records, templates, and delivery errors store only data needed to deliver and diagnose the message. They do not retain bearer tokens, raw event payloads, or unnecessary personal data. | Architecture P0/P1 |

## Interaction rules

- Domain modules own whether an event merits a notification and who is entitled to receive it.
- `communications` owns channel preference and delivery outcome, but does not change a shift
  assignment, plan membership, or risk alert as a side effect of failed delivery.
- External email/push work is an outbox job. A request is committed atomically with the domain
  change that requires it.

## Open questions

| ID | Question                                                                                                                                                                                                                                                                                                | Decision needed by |
| --- |---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| --- |
| `COM-OPEN-001` | Which notification types and channels are enabled by default for a newly seen user? => in-app push => also telegram needs to be added down the line                                                                                                                                                     | Before preference migration. |
| `COM-OPEN-002` | Which providers, retention periods, localization rules, and unsubscribe/legal requirements apply to email and push? => localization is not an issue for now but having the system already added would be greatly beneficial to have the messages contained in a single place and not throughout the app | Before external delivery rollout. |
| `COM-OPEN-003` | Which assignment, trade, schedule, risk, and feedback events are mandatory versus optional notifications? =>  All of those are mandatory, or: What would optional ones look like?                                                                                                                       | Before processor migration. |
| `COM-OPEN-004` | What verified email/contact source may communications use when identity intentionally stores no email profile field? => if the idp doesn't provide an email and therefore there is none in the system, return an error if the user changes his preference to also send to email                         | Before email delivery migration. |
