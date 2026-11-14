package at.conrunner.platform.customfields.api;

import at.conrunner.platform.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/** Validates JSON shape before it reaches domain storage or a schedule draft. */
public final class FieldValueValidator {
    private FieldValueValidator() {}

    /**
     * Checks the value's JSON shape, size, configured bounds, and any user references.
     * Multiple fields require arrays. An empty array is valid only for optional fields.
     * The serialized value may contain up to 1,000,000 characters.
     *
     * @param field definition with validated configuration
     * @param value scalar value or array according to the definition. JSON null and null items are invalid
     * @param eventId event scope passed to the user reference check
     * @param references domain permission check, required when validating any user reference
     * @throws ValidationException if the supplied value violates a constraint
     */
    public static void validate(FieldDefinition field, JsonNode value, UUID eventId, FieldReferenceAccess references) {
        require(value != null && !value.isNull(), "Use remove to unset an optional field");
        require(value.toString().length() <= 1_000_000, "Field value is too large");
        if (field.multiple()) {
            require(value.isArray(), "Expected an array");
            int max = field.configuration().path("maxItems").asInt(1000);
            require(value.size() <= max, "Too many field values");
            require(!field.required() || !value.isEmpty(), "Required field must have a value");
            for (JsonNode item : value) validateOne(field, item, eventId, references);
        } else {
            validateOne(field, value, eventId, references);
        }
    }

    private static void validateOne(FieldDefinition field, JsonNode value, UUID eventId, FieldReferenceAccess references) {
        require(value != null && !value.isNull(), "Null items are not allowed");
        switch (field.valueType()) {
            case TEXT -> {
                require(value.isTextual(), "Expected text");
                require(value.textValue().length() <= field.configuration().path("maxLength").asInt(100_000), "Text is too long");
                require(!field.required() || !value.textValue().isBlank(), "Required text must not be blank");
            }
            case URL -> url(value);
            case NUMBER -> {
                require(value.isNumber() && finite(value), "Expected a finite number");
                JsonNode min = field.configuration().get("min");
                JsonNode max = field.configuration().get("max");
                require(min == null || value.decimalValue().compareTo(min.decimalValue()) >= 0, "Number is below minimum");
                require(max == null || value.decimalValue().compareTo(max.decimalValue()) <= 0, "Number exceeds maximum");
            }
            case DATE -> {
                text(value);
                try { LocalDate.parse(value.textValue()); }
                catch (RuntimeException e) { throw new ValidationException("Expected an ISO date", e); }
            }
            case DATETIME -> {
                text(value);
                try { OffsetDateTime.parse(value.textValue()); }
                catch (RuntimeException e) { throw new ValidationException("Expected an ISO datetime with offset", e); }
            }
            case USER -> {
                text(value);
                UUID id;
                try { id = UUID.fromString(value.textValue()); }
                catch (RuntimeException e) { throw new ValidationException("Expected a user UUID", e); }
                require(id.toString().equalsIgnoreCase(value.textValue()), "Expected a canonical user UUID");
                require(references != null && references.canReferenceUser(eventId, id), "User reference is unavailable");
            }
            case LINK -> {
                require(value.isObject() && value.size() == 2 && value.has("label") && value.has("url"), "Expected link label and URL");
                text(value.get("label"));
                require(!value.get("label").textValue().isBlank() && value.get("label").textValue().length() <= 255, "Invalid link label");
                url(value.get("url"));
            }
        }
    }

    /**
     * Checks the constraint settings in a definition's configuration object.
     * Text fields accept {@code maxLength} from 1 to 1,000,000 (default 100,000).
     * Multiple fields accept {@code maxItems} from 1 to 1,000 (default 1,000).
     * Number fields accept finite, inclusive {@code min} and {@code max} bounds.
     *
     * @param type value type the constraints apply to
     * @param multiple whether the field accepts an array
     * @param config JSON object containing supported keys. An empty object uses default limits
     * @throws ValidationException if the configuration has an unsupported key, an invalid bound,
     *        or a minimum greater than its maximum
     */
    public static void validateConfiguration(FieldValueType type, boolean multiple, JsonNode config) {
        require(config != null && config.isObject(), "Configuration must be an object");
        var names = config.fieldNames();
        while (names.hasNext()) {
            String key = names.next();
            JsonNode value = config.get(key);
            switch (key) {
                case "maxLength" -> require(type == FieldValueType.TEXT && value.canConvertToInt() && value.isIntegralNumber() && value.intValue() > 0 && value.intValue() <= 1_000_000, "Invalid maxLength");
                case "maxItems" -> require(multiple && value.isIntegralNumber() && value.canConvertToInt() && value.intValue() > 0 && value.intValue() <= 1000, "Invalid maxItems");
                case "min", "max" -> require(type == FieldValueType.NUMBER && value.isNumber() && finite(value), "Invalid number bound");
                default -> throw new ValidationException("Unknown field configuration: " + key);
            }
        }
        require(!config.has("min") || !config.has("max") || config.get("min").decimalValue().compareTo(config.get("max").decimalValue()) <= 0, "Minimum exceeds maximum");
    }

    private static boolean finite(JsonNode value) {
        return !(value.isDouble() || value.isFloat()) || Double.isFinite(value.doubleValue());
    }

    private static void text(JsonNode value) { require(value != null && value.isTextual(), "Expected text"); }
    private static void url(JsonNode value) {
        text(value);
        try {
            URI uri = URI.create(value.textValue());
            require(value.textValue().length() <= 2048 && uri.getHost() != null && uri.getUserInfo() == null && Set.of("https", "http").contains(uri.getScheme()), "Expected an absolute HTTP(S) URL without credentials");
        } catch (IllegalArgumentException e) { throw new ValidationException("Invalid URL", e); }
    }
    private static void require(boolean valid, String message) { if (!valid) throw new ValidationException(message); }
}
