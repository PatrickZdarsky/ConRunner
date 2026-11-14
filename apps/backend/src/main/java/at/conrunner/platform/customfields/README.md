# Custom fields

The `platform::customfields` interface provides definitions, value validation, and transactional value operations. `schedule` and `shift` supply owner stores with domain tables. The master Liquibase changelog creates real foreign keys for those owners.

Inject `CustomFields` into an authorized domain application service. The definition builder starts with a global, optional, single-value field and default constraints. Use `forEvent`, `required`, `multiple`, and `configuration` to set the options you need. Global definitions apply to all owners of that entity type. Event-scoped definitions apply only to owners in that event. Global and scoped definitions cannot shadow one another with the same key.

```java
fields.createDefinition(FieldDefinitionData
        .builder(FieldEntityType.SESSION, "stage_notes", "Stage notes", FieldValueType.TEXT)
        .forEvent(eventId)
        .build());

var sessionFields = CustomFields.forOwner(fields, FieldEntityType.SESSION, sessionId);
sessionFields.putText("stage_notes", "Leave the front row clear.");
var values = sessionFields.values();
```

Built-ins include event/session/location/shift descriptions, session abstract, and event social links. Built-in IDs and keys are stable. Labels can be changed with an expected definition version. Structural definition properties are immutable through this API. Type changes require an explicit value migration.

`put` accepts a JSON value, `putText` wraps a Java string, `values` returns values keyed by definition key, and `remove` unsets an optional value. The owner-bound view forwards each operation to the injected service. Each operation resolves the owner's event through its domain adapter. Writes lock the owner and participate in the caller's transaction. Call `validateRequired` when completing an owner creation/edit workflow, after supplying its required values. Adding a required definition to an existing event may leave existing owners incomplete until their values are filled.

| Type | Single value |
| --- | --- |
| TEXT | JSON string |
| URL | Absolute HTTP(S) URL string |
| NUMBER | Finite JSON number |
| DATE | ISO local date string |
| DATETIME | ISO datetime string with explicit offset |
| USER | Canonical UUID string |
| LINK | Object containing exactly `label` and `url` |

Multiple fields use a JSON array of the corresponding value shape. Empty arrays are allowed for optional fields. Missing optional values are represented by absent rows. JSON null and null array items are rejected.

Supported configuration keys are `maxLength` for text, `min`/`max` for numbers, and `maxItems` for multiple values. Unknown configuration keys fail validation. Defaults limit text to 100,000 characters, arrays to 1,000 items, and serialized field values to 1,000,000 characters.

For USER values, call `put(key, value, references)` with a `FieldReferenceAccess` implementation that verifies existence and permission to reference each user within the supplied event. The validator rejects user values without that check. Domain callers authorize read/write access and definition administration before calling the platform API. Schedule callers must preserve their draft/commit rules when applying values.

Value adapters target the UUID catalog tables and the UUID `shift_shift` schema from the master migrations. The older numeric-ID shift HTTP model requires its own integration during that module's migration. Existing description/link DTOs continue to use their existing fields until their domain workflows are switched to this API.

The local profile creates the custom-field tables alongside Hibernate-managed domain tables. As with the rest of that profile, full owner foreign-key coverage is verified with the master migrations. The PostgreSQL persistence tests exercise the master changelog and all four owner stores.

Run verification with `./mvnw -pl apps/backend -Dtest=FieldValueValidatorTest,CustomFieldPersistenceTest test`.
