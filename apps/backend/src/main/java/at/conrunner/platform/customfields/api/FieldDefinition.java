package at.conrunner.platform.customfields.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

/**
 * Definition of a custom field as returned by {@link CustomFields}.
 *
 * @param id persistent definition ID used in value storage
 * @param key stable key used by domain callers
 * @param label display label
 * @param entityType type of owner the definition applies to
 * @param valueType JSON value type, applied to each item for multiple fields
 * @param multiple whether the stored value is an array
 * @param required whether owner completion requires a stored value
 * @param eventScopeId event ID for a scoped definition, or {@code null} for a global definition
 * @param builtIn whether the definition was seeded as a built-in field
 * @param configuration validation constraints as a JSON object
 * @param version optimistic-lock version used when updating the label
 */
public record FieldDefinition(UUID id, String key, String label, FieldEntityType entityType,
        FieldValueType valueType, boolean multiple, boolean required, UUID eventScopeId,
        boolean builtIn, JsonNode configuration, long version) {}
