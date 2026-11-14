package at.conrunner.platform.customfields.internal;

import at.conrunner.TestcontainersConfiguration;
import at.conrunner.platform.customfields.api.*;
import at.conrunner.platform.exception.*;
import at.conrunner.schedule.customfields.ScheduleFieldStorage;
import at.conrunner.shift.customfields.ShiftFieldStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml",
    "spring.jpa.hibernate.ddl-auto=none"
})
@Import({TestcontainersConfiguration.class, CustomFieldService.class, ScheduleFieldStorage.class, ShiftFieldStorage.class})
class CustomFieldPersistenceTest {
    @Autowired CustomFields fields;
    @Autowired JdbcTemplate jdbc;
    final ObjectMapper json = new ObjectMapper();

    UUID event() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into eventcatalog_event(id,name,start_time,end_time,version) values (?, ?, now(), now() + interval '1 day', 0)", id, id.toString());
        return id;
    }

    @Test void builtInsStoreLongDescriptionsAndStructuredLinks() throws Exception {
        UUID id = event();
        var eventFields = CustomFields.forOwner(fields, FieldEntityType.EVENT, id);
        eventFields.putText("description", "x".repeat(5000));
        var links = json.readTree("[{\"label\":\"Website\",\"url\":\"https://example.org\"}]");
        eventFields.put("social_links", links);
        assertThat(eventFields.values().get("description").asText()).hasSize(5000);
        assertThat(eventFields.values().get("social_links")).isEqualTo(links);
        eventFields.putText("description", "edited");
        assertThat(jdbc.queryForObject("select count(*) from schedule_event_field_value where owner_id = ?", Integer.class, id)).isEqualTo(2);
        eventFields.remove("description");
        assertThat(eventFields.values()).doesNotContainKey("description");
    }

    @Test void scopeAndRequiredRulesApplyToTheActualOwner() {
        UUID first = event(); UUID second = event();
        var firstFields = CustomFields.forOwner(fields, FieldEntityType.EVENT, first);
        var secondFields = CustomFields.forOwner(fields, FieldEntityType.EVENT, second);
        fields.createDefinition(FieldDefinitionData.builder(FieldEntityType.EVENT, "capacity", "Capacity", FieldValueType.NUMBER)
                .forEvent(first)
                .required()
                .configuration(json.createObjectNode().put("min", 1))
                .build());
        assertThatThrownBy(firstFields::validateRequired).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> secondFields.put("capacity", json.valueToTree(10))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> firstFields.put("capacity", json.valueToTree(0))).isInstanceOf(ValidationException.class);
        firstFields.put("capacity", json.valueToTree(10));
        firstFields.validateRequired();
        assertThatThrownBy(() -> firstFields.remove("capacity")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> fields.createDefinition(new FieldDefinitionData("description", "Other", FieldEntityType.EVENT, FieldValueType.TEXT, false, false, first, null))).isInstanceOf(ConflictException.class);
    }

    @Test void rejectsStaleDefinitionUpdatesAndUnavailableUsers() {
        UUID event = event();
        var eventFields = CustomFields.forOwner(fields, FieldEntityType.EVENT, event);
        var field = fields.createDefinition(FieldDefinitionData.builder(FieldEntityType.EVENT, "responsible", "Responsible", FieldValueType.USER)
                .multiple()
                .build());
        fields.updateLabel(field.id(), field.version(), "Team");
        assertThatThrownBy(() -> fields.updateLabel(field.id(), field.version(), "Stale")).isInstanceOf(ConflictException.class);
        var users = json.createArrayNode().add(UUID.randomUUID().toString());
        assertThatThrownBy(() -> eventFields.put("responsible", users)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> eventFields.put("responsible", users, (scope, user) -> false)).isInstanceOf(ValidationException.class);
        eventFields.put("responsible", users, (scope, user) -> scope.equals(event));
        assertThat(eventFields.values().get("responsible")).isEqualTo(users);
    }
    @Test void storesValuesForSessionsLocationsAndShifts() {
        UUID event = event(); UUID location = UUID.randomUUID(); UUID session = UUID.randomUUID();
        UUID plan = UUID.randomUUID(); UUID shift = UUID.randomUUID();
        jdbc.update("insert into eventcatalog_location(id,event_id,name,read_only,version) values (?, ?, 'Hall', false, 0)", location, event);
        jdbc.update("insert into eventcatalog_session(id,event_id,name,start_time,end_time,read_only,version) values (?, ?, 'Talk', now(), now() + interval '1 hour', false, 0)", session, event);
        jdbc.update("insert into shift_plan(id,event_id,name,default_staffing_mode,created_at,version) values (?, ?, 'Plan', 'OPEN', now(), 0)", plan, event);
        jdbc.update("insert into shift_shift(id,plan_id,name,start_time,end_time,created_at,version) values (?, ?, 'Shift', now(), now() + interval '1 hour', now(), 0)", shift, plan);
        fields.put(FieldEntityType.LOCATION, location, "description", json.valueToTree("Room details"), null);
        fields.put(FieldEntityType.SESSION, session, "abstract", json.valueToTree("Session abstract"), null);
        fields.put(FieldEntityType.SHIFT, shift, "description", json.valueToTree("Shift details"), null);
        assertThat(fields.values(FieldEntityType.LOCATION, location).get("description").asText()).isEqualTo("Room details");
        assertThat(fields.values(FieldEntityType.SESSION, session).get("abstract").asText()).isEqualTo("Session abstract");
        assertThat(fields.values(FieldEntityType.SHIFT, shift).get("description").asText()).isEqualTo("Shift details");
        jdbc.update("delete from eventcatalog_session where id = ?", session);
        assertThat(jdbc.queryForObject("select count(*) from schedule_session_field_value where owner_id = ?", Integer.class, session)).isZero();
    }
}
