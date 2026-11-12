package at.conrunner.shift.service.positionslot;

import java.util.UUID;
import java.util.Collection;

import jakarta.validation.Valid;

import at.conrunner.shift.dto.assignment.AssignmentAssignDto;
import at.conrunner.shift.dto.assignment.AssignmentDto;
import at.conrunner.shift.dto.plannerdashboard.AssignmentFilterDto;
import at.conrunner.shift.dto.plannerdashboard.AssignmentPlannerInfoDto;
import at.conrunner.shift.dto.userprofile.VolunteerDto;

public interface PlannerPositionSlotService {
    Collection<AssignmentPlannerInfoDto> getSlots(long shiftPlanId, @Valid AssignmentFilterDto filterDto);

    void acceptRequest(long shiftPlanId, long positionSlotId, UUID userId);

    void declineRequest(long shiftPlanId, long positionSlotId, UUID userId);

    Collection<VolunteerDto> getAssignableUsers(String positionSlotId);

    Collection<AssignmentDto> assignUsersToSlot(AssignmentAssignDto assignmentAssignDto);

    void unAssignUsersFromSlot(AssignmentAssignDto assignmentAssignDto);
}
