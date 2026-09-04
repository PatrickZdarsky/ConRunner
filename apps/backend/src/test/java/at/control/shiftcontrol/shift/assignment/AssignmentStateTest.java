package at.control.shiftcontrol.shift.assignment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AssignmentStateTest {
    @Test
    void permitsOnlyWorkflowTransitionsAndExplicitReuseOfFinalStates() {
        assertTrue(AssignmentState.mayCreateAs(AssignmentState.JOIN_REQUESTED));
        assertTrue(AssignmentState.JOIN_REQUESTED.mayTransitionTo(AssignmentState.ACCEPTED));
        assertTrue(AssignmentState.LEAVE_REQUESTED.mayTransitionTo(AssignmentState.UNASSIGNED));
        assertTrue(AssignmentState.UNASSIGNED.mayTransitionTo(AssignmentState.JOIN_REQUESTED));
        assertFalse(AssignmentState.ACCEPTED.mayTransitionTo(AssignmentState.JOIN_REQUESTED));
        assertFalse(AssignmentState.JOIN_REQUESTED.mayTransitionTo(AssignmentState.LEAVE_REQUESTED));
    }
}
