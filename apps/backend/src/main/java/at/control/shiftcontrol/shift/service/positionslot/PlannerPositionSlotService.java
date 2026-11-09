package at.control.shiftcontrol.shift.service.positionslot;

import java.util.Collection;

import jakarta.validation.Valid;

import at.shiftcontrol.shiftservice.dto.assignment.AssignmentAssignDto;
import at.shiftcontrol.shiftservice.dto.assignment.AssignmentDto;
import at.shiftcontrol.shiftservice.dto.plannerdashboard.AssignmentFilterDto;
import at.shiftcontrol.shiftservice.dto.plannerdashboard.AssignmentPlannerInfoDto;
import at.shiftcontrol.shiftservice.dto.userprofile.VolunteerDto;

public interface PlannerPositionSlotService {
    Collection<AssignmentPlannerInfoDto> getSlots(long shiftPlanId, @Valid AssignmentFilterDto filterDto);

    void acceptRequest(long shiftPlanId, long positionSlotId, String userId);

    void declineRequest(long shiftPlanId, long positionSlotId, String userId);

    Collection<VolunteerDto> getAssignableUsers(String positionSlotId);

    Collection<AssignmentDto> assignUsersToSlot(AssignmentAssignDto assignmentAssignDto);

    void unAssignUsersFromSlot(AssignmentAssignDto assignmentAssignDto);
}
