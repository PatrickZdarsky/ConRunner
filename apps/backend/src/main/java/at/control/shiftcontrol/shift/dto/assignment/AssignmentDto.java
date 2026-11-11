package at.control.shiftcontrol.shift.dto.assignment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentDto {
    @NotNull
    private String id;

    @NotNull
    private String positionSlotId;

    @NotNull
    @Valid
    private VolunteerDto assignedVolunteer;

    @NotNull
    private AssignmentStatus status;

    @NotNull
    @Min(0)
    private int acceptedRewardPoints;

    /**
     * Indicates whether the assigned volunteer is unable to attend the assigned shift.
     * This is set based on the emergency TimeConstraints of the volunteer.
     */
    @NotNull
    private boolean unableToAttend;
}
