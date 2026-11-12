package at.conrunner.shift.lib.event.events.parts;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.lib.type.AssignmentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentPart {
    private String volunteerId;
    private AssignmentStatus status;

    private PositionSlotPart positionSlot;

    @NonNull
    public static AssignmentPart of(@NonNull Assignment assignment) {
        return AssignmentPart.builder()
            .volunteerId(assignment.getAssignedVolunteer().getId().toString())
            .status(assignment.getStatus())
            .positionSlot(PositionSlotPart.of(assignment.getPositionSlot()))
            .build();
    }

    @NonNull
    public static Collection<AssignmentPart> of(@NonNull Collection<Assignment> assignments) {
        return assignments.stream().map(AssignmentPart::of).toList();
    }
}
