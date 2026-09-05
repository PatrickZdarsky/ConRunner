package at.control.shiftcontrol.shift.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class AssignmentStatusTest {
    @Test
    void retainsTheFourLegacyLiveAssignmentStates() {
        assertEquals(EnumSet.of(AssignmentStatus.ACCEPTED, AssignmentStatus.AUCTION,
            AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN, AssignmentStatus.REQUEST_FOR_ASSIGNMENT),
            EnumSet.allOf(AssignmentStatus.class));
    }
}
