package at.conrunner.platform.customfields.internal;

import at.conrunner.platform.customfields.api.*;
import at.conrunner.platform.exception.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * Stores definitions centrally and delegates value persistence to the store for each owner type.
 * Definition and value writes participate in the enclosing Spring transaction when present.
 */
@Service
@Transactional(readOnly = true)
class CustomFieldService implements CustomFields {
    private final DefinitionRepository definitions;
    private final Map<FieldEntityType, FieldOwnerStore> stores;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    CustomFieldService(DefinitionRepository definitions, List<FieldOwnerStore> stores, JdbcTemplate jdbc) {
        this.definitions = definitions;
        this.jdbc = jdbc;
        this.stores = new EnumMap<>(FieldEntityType.class);
        for (var store : stores) {
            if (this.stores.put(store.entityType(), store) != null) throw new java.lang.IllegalStateException("Duplicate field owner store");
        }
    }

    @Override @Transactional
    public FieldDefinition createDefinition(FieldDefinitionData data) {
        if (data == null || data.entityType() == null || data.valueType() == null || data.key() == null
                || !data.key().matches("[a-z][a-z0-9_]{0,63}")) throw new ValidationException("Invalid field definition");
        label(data.label());
        JsonNode config = data.configuration() == null ? json.createObjectNode() : data.configuration();
        FieldValueValidator.validateConfiguration(data.valueType(), data.multiple(), config);
        if (data.eventScopeId() != null) store(FieldEntityType.EVENT).eventId(data.eventScopeId(), false);
        // Lock by type and key across every scope so global registration also serializes with scoped registration.
        jdbc.queryForList("select pg_advisory_xact_lock(hashtext(?))", data.entityType() + ":" + data.key());
        var applicable = definitions.applicable(data.entityType(), data.eventScopeId());
        boolean duplicate = applicable.stream().anyMatch(d -> d.getKey().equals(data.key()));
        if (data.eventScopeId() == null) duplicate = definitions.existsByEntityTypeAndKey(data.entityType(), data.key());
        if (duplicate) throw new ConflictException("Field key already exists in this scope");
        var entity = new DefinitionEntity();
        entity.setId(UUID.randomUUID());
        entity.setKey(data.key());
        entity.setLabel(data.label().trim());
        entity.setEntityType(data.entityType());
        entity.setValueType(data.valueType());
        entity.setMultiple(data.multiple());
        entity.setRequired(data.required());
        entity.setEventScopeId(data.eventScopeId());
        entity.setConfiguration(config.toString());
        return dto(definitions.saveAndFlush(entity));
    }

    @Override @Transactional
    public FieldDefinition updateLabel(UUID id, long expectedVersion, String label) {
        label(label);
        var entity = definitions.findById(id).orElseThrow(() -> new NotFoundException("Field definition not found"));
        if (entity.getVersion() != expectedVersion) throw new ConflictException("Field definition changed");
        entity.setLabel(label.trim());
        return dto(definitions.saveAndFlush(entity));
    }

    @Override
    public List<FieldDefinition> definitions(FieldEntityType type, UUID eventId) {
        return definitions.applicable(type, eventId).stream().map(this::dto).toList();
    }

    @Override
    public Map<String, JsonNode> values(FieldEntityType type, UUID ownerId) {
        var store = store(type);
        UUID eventId = store.eventId(ownerId, false);
        var values = store.read(ownerId);
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (var definition : definitions(type, eventId)) {
            String value = values.get(definition.id());
            if (value != null) result.put(definition.key(), parse(value));
        }
        return Collections.unmodifiableMap(result);
    }

    @Override @Transactional
    public void put(FieldEntityType type, UUID ownerId, String key, JsonNode value, FieldReferenceAccess references) {
        var store = store(type);
        UUID eventId = store.eventId(ownerId, true);
        var definition = requireDefinition(type, eventId, key);
        FieldValueValidator.validate(definition, value, eventId, references);
        store.put(ownerId, definition.id(), value.toString());
    }

    @Override @Transactional
    public void remove(FieldEntityType type, UUID ownerId, String key) {
        var store = store(type);
        UUID eventId = store.eventId(ownerId, true);
        var definition = requireDefinition(type, eventId, key);
        if (definition.required()) throw new ValidationException("Required field cannot be removed");
        store.remove(ownerId, definition.id());
    }

    @Override
    public void validateRequired(FieldEntityType type, UUID ownerId) {
        var store = store(type);
        UUID eventId = store.eventId(ownerId, false);
        var values = store.read(ownerId);
        for (var definition : definitions(type, eventId)) {
            if (definition.required() && !values.containsKey(definition.id()))
                throw new ValidationException("Missing required field: " + definition.key());
        }
    }

    private FieldDefinition requireDefinition(FieldEntityType type, UUID eventId, String key) {
        return definitions(type, eventId).stream().filter(d -> d.key().equals(key)).findFirst()
                .orElseThrow(() -> new NotFoundException("Field definition not found for owner"));
    }
    private FieldOwnerStore store(FieldEntityType type) {
        var store = stores.get(type);
        if (store == null) throw new ValidationException("Unsupported field owner type");
        return store;
    }
    private void label(String label) {
        if (label == null || label.isBlank() || label.length() > 255) throw new ValidationException("Invalid field label");
    }
    private JsonNode parse(String value) {
        try { return json.readTree(value); }
        catch (Exception e) { throw new java.lang.IllegalStateException("Invalid stored field JSON", e); }
    }
    private FieldDefinition dto(DefinitionEntity d) {
        return new FieldDefinition(d.getId(), d.getKey(), d.getLabel(), d.getEntityType(), d.getValueType(), d.isMultiple(),
                d.isRequired(), d.getEventScopeId(), d.isBuiltIn(), parse(d.getConfiguration()), d.getVersion());
    }
}
