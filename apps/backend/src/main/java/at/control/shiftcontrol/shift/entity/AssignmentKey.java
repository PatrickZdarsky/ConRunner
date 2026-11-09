package at.control.shiftcontrol.shift.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentKey {
    private long positionSlotId;
    private String volunteerId;

    public static AssignmentKey of(long positionSlotId, String volunteerId) {
        return AssignmentKey.builder()
                .positionSlotId(positionSlotId)
                .volunteerId(volunteerId)
                .build();
    }

    public static AssignmentKey of(Assignment assignment) {
        return AssignmentKey.builder()
                .positionSlotId(assignment.getPositionSlot().getId())
                .volunteerId(assignment.getAssignedVolunteer().getId())
                .build();
    }
}
