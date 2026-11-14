package at.conrunner.platform.customfields.api;

import at.conrunner.platform.exception.ConflictException;
import at.conrunner.platform.exception.NotFoundException;
import at.conrunner.platform.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages custom-field definitions and values for domain-owned records.
 *
 * <p>Callers authorize definition administration and owner access. Domain workflows decide when
 * to write values and when to check required fields, including when a schedule draft is committed.
 * Owner lookups use JDBC. When creating an owner with JPA in the same transaction, flush the owner
 * before reading or writing its custom-field values.
 */
public interface CustomFields {
    /**
     * Binds an owner type and ID for a series of value operations. The owner is resolved on each
     * operation, and each operation uses the supplied service's transaction boundary.
     *
     * @param fields injected custom-field service
     * @param entityType type of owner
     * @param ownerId domain ID of the owner
     * @return an owner-bound view of the service
     */
    static OwnerFields forOwner(CustomFields fields, FieldEntityType entityType, UUID ownerId) {
        return new OwnerFields(fields, entityType, ownerId);
    }

    /**
     * Registers a definition. A global key reserves that key across all events for the entity type.
     * Event-scoped definitions can reuse a key in different events.
     *
     * @param data definition properties
     * @return the persisted definition, including its generated ID and version
     * @throws ValidationException if the definition properties or configuration are invalid
     * @throws NotFoundException if the supplied event scope does not exist
     * @throws ConflictException if the key is already used in an overlapping scope
     */
    FieldDefinition createDefinition(FieldDefinitionData data);

    /**
     * Replaces a definition's display label, including for built-in definitions.
     *
     * @param id definition ID
     * @param expectedVersion version from the caller's last read
     * @param label nonblank display label of at most 255 characters, trimmed before storage
     * @return the definition after the update
     * @throws ValidationException if the label is invalid
     * @throws NotFoundException if the definition does not exist
     * @throws ConflictException if the stored version already differs from the expected version
     * @throws org.springframework.dao.OptimisticLockingFailureException if another write wins after the version check
     */
    FieldDefinition updateLabel(UUID id, long expectedVersion, String label);

    /**
     * Lists global and applicable event-scoped definitions in key order.
     *
     * @param entityType type of owner the definitions apply to
     * @param eventId event scope, or {@code null} to list global definitions only
     * @return the applicable definitions
     */
    List<FieldDefinition> definitions(FieldEntityType entityType, UUID eventId);

    /**
     * Reads the stored values for definitions that apply to the owner's current event.
     *
     * @param entityType type of owner
     * @param ownerId domain ID of the owner
     * @return an unmodifiable map of set values by definition key, in key order
     * @throws ValidationException if no store supports the entity type
     * @throws NotFoundException if the owner does not exist
     */
    Map<String, JsonNode> values(FieldEntityType entityType, UUID ownerId);

    /**
     * Locks the owner, validates the value against the applicable definition, and stores it.
     * A write replaces the entire previous value, including any array.
     *
     * @param entityType type of owner
     * @param ownerId domain ID of the owner
     * @param key key of an applicable definition
     * @param value JSON value to store
     * @param references domain permission check for user references, which may be {@code null}
     *        when the value contains no user references
     * @throws ValidationException if the entity type is unsupported or the value is invalid
     * @throws NotFoundException if the owner or an applicable definition for the key does not exist
     */
    void put(FieldEntityType entityType, UUID ownerId, String key, JsonNode value, FieldReferenceAccess references);

    /**
     * Locks the owner and unsets an optional field. An already unset optional field needs no change.
     *
     * @param entityType type of owner
     * @param ownerId domain ID of the owner
     * @param key key of an applicable optional definition
     * @throws ValidationException if the entity type is unsupported or the definition is required
     * @throws NotFoundException if the owner or an applicable definition for the key does not exist
     */
    void remove(FieldEntityType entityType, UUID ownerId, String key);

    /**
     * Checks that the owner has a stored value for every applicable required definition.
     * Call after saving the values in a workflow that requires a complete record.
     *
     * @param entityType type of owner
     * @param ownerId domain ID of the owner
     * @throws ValidationException if the entity type is unsupported or a required value is missing
     * @throws NotFoundException if the owner does not exist
     */
    void validateRequired(FieldEntityType entityType, UUID ownerId);
}
