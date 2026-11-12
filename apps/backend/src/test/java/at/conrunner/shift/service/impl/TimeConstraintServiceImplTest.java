package at.conrunner.shift.service.impl;

import at.conrunner.shift.audit.OperationsAudit;
import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.TimeConstraint;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.platform.exception.BadRequestException;
import at.conrunner.platform.exception.ConflictException;
import at.conrunner.platform.exception.NotFoundException;
import at.conrunner.shift.lib.type.TimeConstraintType;
import at.conrunner.shift.dao.AssignmentDao;
import at.conrunner.shift.dao.EventDao;
import at.conrunner.shift.dao.TimeConstraintDao;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.dto.TimeConstraintCreateDto;
import at.conrunner.shift.dto.TimeConstraintDto;
import at.conrunner.shift.util.SecurityHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeConstraintServiceImplTest {

    @Mock
    OperationsAudit operationsAudit;
    @Mock
    private TimeConstraintDao timeConstraintDao;
    @Mock
    private VolunteerDao volunteerDao;
    @Mock
    private EventDao eventDao;
    @Mock
    private AssignmentDao assignmentDao;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private TimeConstraintServiceImpl service;

    private static final java.util.UUID USER_ID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final long EVENT_ID = 42L;

    // Helper
    private Volunteer mockVolunteerWithEvent() {
        Event event = new Event();
        event.setId(EVENT_ID);
        ShiftPlan plan = new ShiftPlan();
        plan.setEvent(event);
        Volunteer v = new Volunteer();
        v.setId(USER_ID);
        v.setVolunteeringPlans(Set.of(plan));
        return v;
    }

    @Test
    void createTimeConstraint_volunteerMissing_throwsConflictException() {
        var dto = TimeConstraintCreateDto.builder()
            .type(TimeConstraintType.UNAVAILABLE)
            .from(Instant.parse("2030-01-01T09:00:00Z"))
            .to(Instant.parse("2030-01-01T11:00:00Z"))
            .build();

        when(eventDao.getById(EVENT_ID)).thenReturn(new Event());
        when(volunteerDao.getById(USER_ID)).thenThrow(NotFoundException.class);

        assertThatThrownBy(() -> service.createTimeConstraint(dto, USER_ID, EVENT_ID))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createTimeConstraint_volunteerNotPartOfEvent_throwsConflict() {
        var dto = TimeConstraintCreateDto.builder()
            .type(TimeConstraintType.UNAVAILABLE)
            .from(Instant.parse("2030-01-01T09:00:00Z"))
            .to(Instant.parse("2030-01-01T11:00:00Z"))
            .build();

        when(eventDao.getById(EVENT_ID)).thenReturn(new Event());
        Volunteer v = new Volunteer(); // no volunteering plans
        when(volunteerDao.getById(USER_ID)).thenReturn(v);

        assertThatThrownBy(() -> service.createTimeConstraint(dto, USER_ID, EVENT_ID))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("not part of event");
    }

    @Test
    void createTimeConstraint_overlapExisting_unavailable_throwsConflict() {
        var dto = TimeConstraintCreateDto.builder()
            .type(TimeConstraintType.UNAVAILABLE)
            .from(Instant.parse("2030-01-01T09:00:00Z"))
            .to(Instant.parse("2030-01-01T12:00:00Z"))
            .build();

        when(eventDao.getById(EVENT_ID)).thenReturn(new Event());
        when(volunteerDao.getById(USER_ID)).thenReturn(mockVolunteerWithEvent());

        TimeConstraint existing = new TimeConstraint();
        existing.setStartTime(Instant.parse("2030-01-01T10:00:00Z"));
        existing.setEndTime(Instant.parse("2030-01-01T13:00:00Z"));
        existing.setId(99L);

        when(timeConstraintDao.searchByVolunteerAndEvent(USER_ID, EVENT_ID)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createTimeConstraint(dto, USER_ID, EVENT_ID))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("overlaps with existing");
    }

    @Test
    void createTimeConstraint_assignmentOverlap_throwsConflict() {
        var dto = TimeConstraintCreateDto.builder()
            .type(TimeConstraintType.UNAVAILABLE)
            .from(Instant.parse("2030-01-01T09:00:00Z"))
            .to(Instant.parse("2030-01-01T11:00:00Z"))
            .build();

        when(eventDao.getById(EVENT_ID)).thenReturn(new Event());
        when(volunteerDao.getById(USER_ID)).thenReturn(mockVolunteerWithEvent());
        when(timeConstraintDao.searchByVolunteerAndEvent(USER_ID, EVENT_ID)).thenReturn(List.of());

        Assignment a = new Assignment();
        when(assignmentDao.getConflictingAssignments(eq(USER_ID), any(), any()))
            .thenReturn(List.of(a));

        assertThatThrownBy(() -> service.createTimeConstraint(dto, USER_ID, EVENT_ID))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("overlaps with existing assignments");
    }

    @Test
    void createTimeConstraint_emergency_invalidWholeDays_throwsBadRequest() {
        var dto = TimeConstraintCreateDto.builder()
            .type(TimeConstraintType.EMERGENCY)
            .from(Instant.parse("2030-01-01T10:00:00Z"))
            .to(Instant.parse("2030-01-02T00:00:00Z"))
            .build();

        assertThatThrownBy(() -> service.createTimeConstraint(dto, USER_ID, EVENT_ID))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("must span a positive multiple of 24 hours");
    }

    @Test
    void delete_missingConstraint_throwsNotFound() {
        when(timeConstraintDao.getById(anyLong())).thenThrow(NotFoundException.class);

        assertThatThrownBy(() -> service.delete(123L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_existing_deletesSuccessfully() {
        Volunteer volunteer = new Volunteer();
        volunteer.setId(USER_ID);

        TimeConstraint tc = new TimeConstraint();
        tc.setId(321L);
        tc.setVolunteer(volunteer);

        when(timeConstraintDao.getById(321L)).thenReturn(tc);

        service.delete(321L);
        verify(timeConstraintDao).delete(tc);
    }

    @Test
    void createTimeConstraint_success_unavailable_returnsDto() {
        var dto = TimeConstraintCreateDto.builder()
            .type(TimeConstraintType.UNAVAILABLE)
            .from(Instant.parse("2030-01-01T09:00:00Z"))
            .to(Instant.parse("2030-01-01T11:00:00Z"))
            .build();

        when(eventDao.getById(EVENT_ID)).thenReturn(new Event());
        when(volunteerDao.getById(USER_ID)).thenReturn(mockVolunteerWithEvent());
        when(timeConstraintDao.searchByVolunteerAndEvent(USER_ID, EVENT_ID)).thenReturn(List.of());
        when(assignmentDao.getConflictingAssignments(eq(USER_ID), any(), any())).thenReturn(List.of());


        Volunteer volunteer = new Volunteer();
        volunteer.setId(USER_ID);
        TimeConstraint saved = new TimeConstraint();
        saved.setId(10L);
        saved.setStartTime(dto.getFrom());
        saved.setEndTime(dto.getTo());
        saved.setVolunteer(volunteer);

        when(timeConstraintDao.save(any(TimeConstraint.class))).thenReturn(saved);

        TimeConstraintDto result = service.createTimeConstraint(dto, USER_ID, EVENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("10");
        assertThat(result.getFrom()).isEqualTo(dto.getFrom());
        assertThat(result.getTo()).isEqualTo(dto.getTo());
    }

}
