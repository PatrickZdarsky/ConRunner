package at.control.shiftcontrol.shift.endpoint.plannerdashboard;

import java.util.Collection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.control.shiftcontrol.shift.dto.PaginationDto;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentAssignDto;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentDto;
import at.control.shiftcontrol.shift.dto.plannerdashboard.AssignmentFilterDto;
import at.control.shiftcontrol.shift.dto.plannerdashboard.AssignmentPlannerInfoDto;
import at.control.shiftcontrol.shift.dto.shift.ShiftDto;
import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;
import at.control.shiftcontrol.shift.service.ShiftPlanService;
import at.control.shiftcontrol.shift.service.ShiftService;
import at.control.shiftcontrol.shift.service.positionslot.PlannerPositionSlotService;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/shift-plans/{shiftPlanId}/", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class SignupEndpoint {
    private final PlannerPositionSlotService positionSlotService;
    private final ShiftPlanService shiftPlanService;
    private final ShiftService shiftService;

    @GetMapping("/open-shifts")
    @Operation(
        operationId = "getAllOpenShifts",
        description = "Get all shifts for the shiftplan where positions exist that are not fully filled"
    )
    public PaginationDto<ShiftDto> getAllOpenShifts(@PathVariable String shiftPlanId, @RequestParam int page,  @RequestParam int size) {
        return shiftService.getAllOpenShiftsOfPlanPaginated(ConvertUtil.idToLong(shiftPlanId), page, size);
    }

    @GetMapping("/requests")
    @Operation(
        operationId = "getSlots",
        description = "Get assignments for an Event filtered by the AssignmentStatus"
    )
    public Collection<AssignmentPlannerInfoDto> getSlots(
        @PathVariable String shiftPlanId,
        @RequestParam Collection<AssignmentStatus> statuses
    ) {
        var filterDto = new AssignmentFilterDto();
        filterDto.setStatuses(statuses);
        return positionSlotService.getSlots(ConvertUtil.idToLong(shiftPlanId), filterDto);
    }

    @PostMapping("/requests/{positionSlotId}/accept")
    @Operation(
        operationId = "acceptRequest",
        description = "Accept  the slot request"
    )
    public void acceptRequest(@PathVariable String shiftPlanId, @PathVariable String positionSlotId, String userid) {
        positionSlotService.acceptRequest(ConvertUtil.idToLong(shiftPlanId), ConvertUtil.idToLong(positionSlotId), userid);
    }

    @PostMapping("/requests/{positionSlotId}/decline")
    @Operation(
        operationId = "declineRequest",
        description = "Decline the slot request"
    )
    public void declineRequest(@PathVariable String shiftPlanId, @PathVariable String positionSlotId, String userid) {
        positionSlotService.declineRequest(ConvertUtil.idToLong(shiftPlanId), ConvertUtil.idToLong(positionSlotId), userid);
    }

    @GetMapping("/assignable-users/{positionSlotId}")
    @Operation(
        operationId = "getAssignableUsers",
        description = "Gets all users that could be assigned to the given slot"
    )
    public Collection<VolunteerDto> getAssignableUsers(@PathVariable String shiftPlanId, @PathVariable String positionSlotId) {
        return positionSlotService.getAssignableUsers(positionSlotId);
    }

    @PostMapping("/assign")
    @Operation(
        operationId = "assignUsersToSlot",
        description = "Assigns the given users to the given slot"
    )
    public Collection<AssignmentDto> assignUsersToSlot(@PathVariable String shiftPlanId, @RequestBody AssignmentAssignDto assignmentAssignDto) {
        return positionSlotService.assignUsersToSlot(assignmentAssignDto);
    }

    @PostMapping("/unassign")
    @Operation(
        operationId = "unAssignUsersFromSlot",
        description = "Unassigns the given users from the given slot"
    )
    public void unAssignUsersFromSlot(@PathVariable String shiftPlanId, @RequestBody AssignmentAssignDto assignmentAssignDto) {
        positionSlotService.unAssignUsersFromSlot(assignmentAssignDto);
    }
}
