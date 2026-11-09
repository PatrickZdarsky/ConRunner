package at.control.shiftcontrol.shift.util;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.lib.exception.StateViolationException;
import at.control.shiftcontrol.shift.lib.type.LockStatus;

public class LockStatusHelper {
    //     --------------------- LOCKED ---------------------

    public static boolean isLocked(ShiftPlan shiftPlan) {
        return shiftPlan.getLockStatus() == LockStatus.LOCKED;
    }

    public static boolean isLocked(Shift shift) {
        return isLocked(shift.getShiftPlan());
    }

    public static boolean isLocked(PositionSlot positionSlot) {
        return isLocked(positionSlot.getShift());
    }

    public static boolean isLocked(Assignment assignment) {
        return isLocked(assignment.getPositionSlot());
    }

    public static boolean isLocked(AssignmentSwitchRequest switchRequest) {
        return isLocked(switchRequest.getOfferingAssignment())
            || isLocked(switchRequest.getRequestedAssignment());
    }

    //     --------------------- SUPERVISED ---------------------

    public static boolean isSupervised(ShiftPlan shiftPlan) {
        return shiftPlan.getLockStatus() == LockStatus.SUPERVISED;
    }

    public static boolean isSupervised(Shift shift) {
        return isSupervised(shift.getShiftPlan());
    }

    public static boolean isSupervised(PositionSlot positionSlot) {
        return isSupervised(positionSlot.getShift());
    }

    public static boolean isSupervised(Assignment assignment) {
        return isSupervised(assignment.getPositionSlot());
    }

    public static boolean isSupervised(AssignmentSwitchRequest switchRequest) {
        return isSupervised(switchRequest.getOfferingAssignment())
            || isSupervised(switchRequest.getRequestedAssignment());
    }

    public static void assertIsSupervisedWithMessage(ShiftPlan shiftPlan, String triedAction) {
        if (isLocked(shiftPlan)) {
            throw new StateViolationException(triedAction + " not possible, shift plan is locked");
        }
        if (isSelfSignup(shiftPlan)) {
            throw new StateViolationException(triedAction + " not possible, shift plan is for self signup");
        }
    }

    public static void assertIsSupervisedWithMessage(Shift shift, String triedAction) {
        assertIsSupervisedWithMessage(shift.getShiftPlan(), triedAction);
    }

    public static void assertIsSupervisedWithMessage(PositionSlot positionSlot, String triedAction) {
        assertIsSupervisedWithMessage(positionSlot.getShift(), triedAction);
    }

    public static void assertIsSupervisedWithMessage(Assignment assignment, String triedAction) {
        assertIsSupervisedWithMessage(assignment.getPositionSlot(), triedAction);
    }

    //     --------------------- SELF_SIGNUP ---------------------

    public static boolean isSelfSignup(ShiftPlan shiftPlan) {
        return shiftPlan.getLockStatus() == LockStatus.SELF_SIGNUP;
    }

    public static boolean isSelfSignup(Shift shift) {
        return isSelfSignup(shift.getShiftPlan());
    }

    public static boolean isSelfSignup(PositionSlot positionSlot) {
        return isSelfSignup(positionSlot.getShift());
    }

    public static boolean isSelfSignup(Assignment assignment) {
        return isSelfSignup(assignment.getPositionSlot());
    }

    public static void assertIsSelfSignUpWithMessage(ShiftPlan shiftPlan, String triedAction) {
        if (isLocked(shiftPlan)) {
            throw new StateViolationException(triedAction + " not possible, shift plan is locked");
        }
        if (isSupervised(shiftPlan)) {
            throw new StateViolationException(triedAction + " not possible, shift plan is supervised");
        }
    }

    public static void assertIsSelfSignUpWithMessage(Shift shift, String triedAction) {
        assertIsSelfSignUpWithMessage(shift.getShiftPlan(), triedAction);
    }

    public static void assertIsSelfSignUpWithMessage(PositionSlot positionSlot, String triedAction) {
        assertIsSelfSignUpWithMessage(positionSlot.getShift(), triedAction);
    }

    public static void assertIsSelfSignUpWithMessage(Assignment assignment, String triedAction) {
        assertIsSelfSignUpWithMessage(assignment.getPositionSlot(), triedAction);
    }
}
