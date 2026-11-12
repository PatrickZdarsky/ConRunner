package at.conrunner.shift.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.lib.type.TimeConstraintType;
import at.conrunner.shift.dao.TimeConstraintDao;
import at.conrunner.shift.dto.assignment.AssignmentContextDto;
import at.conrunner.shift.dto.assignment.AssignmentDto;
import at.conrunner.identity.User;
import at.conrunner.identity.UserService;

@RequiredArgsConstructor
@Service
public class AssignmentAssemblingMapper {
    private final UserService userService;
    private final PositionSlotContextAssemblingMapper positionSlotContextAssemblingMapper;
    private final TimeConstraintDao timeConstraintDao;

    public AssignmentDto assemble(@NonNull Assignment assignment) {
        return assemble(assignment, userService.getUser(assignment.getAssignedVolunteer().getId()));
    }

    public Collection<AssignmentDto> assemble(Collection<Assignment> assignments) {
        if (assignments == null) {
            return List.of();
        }

        Map<java.util.UUID, User> usersById = userService.getUsers(
            assignments.stream()
                .map(Assignment::getAssignedVolunteer)
                .map(volunteer -> volunteer.getId())
                .distinct()
                .toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return assignments.stream()
            .map(assignment -> assemble(
                assignment,
                usersById.getOrDefault(
                    assignment.getAssignedVolunteer().getId(),
                    fallbackUser(assignment.getAssignedVolunteer().getId())
                )
            ))
            .toList();
    }

    private AssignmentDto assemble(@NonNull Assignment assignment, User user) {
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

    private User fallbackUser(java.util.UUID userId) {
        return User.builder().id(userId).username(userId.toString()).email("").build();
    }
}
