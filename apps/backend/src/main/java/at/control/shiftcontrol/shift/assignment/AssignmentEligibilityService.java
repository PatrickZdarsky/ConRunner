package at.control.shiftcontrol.shift.assignment;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.shift.availability.AvailabilityRepository;
import at.control.shiftcontrol.shift.plan.PlanMembershipEntity;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanEntity;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Shared, side-effect-free eligibility evaluator. Mutation commands hold the plan, volunteer, and
 * slot locks before calling it; preflight callers may use its result as advisory only.
 */
@Service
@RequiredArgsConstructor
public class AssignmentEligibilityService {
    public static final Set<AssignmentState> OCCUPYING_STATES = EnumSet.of(AssignmentState.ACCEPTED, AssignmentState.LEAVE_REQUESTED);

    private final PlanMembershipRepository membershipRepository;
    private final StaffingAssignmentRepository assignmentRepository;
    private final AvailabilityRepository availabilityRepository;

    public AssignmentEligibility forDirectJoin(PositionSlotEntity slot, StaffingShiftEntity staffingShift,
                                                ShiftPlanEntity plan, UUID volunteerId,
                                                StaffingAssignmentEntity existingAssignment) {
        return evaluate(slot, staffingShift, plan, volunteerId, existingAssignment, true, true, true, true);
    }

    public AssignmentEligibility forSupervisedJoinRequest(PositionSlotEntity slot, StaffingShiftEntity staffingShift,
                                                           ShiftPlanEntity plan, UUID volunteerId,
                                                           StaffingAssignmentEntity existingAssignment) {
        return evaluate(slot, staffingShift, plan, volunteerId, existingAssignment, false, true, false, true);
    }

    public AssignmentEligibility forPlannerAssignment(PositionSlotEntity slot, StaffingShiftEntity staffingShift,
                                                       ShiftPlanEntity plan, UUID volunteerId,
                                                       StaffingAssignmentEntity existingAssignment) {
        return evaluate(slot, staffingShift, plan, volunteerId, existingAssignment, false, true, true, false);
    }

    /** Used by plan transitions after request-state and slot locks have been obtained. */
    public AssignmentEligibility forPlanTransition(PositionSlotEntity slot, StaffingShiftEntity staffingShift,
                                                   ShiftPlanEntity plan, UUID volunteerId,
                                                   StaffingAssignmentEntity existingAssignment) {
        return evaluate(slot, staffingShift, plan, volunteerId, existingAssignment, false, true, false, false);
    }

    private AssignmentEligibility evaluate(PositionSlotEntity slot, StaffingShiftEntity staffingShift, ShiftPlanEntity plan,
                                            UUID volunteerId, StaffingAssignmentEntity existingAssignment,
                                            boolean requireSelfSignup, boolean enforceVolunteerRestriction,
                                            boolean enforceCapacity, boolean rejectOpenJoinRequest) {
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

        StaffingMode effectiveMode = effectiveMode(staffingShift, plan);
        if (requireSelfSignup && effectiveMode != StaffingMode.SELF_SIGNUP) {
            reasons.add(AssignmentEligibilityReason.EFFECTIVE_MODE_DOES_NOT_ALLOW_DIRECT_SIGNUP);
        }
        if (existingAssignment != null) {
            switch (existingAssignment.getState()) {
                case ACCEPTED, LEAVE_REQUESTED -> reasons.add(AssignmentEligibilityReason.ALREADY_ASSIGNED_TO_SLOT);
                case JOIN_REQUESTED -> {
                    if (rejectOpenJoinRequest) {
                        reasons.add(AssignmentEligibilityReason.JOIN_REQUEST_ALREADY_EXISTS);
                    }
                }
                default -> {
                    // A final workflow state may be reused, keeping a complete transition history.
                }
            }
        }

        long occupied = assignmentRepository.countByPositionSlotIdAndStateIn(slot.getId(), OCCUPYING_STATES);
        if (enforceCapacity && occupied >= slot.getDesiredVolunteerCount()) {
            reasons.add(AssignmentEligibilityReason.SLOT_FULL);
        }
        if (assignmentRepository.existsOccupiedAssignmentOverlappingShift(volunteerId, staffingShift.getStartTime(),
            staffingShift.getEndTime(), OCCUPYING_STATES)) {
            reasons.add(AssignmentEligibilityReason.OVERLAPPING_ASSIGNMENT);
        }
        if (availabilityRepository.existsOverlapping(plan.getEventId(), volunteerId, staffingShift.getStartTime(),
            staffingShift.getEndTime())) {
            reasons.add(AssignmentEligibilityReason.UNAVAILABLE);
        }

        return new AssignmentEligibility(slot.getId(), volunteerId, effectiveMode, Math.toIntExact(occupied),
            slot.getDesiredVolunteerCount(), reasons);
    }

    public static StaffingMode effectiveMode(StaffingShiftEntity staffingShift, ShiftPlanEntity plan) {
        return staffingShift.getStaffingModeOverride() == null
            ? plan.getDefaultStaffingMode()
            : staffingShift.getStaffingModeOverride();
    }

    public static boolean onlyOverrideable(Set<AssignmentEligibilityReason> reasons) {
        return !reasons.isEmpty() && reasons.stream().allMatch(reason -> switch (reason) {
            case VOLUNTEER_CHANGES_RESTRICTED, REQUIRED_ROLE_MISSING, OVERLAPPING_ASSIGNMENT, UNAVAILABLE -> true;
            default -> false;
        });
    }
}
