package at.conrunner.shift.dto.rows;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanVolunteerIdRow {
    private long planId;
    private String planName;
    private java.util.UUID volunteerId;
}
