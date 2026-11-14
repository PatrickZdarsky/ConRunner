package at.conrunner.shift.customfields;

import at.conrunner.platform.customfields.api.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Registers custom-field storage for the UUID-backed shift model. */
@Configuration
public class ShiftFieldStorage {
    /** Resolves the scope from the shift plan and locks both rows for writes. */
    @Bean FieldOwnerStore shiftFields(JdbcTemplate jdbc) {
        return new JdbcFieldOwnerStore(FieldEntityType.SHIFT, jdbc, "shift_field_value",
                "select p.event_id from shift_shift s join shift_plan p on p.id = s.plan_id where s.id = ?",
                " for update of s, p");
    }
}
