# Risk requirements

Status: WIP — out of scope for the current migration
Owner: `at.control.shiftcontrol.risk`
Last reviewed: 2026-08-30

## Purpose and scope

Risk evaluates defined suspicious-behavior policies and records actionable alerts. It owns policy
evaluation state, alert lifecycle, and review evidence. It does not own a volunteer copy, plan
membership, global identity, or enforcement of staffing actions.

No risk-policy, alert, or enforcement migration is currently planned. These constraints are kept
for later work so risk does not grow into a second volunteer or staffing module.

## Functional requirements

| ID | Status | Requirement | Responsibility/architecture source |
| --- | --- | --- | --- |
| `RSK-POLICY-001` | WIP | Risk policies use explicit types, thresholds, time windows, and versions. A policy evaluation is reproducible from its recorded facts and configuration version. | Trust-alert backlog, legacy alert types |
| `RSK-ALERT-001` | WIP | A policy may create one deduplicated alert for a volunteer/user and relevant event or plan scope. The alert identifies the policy, observed window, safe evidence summary, creation time, and lifecycle state. | Trust alerts, architecture P1 |
| `RSK-ALERT-002` | WIP | Authorized administrators and scoped planners can review their permitted alerts and record a disposition and reason. Alert reads do not expose unnecessary volunteer data. | Trust-alert backlog |
| `RSK-ACTION-001` | WIP | Risk does not directly update shift membership, assignments, or volunteer locks. An approved action invokes an explicit `shift` command with actor, scope, reason, and audit trail. | Ownership baseline |
| `RSK-NOTIFY-001` | WIP | Required alert notifications are requested through communications as durable work after the alert/disposition transaction commits. | Architecture outbox model |
| `RSK-OPER-001` | WIP | Evaluation is idempotent, bounded by durable state, and observable. It must not require an in-memory Redis-only counter as the source of truth. | Architecture phase 3 |

## Open questions

| ID | Question | Decision needed by |
| --- | --- | --- |
| `RSK-OPEN-001` | WIP — Which policies are appropriate, what evidence is retained, and what false-positive review process is acceptable? | Before activating automated alerts. |
| `RSK-OPEN-002` | WIP — May any risk policy automatically restrict a volunteer, or is human review always required? | Before enforcement is implemented. |
