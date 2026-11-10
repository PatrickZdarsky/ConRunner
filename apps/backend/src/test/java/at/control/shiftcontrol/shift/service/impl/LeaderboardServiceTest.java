package at.control.shiftcontrol.shift.service.impl;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.dao.EventDao;
import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserService;
import at.control.shiftcontrol.shift.util.SecurityHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {
    @Mock
    private EventDao eventDao;

    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private UserService userDirectoryService;

    @InjectMocks
    private LeaderboardServiceImpl leaderboardService;

    @Test
    void getLeaderBoard_aggregatesHoursAndUsesUserInfosForNames() {
        var user1 = volunteer(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var user2 = volunteer(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"));

        var shift1 = shift(Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T12:00:00Z"), assignment(user1));
        var shift2 = shift(Instant.parse("2026-01-01T13:00:00Z"), Instant.parse("2026-01-01T14:00:00Z"), assignment(user1));
        var shift3 = shift(Instant.parse("2026-01-01T15:00:00Z"), Instant.parse("2026-01-01T16:00:00Z"), assignment(user2));

        var plan = new ShiftPlan();
        plan.setShifts(List.of(shift1, shift2, shift3));

        var event = new Event();
        event.setId(9L);
        event.setShiftPlans(List.of(plan));

        when(eventDao.findById(9L)).thenReturn(Optional.of(event));
        when(userDirectoryService.getUsers(Set.of(user1.getId(), user2.getId()))).thenReturn(List.of(
            directoryUser(user1.getId(), "Alice"),
            directoryUser(user2.getId(), "Bob")
        ));

        var result = leaderboardService.getLeaderBoard(9L, user1.getId());

        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getOwnRank()).isNotNull();
        assertThat(result.getOwnRank().getRank()).isEqualTo(1);
        assertThat(result.getOwnRank().getHours()).isEqualTo(3);
        assertThat(result.getOwnRank().getName()).isEqualTo("Alice");
        assertThat(result.getRanks())
            .extracting("rank", "hours", "name")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1, 3L, "Alice"),
                org.assertj.core.groups.Tuple.tuple(2, 1L, "Bob")
            );

        verify(securityHelper).assertUserIsAllowedToAccessEvent(event);
    }

    private static Volunteer volunteer(java.util.UUID id) {
        var volunteer = new Volunteer();
        volunteer.setId(id);
        return volunteer;
    }

    private static Assignment assignment(Volunteer volunteer) {
        var assignment = new Assignment();
        assignment.setAssignedVolunteer(volunteer);
        return assignment;
    }

    private static Shift shift(Instant start, Instant end, Assignment assignment) {
        var slot = new PositionSlot();
        slot.setAssignments(List.of(assignment));

        var shift = new Shift();
        shift.setStartTime(start);
        shift.setEndTime(end);
        shift.setSlots(List.of(slot));

        slot.setShift(shift);
        var assignmentPositionSlot = new PositionSlot();
        assignmentPositionSlot.setShift(shift);
        assignment.setPositionSlot(assignmentPositionSlot);
        return shift;
    }

    private static User directoryUser(java.util.UUID id, String name) {
        return User.builder().id(id).username(name).build();
    }
}
