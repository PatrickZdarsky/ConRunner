package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.lib.type.TimeConstraintType;
import at.control.shiftcontrol.shift.dao.TimeConstraintDao;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentContextDto;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentDto;
import at.control.shiftcontrol.shift.userdirectory.UserInfo;
import at.control.shiftcontrol.shift.userdirectory.UserLookupService;

@RequiredArgsConstructor
@Service
public class AssignmentAssemblingMapper {
    private final UserLookupService userDirectoryService;
    private final PositionSlotContextAssemblingMapper positionSlotContextAssemblingMapper;
    private final TimeConstraintDao timeConstraintDao;

    public AssignmentDto assemble(@NonNull Assignment assignment) {
        return assemble(assignment, userDirectoryService.getUserById(assignment.getAssignedVolunteer().getId()));
    }

    public Collection<AssignmentDto> assemble(Collection<Assignment> assignments) {
        if (assignments == null) {
            return List.of();
        }

        Map<String, UserInfo> usersById = userDirectoryService.getUserByIds(
            assignments.stream()
                .map(Assignment::getAssignedVolunteer)
                .map(volunteer -> volunteer.getId())
                .distinct()
                .toList()
        ).stream().collect(Collectors.toMap(UserInfo::id, Function.identity()));

        return assignments.stream()
            .map(assignment -> assemble(
                assignment,
                usersById.getOrDefault(
                    assignment.getAssignedVolunteer().getId(),
                    fallbackUserInfo(assignment.getAssignedVolunteer().getId())
                )
            ))
            .toList();
    }

    private AssignmentDto assemble(@NonNull Assignment assignment, UserInfo user) {
        var hasEmergencyConstraint = timeConstraintDao.findByAssignmentIdAndType(assignment.getId(), TimeConstraintType.EMERGENCY)
            .isPresent();

        return new AssignmentDto(
            String.valueOf(assignment.getId()),
            String.valueOf(assignment.getPositionSlot().getId()),
            VolunteerAssemblingMapper.toDtoFromUser(user),
            assignment.getStatus(),
            assignment.getAcceptedRewardPoints(),
            hasEmergencyConstraint);
    }

    public static Assignment shallowCopy(@NonNull Assignment oldAssignment) {
        return Assignment.builder()
            .positionSlot(oldAssignment.getPositionSlot())
            .assignedVolunteer(oldAssignment.getAssignedVolunteer())
            .status(oldAssignment.getStatus())
            .outgoingSwitchRequests(oldAssignment.getOutgoingSwitchRequests())
            .incomingSwitchRequests(oldAssignment.getIncomingSwitchRequests())
            .acceptedRewardPoints(oldAssignment.getAcceptedRewardPoints())
            .build();
    }

    public AssignmentContextDto toContextDto(@NonNull Assignment assignment) {
        return new AssignmentContextDto(
            assemble(assignment),
            ShiftAssemblingMapper.toContextDto(assignment.getPositionSlot().getShift()),
            positionSlotContextAssemblingMapper.toContextDto(assignment.getPositionSlot())
        );
    }

    public Collection<AssignmentContextDto> toContextDto(Collection<Assignment> assignments) {
        if (assignments == null) {
            return List.of();
        }
        return assignments.stream().map(this::toContextDto).toList();
    }

    private UserInfo fallbackUserInfo(String userId) {
        return new UserInfo(userId, userId, "", "", "", null, false);
    }
}
