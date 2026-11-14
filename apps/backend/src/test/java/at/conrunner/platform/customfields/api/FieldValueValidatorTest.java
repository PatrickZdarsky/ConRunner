package at.conrunner.platform.customfields.api;

import at.conrunner.platform.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class FieldValueValidatorTest {
    final ObjectMapper json = new ObjectMapper();
    FieldDefinition field(FieldValueType type, boolean multiple) {
        return new FieldDefinition(UUID.randomUUID(), "test", "Test", FieldEntityType.EVENT, type,
                multiple, false, null, false, json.createObjectNode(), 0);
    }
    void validate(FieldValueType type, boolean multiple, String value) throws Exception {
        FieldValueValidator.validate(field(type, multiple), json.readTree(value), UUID.randomUUID(), null);
    }
    @Test void enforcesTypesAndCardinality() throws Exception {
        validate(FieldValueType.TEXT, true, "[\"one\",\"two\"]");
        validate(FieldValueType.NUMBER, false, "12.5");
        validate(FieldValueType.DATE, false, "\"2026-11-13\"");
        validate(FieldValueType.DATETIME, false, "\"2026-11-13T10:00:00+01:00\"");
        assertThatThrownBy(() -> validate(FieldValueType.NUMBER, false, "\"12\"")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validate(FieldValueType.TEXT, false, "[]")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validate(FieldValueType.TEXT, true, "[null]")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validate(FieldValueType.DATE, false, "\"2026-02-30\"")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validate(FieldValueType.DATETIME, false, "\"2026-11-13T10:00:00\"")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validate(FieldValueType.URL, false, "\"javascript:alert(1)\"")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validate(FieldValueType.LINK, false, "{\"url\":\"https://example.org\"}")).isInstanceOf(ValidationException.class);
    }
    @Test void validatesConfigurationAndBounds() throws Exception {
        var config = json.readTree("{\"min\":1,\"max\":5}");
        FieldValueValidator.validateConfiguration(FieldValueType.NUMBER, false, config);
        var field = new FieldDefinition(UUID.randomUUID(), "test", "Test", FieldEntityType.EVENT,
                FieldValueType.NUMBER, false, false, null, false, config, 0);
        assertThatThrownBy(() -> FieldValueValidator.validate(field, json.valueToTree(6), null, null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> FieldValueValidator.validateConfiguration(FieldValueType.TEXT, false, config)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> FieldValueValidator.validateConfiguration(FieldValueType.TEXT, false, json.readTree("{\"unknown\":1}"))).isInstanceOf(ValidationException.class);
    }
}
