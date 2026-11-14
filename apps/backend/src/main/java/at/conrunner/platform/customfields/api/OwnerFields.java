package at.conrunner.platform.customfields.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Custom-field value operations for one owner. Obtain a view using
 * {@link CustomFields#forOwner(CustomFields, FieldEntityType, UUID)}.
 */
public final class OwnerFields {
    private final CustomFields fields;
    private final FieldEntityType entityType;
    private final UUID ownerId;

    OwnerFields(CustomFields fields, FieldEntityType entityType, UUID ownerId) {
        this.fields = Objects.requireNonNull(fields, "fields");
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
    }

    /**
     * Reads this owner's set values.
     *
     * @return an unmodifiable map of values by definition key, in key order
     * @see CustomFields#values(FieldEntityType, UUID)
     */
    public Map<String, JsonNode> values() {
        return fields.values(entityType, ownerId);
    }

    /**
     * Saves a JSON value that contains no user references.
     *
     * @param key key of an applicable definition
     * @param value JSON value to store
     * @see CustomFields#put(FieldEntityType, UUID, String, JsonNode, FieldReferenceAccess)
     */
    public void put(String key, JsonNode value) {
        put(key, value, null);
    }

    /**
     * Saves a string as a JSON value. The definition still determines the allowed value format.
     *
     * @param key key of an applicable definition
     * @param value string to store
     * @see #put(String, JsonNode)
     */
    public void putText(String key, String value) {
        put(key, TextNode.valueOf(value));
    }

    /**
     * Saves a JSON value using the domain's permission check for any user references.
     *
     * @param key key of an applicable definition
     * @param value JSON value to store
     * @param references domain permission check for user references
     * @see CustomFields#put(FieldEntityType, UUID, String, JsonNode, FieldReferenceAccess)
     */
    public void put(String key, JsonNode value, FieldReferenceAccess references) {
        fields.put(entityType, ownerId, key, value, references);
    }

    /**
     * Unsets an optional field.
     *
     * @param key key of an applicable optional definition
     * @see CustomFields#remove(FieldEntityType, UUID, String)
     */
    public void remove(String key) {
        fields.remove(entityType, ownerId, key);
    }

    /**
     * Checks that every applicable required field has a stored value.
     *
     * @see CustomFields#validateRequired(FieldEntityType, UUID)
     */
    public void validateRequired() {
        fields.validateRequired(entityType, ownerId);
    }
}
