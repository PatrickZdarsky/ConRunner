package at.conrunner.schedule.customfields;

import at.conrunner.platform.customfields.api.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Registers custom-field stores for schedule records backed by the event catalog. */
@Configuration
public class ScheduleFieldStorage {
    /** Uses the event owner ID as its scope. */
    @Bean FieldOwnerStore eventFields(JdbcTemplate jdbc) {
        return new JdbcFieldOwnerStore(FieldEntityType.EVENT, jdbc, "schedule_event_field_value",
                "select id from eventcatalog_event where id = ?", " for update");
    }
    /** Resolves the scope from the session's parent event. */
    @Bean FieldOwnerStore sessionFields(JdbcTemplate jdbc) {
        return new JdbcFieldOwnerStore(FieldEntityType.SESSION, jdbc, "schedule_session_field_value",
                "select event_id from eventcatalog_session where id = ?", " for update");
    }
    /** Resolves the scope from the location's parent event. */
    @Bean FieldOwnerStore locationFields(JdbcTemplate jdbc) {
        return new JdbcFieldOwnerStore(FieldEntityType.LOCATION, jdbc, "schedule_location_field_value",
                "select event_id from eventcatalog_location where id = ?", " for update");
    }
}
