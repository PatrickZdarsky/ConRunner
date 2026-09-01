package at.control.shiftcontrol.shift.plan;

import java.util.UUID;

import at.control.shiftcontrol.shift.error.ShiftAccessDeniedException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PlanAccessService implements PlanAccess {
    private final CurrentShiftActor currentShiftActor;
    private final PlanMembershipRepository membershipRepository;

    @Override
    public CurrentShiftActor.ShiftActor currentActor() {
        return currentShiftActor.current();
    }

    @Override
    public void requireMember(UUID planId) {
        CurrentShiftActor.ShiftActor actor = currentActor();
        if (actor.platformAdmin()) {
            return;
        }
        if (!membershipRepository.existsByPlanIdAndUserId(planId, actor.userId())) {
            throw new ShiftAccessDeniedException("Actor is not a member of this shift plan");
        }
    }

    @Override
    public void requirePlanner(UUID planId) {
        CurrentShiftActor.ShiftActor actor = currentActor();
        if (actor.platformAdmin()) {
            return;
        }
        if (!membershipRepository.existsByPlanIdAndUserIdAndPlannerTrue(planId, actor.userId())) {
            throw new ShiftAccessDeniedException("Actor is not a planner of this shift plan");
        }
    }

    @Override
    public void requirePlatformAdmin() {
        if (!currentActor().platformAdmin()) {
            throw new ShiftAccessDeniedException("Platform administrator access is required");
        }
    }
}
