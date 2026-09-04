package at.control.shiftcontrol.shift.assignment;

import java.time.Clock;
import java.util.UUID;

import at.control.shiftcontrol.shift.error.ShiftConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** The sole mutation path for assignment workflow state and its immutable history. */
@Component
@RequiredArgsConstructor
public class AssignmentLifecycle {
    private final StaffingAssignmentRepository assignmentRepository;
    private final AssignmentTransitionRepository transitionRepository;
    private final Clock clock;

    public StaffingAssignmentEntity transition(StaffingAssignmentEntity assignment, AssignmentState nextState,
                                               UUID actorUserId, String reason) {
        AssignmentState previousState = assignment.getState();
        boolean permitted = previousState == null
            ? AssignmentState.mayCreateAs(nextState)
            : previousState.mayTransitionTo(nextState);
        if (!permitted) {
            throw new ShiftConflictException("Invalid assignment state transition from " + previousState + " to " + nextState);
        }
        assignment.setState(nextState);
        assignment.setUpdatedAt(clock.instant());
        assignment = assignmentRepository.save(assignment);

        AssignmentTransitionEntity transition = new AssignmentTransitionEntity();
        transition.setAssignmentId(assignment.getId());
        transition.setFromState(previousState);
        transition.setToState(nextState);
        transition.setActorUserId(actorUserId);
        transition.setReason(reason);
        transition.setOccurredAt(clock.instant());
        transitionRepository.save(transition);
        return assignment;
    }
}
