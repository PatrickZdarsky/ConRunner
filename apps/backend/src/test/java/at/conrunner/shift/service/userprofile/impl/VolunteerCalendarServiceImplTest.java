package at.conrunner.shift.service.userprofile.impl;

import at.conrunner.shift.util.UniqueCodeGenerator;
import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.Location;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.platform.exception.NotFoundException;
import at.conrunner.shift.lib.type.AssignmentStatus;
import at.conrunner.shift.lib.type.LockStatus;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.repo.AssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerCalendarServiceImplTest {
    private static final java.util.UUID USER_ID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final java.util.UUID MISSING_ID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");
    @Mock
    private VolunteerDao volunteerDao;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private UniqueCodeGenerator uniqueCodeGenerator;

    @InjectMocks
    private VolunteerCalendarServiceImpl volunteerCalendarService;

    @Test
    void getOrCreateCalendarShareId_returnsExistingShareId() {
        Volunteer volunteer = Volunteer.builder()
            .id(USER_ID)
            .calendarShareId("EXISTING123")
            .build();
        when(volunteerDao.findById(USER_ID)).thenReturn(Optional.of(volunteer));

        var result = volunteerCalendarService.getOrCreateCalendarShareId(USER_ID);

        assertThat(result).contains("EXISTING123");
        verify(volunteerDao, never()).save(any());
    }

    @Test
    void regenerateCalendarShareId_rotatesAndPersistsShareId() {
        Volunteer volunteer = Volunteer.builder()
            .id(USER_ID)
            .calendarShareId("OLD123")
            .build();
        when(volunteerDao.findById(USER_ID)).thenReturn(Optional.of(volunteer));
        when(uniqueCodeGenerator.generateUnique(any(), eq(24), eq(10), any())).thenReturn("NEW123");

        var result = volunteerCalendarService.regenerateCalendarShareId(USER_ID);

        assertThat(result.getCalendarShareId()).isEqualTo("NEW123");
        assertThat(volunteer.getCalendarShareId()).isEqualTo("NEW123");
        verify(volunteerDao).save(volunteer);
    }

    @Test
    void regenerateCalendarShareId_rejectsMissingVolunteer() {
        when(volunteerDao.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> volunteerCalendarService.regenerateCalendarShareId(MISSING_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Volunteer not found");
    }

    @Test
    void getCalendar_returnsAggregatedIcsFeed() {
        Volunteer volunteer = Volunteer.builder()
            .id(USER_ID)
            .calendarShareId("CALENDAR123")
            .build();
        when(volunteerDao.findByCalendarShareId("CALENDAR123")).thenReturn(Optional.of(volunteer));

        Event event = Event.builder()
            .id(5L)
            .name("ShiftControl Summit")
            .build();
        ShiftPlan shiftPlan = ShiftPlan.builder()
            .id(7L)
            .name("Main Plan")
            .event(event)
            .lockStatus(LockStatus.SELF_SIGNUP)
            .build();
        Location location = Location.builder()
            .id(9L)
            .name("Hall A")
            .description("North entrance")
            .url("https://maps.example.test/hall-a")
            .build();
        Shift shift = Shift.builder()
            .id(11L)
            .name("Morning Shift")
            .shortDescription("Check-in desk")
            .startTime(Instant.parse("2026-07-04T08:00:00Z"))
            .endTime(Instant.parse("2026-07-04T12:00:00Z"))
            .shiftPlan(shiftPlan)
            .location(location)
            .build();

        Assignment assignmentOne = Assignment.builder()
            .id(1L)
            .status(AssignmentStatus.ACCEPTED)
            .assignedVolunteer(volunteer)
            .positionSlot(PositionSlot.builder()
                .id(100L)
                .name("Check-in")
                .shift(shift)
                .desiredVolunteerCount(1)
                .skipAutoAssignment(false)
                .build())
            .build();
        Assignment assignmentTwo = Assignment.builder()
            .id(2L)
            .status(AssignmentStatus.AUCTION)
            .assignedVolunteer(volunteer)
            .positionSlot(PositionSlot.builder()
                .id(101L)
                .name("Info Desk")
                .shift(shift)
                .desiredVolunteerCount(1)
                .skipAutoAssignment(false)
                .build())
            .build();

        when(assignmentRepository.findCalendarAssignmentsByVolunteerId(eq(USER_ID), anyCollection()))
            .thenReturn(List.of(assignmentOne, assignmentTwo));

        String calendar = volunteerCalendarService.getCalendar("CALENDAR123");

        assertThat(calendar).contains("BEGIN:VCALENDAR")
            .contains("SUMMARY:ShiftControl Summit - Morning Shift")
            .contains("Assigned slots: Check-in\\, Info Desk")
            .contains("LOCATION:Hall A")
            .contains("URL:https://maps.example.test/hall-a")
            .contains("UID:shift-11-volunteer-" + USER_ID + "@shiftcontrol");
    }

    @Test
    void getCalendar_rejectsUnknownShareId() {
        when(volunteerDao.findByCalendarShareId("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> volunteerCalendarService.getCalendar("UNKNOWN"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Volunteer calendar not found");
    }
}
