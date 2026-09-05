package at.control.shiftcontrol.shift.assignment;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.shift.ShiftEntity;
import at.control.shiftcontrol.shift.TimeConstraintRepository;
import at.control.shiftcontrol.shift.plan.PlanMembershipEntity;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanEntity;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Side-effect-free evaluator shared by preflight and locked command paths. */
@Service
@RequiredArgsConstructor
public class AssignmentEligibilityService {
    public static final Set<AssignmentStatus> OCCUPYING_STATUSES =
        EnumSet.of(AssignmentStatus.ACCEPTED, AssignmentStatus.AUCTION, AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN);

    private final PlanMembershipRepository membershipRepository;
    private final AssignmentRepository assignmentRepository;
    private final TimeConstraintRepository timeConstraintRepository;

    public AssignmentEligibility forDirectJoin(PositionSlotEntity slot, ShiftEntity shift, ShiftPlanEntity plan,
                                                UUID volunteerId, AssignmentEntity existingAssignment) {
        return evaluate(slot, shift, plan, volunteerId, existingAssignment, true, true, true, true);
    }

    public AssignmentEligibility forSupervisedJoinRequest(PositionSlotEntity slot, ShiftEntity shift, ShiftPlanEntity plan,
                                                           UUID volunteerId, AssignmentEntity existingAssignment) {
        return evaluate(slot, shift, plan, volunteerId, existingAssignment, false, true, false, true);
    }

    public AssignmentEligibility forPlannerAssignment(PositionSlotEntity slot, ShiftEntity shift, ShiftPlanEntity plan,
                                                       UUID volunteerId, AssignmentEntity existingAssignment) {
        return evaluate(slot, shift, plan, volunteerId, existingAssignment, false, true, true, false);
    }

    public AssignmentEligibility forPlanTransition(PositionSlotEntity slot, ShiftEntity shift, ShiftPlanEntity plan,
                                                   UUID volunteerId) {
        return evaluate(slot, shift, plan, volunteerId, null, false, true, false, false);
    }

    private AssignmentEligibility evaluate(PositionSlotEntity slot, ShiftEntity shift, ShiftPlanEntity plan,
                                            UUID volunteerId, AssignmentEntity existingAssignment,
                                            boolean requireSelfSignup, boolean enforceVolunteerRestriction,
                                            boolean enforceCapacity, boolean rejectOpenRequest) {
        Set<AssignmentEligibilityReason> reasons = EnumSet.noneOf(AssignmentEligibilityReason.class);
        PlanMembershipEntity membership = membershipRepository.findByPlanIdAndUserId(plan.getId(), volunteerId).orElse(null);
        if (membership == null) {
            reasons.add(AssignmentEligibilityReason.NOT_PLAN_MEMBER);
        } else {
            if (enforceVolunteerRestriction && membership.isVolunteerChangesRestricted()) {
                reasons.add(AssignmentEligibilityReason.VOLUNTEER_CHANGES_RESTRICTED);
            }
            if (slot.getRequiredRoleId() != null && !membership.getRoleIds().contains(slot.getRequiredRoleId())) {
                reasons.add(AssignmentEligibilityReason.REQUIRED_ROLE_MISSING);
            }
        }
        StaffingMode effectiveMode = effectiveMode(shift, plan);
        if (requireSelfSignup && effectiveMode != StaffingMode.SELF_SIGNUP) {
            reasons.add(AssignmentEligibilityReason.EFFECTIVE_MODE_DOES_NOT_ALLOW_DIRECT_SIGNUP);
        }
        if (existingAssignment != null) {
            if (OCCUPYING_STATUSES.contains(existingAssignment.getStatus())) {
                reasons.add(AssignmentEligibilityReason.ALREADY_ASSIGNED_TO_SLOT);
            } else if (existingAssignment.getStatus() == AssignmentStatus.REQUEST_FOR_ASSIGNMENT && rejectOpenRequest) {
                reasons.add(AssignmentEligibilityReason.JOIN_REQUEST_ALREADY_EXISTS);
            }
        }
        long occupied = assignmentRepository.countByPositionSlotIdAndStatusIn(slot.getId(), OCCUPYING_STATUSES);
        if (enforceCapacity && occupied >= slot.getDesiredVolunteerCount()) {
            reasons.add(AssignmentEligibilityReason.SLOT_FULL);
        }
        if ((existingAssignment == null || !OCCUPYING_STATUSES.contains(existingAssignment.getStatus()))
            && assignmentRepository.existsOccupiedAssignmentOverlappingShift(volunteerId, shift.getStartTime(), shift.getEndTime(),
            OCCUPYING_STATUSES)) {
            reasons.add(AssignmentEligibilityReason.OVERLAPPING_ASSIGNMENT);
        }
        if (timeConstraintRepository.existsOverlapping(plan.getEventId(), volunteerId, shift.getStartTime(), shift.getEndTime())) {
            reasons.add(AssignmentEligibilityReason.UNAVAILABLE);
        }
        return new AssignmentEligibility(slot.getId(), volunteerId, effectiveMode, Math.toIntExact(occupied),
            slot.getDesiredVolunteerCount(), reasons);
    }

    public static StaffingMode effectiveMode(ShiftEntity shift, ShiftPlanEntity plan) {
        return shift.getStaffingModeOverride() == null ? plan.getDefaultStaffingMode() : shift.getStaffingModeOverride();
    }

    public static boolean onlyOverrideable(Set<AssignmentEligibilityReason> reasons) {
        return !reasons.isEmpty() && reasons.stream().allMatch(reason -> switch (reason) {
            case VOLUNTEER_CHANGES_RESTRICTED, REQUIRED_ROLE_MISSING, OVERLAPPING_ASSIGNMENT, UNAVAILABLE -> true;
            default -> false;
        });
    }
}
