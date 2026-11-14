package at.conrunner.platform.customfields.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.UUID;

/**
 * Input to {@link CustomFields#createDefinition(FieldDefinitionData)}.
 *
 * @param key stable key of 1–64 characters, starting with a lowercase ASCII letter and
 *        followed by lowercase ASCII letters, digits, or underscores
 * @param label nonblank display label of at most 255 characters, trimmed before storage
 * @param entityType type of owner the definition applies to
 * @param valueType JSON value type, applied to each item for multiple fields
 * @param multiple whether the stored value is an array
 * @param required whether owner completion requires a stored value
 * @param eventScopeId event ID for a scoped definition, or {@code null} for a global definition
 * @param configuration validation constraints as a JSON object, or {@code null} for defaults
 */
public record FieldDefinitionData(String key, String label, FieldEntityType entityType,
        FieldValueType valueType, boolean multiple, boolean required, UUID eventScopeId,
        JsonNode configuration) {

    /**
     * Starts a definition with global scope, one optional value, and default constraints.
     *
     * @param entityType type of owner the definition applies to
     * @param key stable definition key
     * @param label display label
     * @param valueType JSON value type
     * @return a builder for the definition input
     */
    public static Builder builder(FieldEntityType entityType, String key, String label, FieldValueType valueType) {
        return new Builder(entityType, key, label, valueType);
    }

    /** Adds options to a definition before it is registered. */
    public static final class Builder {
        private final FieldEntityType entityType;
        private final String key;
        private final String label;
        private final FieldValueType valueType;
        private boolean multiple;
        private boolean required;
        private UUID eventScopeId;
        private JsonNode configuration;

        private Builder(FieldEntityType entityType, String key, String label, FieldValueType valueType) {
            this.entityType = entityType;
            this.key = key;
            this.label = label;
            this.valueType = valueType;
        }

        /**
         * Stores values as an array of the selected value type.
         *
         * @return this builder
         */
        public Builder multiple() {
            multiple = true;
            return this;
        }

        /**
         * Requires a value when the owner's workflow checks for completion.
         *
         * @return this builder
         */
        public Builder required() {
            required = true;
            return this;
        }

        /**
         * Sets the event scope of the definition.
         *
         * @param eventId non-null event ID
         * @return this builder
         * @throws NullPointerException if the event ID is null
         */
        public Builder forEvent(UUID eventId) {
            eventScopeId = Objects.requireNonNull(eventId, "eventId");
            return this;
        }

        /**
         * Sets the validation constraints.
         *
         * @param configuration JSON object, or {@code null} for defaults
         * @return this builder
         */
        public Builder configuration(JsonNode configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Creates the input for {@link CustomFields#createDefinition(FieldDefinitionData)}.
         *
         * @return the definition input
         */
        public FieldDefinitionData build() {
            return new FieldDefinitionData(key, label, entityType, valueType, multiple, required, eventScopeId, configuration);
        }
    }
}
