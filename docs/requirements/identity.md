# Identity requirements

Status: Foundation implemented
Owner: `at.control.shiftcontrol.identity`
Last reviewed: 2026-08-30

## Purpose and scope

Identity establishes who is authenticated and supplies the stable local user UUID used throughout
the application. It owns local accounts, external OIDC identity mappings, authentication state,
and global platform capabilities. It does not own volunteer profiles, plan membership, staffing
roles, notification preferences, or dispatch responder state.

## Functional requirements

| ID | Status | Requirement | Evidence |
| --- | --- | --- | --- |
| `ID-AUTH-001` | Implemented | A successful authentication resolves to one local UUID account. The UI and all other modules use this UUID; they never need to carry OIDC issuer and subject. | [User sync tests](../../apps/backend/src/test/java/at/control/shiftcontrol/identity/service/impl/ApplicationUserSyncServiceImplTest.java) |
| `ID-AUTH-002` | Implemented | An external identity mapping is unique by `(issuer, subject)` and remains internal to identity. A local account may therefore outlive a provider-specific representation. | [User sync tests](../../apps/backend/src/test/java/at/control/shiftcontrol/identity/service/impl/ApplicationUserSyncServiceImplTest.java) |
| `ID-AUTH-003` | Implemented | User JWTs must satisfy standard temporal validation and a configured trusted issuer and audience before they authenticate. | [JWT security tests](../../apps/backend/src/test/java/at/control/shiftcontrol/identity/auth/JwtSecurityConfigTest.java) |
| `ID-AUTH-004` | Implemented | A suspended local account cannot authenticate even when its external token is otherwise valid. | [JWT token tests](../../apps/backend/src/test/java/at/control/shiftcontrol/identity/auth/JwtTokenDecoderTest.java) |
| `ID-PROFILE-001` | Implemented | The local username is read from one configured token claim, `preferred_username` by default. First name, last name, and a derived display name are intentionally not stored. | [User sync tests](../../apps/backend/src/test/java/at/control/shiftcontrol/identity/service/impl/ApplicationUserSyncServiceImplTest.java) |
| `ID-CAP-001` | Implemented | Platform roles are persisted local roles. Initial platform administrators are assigned only through explicit configured external-identity mappings, not arbitrary token role claims. | [User sync tests](../../apps/backend/src/test/java/at/control/shiftcontrol/identity/service/impl/ApplicationUserSyncServiceImplTest.java) |
| `ID-CAP-002` | Implemented | Transitional machine clients receive only explicitly configured authorities that are also present in their token scope claim. They are not a general substitute for inter-module calls. | [JWT security tests](../../apps/backend/src/test/java/at/control/shiftcontrol/identity/auth/JwtSecurityConfigTest.java) |
| `ID-USER-001` | Planned | Authorized platform administration can search and inspect the minimal account directory: local UUID, username, status, and global roles. Staffing memberships are queried from `shift`, not duplicated in identity. | Legacy user-administration endpoints and services |
| `ID-USER-002` | Planned | Every protected application action has an actor: a local user UUID, a constrained machine identity, or an explicit system actor. | [Platform requirements](platform.md) |

## Interaction rules

- A module may retain the local UUID as an opaque reference, but must not import identity entities or
  repositories.
- Global capabilities establish platform-wide authority. Event and shift-plan authority is evaluated
  by the owning `shift` module, not inferred from a global role alone.
- Existing accounts are associated with a different OIDC identity only through an explicit,
  security-reviewed account-linking flow. Silent linking by username or email is prohibited.

## Open questions

| ID | Question                                                                                                                                                                                                                                | Decision needed by |
| --- |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| --- |
| `ID-OPEN-001` | Which verified recovery or explicit account-linking flow, if any, may attach an additional OIDC identity to an account? => by default none. Still the option to add one which would be configured by the app config should be left open | Before supporting more than one identity per person. |
| `ID-OPEN-002` | What retention/deletion and anonymization policy applies to a local account once staffing, audit, rewards, and legal obligations are considered? => Is not needed for now                                                               | Before production data deletion is offered. |
