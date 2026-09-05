package at.control.shiftcontrol.shift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.audit.AuditLog;
import at.control.shiftcontrol.eventcatalog.api.EventCatalogQuery;
import at.control.shiftcontrol.shift.assignment.AssignmentRepository;
import at.control.shiftcontrol.shift.assignment.AssignmentEligibilityService;
import at.control.shiftcontrol.shift.assignment.AssignmentService;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.CurrentShiftActor;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.plan.PlanMembershipEntity;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanEntity;
import at.control.shiftcontrol.shift.plan.ShiftPlanRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileEntity;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimeConstraintServiceTest {
    private static final Instant START = Instant.parse("2026-05-01T00:00:00Z");
    @Mock private TimeConstraintRepository timeConstraintRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentService assignmentService;
    @Mock private VolunteerProfileService volunteerProfileService;
    @Mock private VolunteerProfileRepository volunteerProfileRepository;
    @Mock private PlanMembershipRepository membershipRepository;
    @Mock private ShiftPlanRepository shiftPlanRepository;
    @Mock private EventCatalogQuery catalog;
    @Mock private PlanAccess planAccess;
    @Mock private AuditLog auditLog;
    @Mock private Clock clock;
    @InjectMocks private TimeConstraintService constraints;

    @Test
    void emergencyCreatesConstraintAndResolvesConflictingAssignments() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(userId, false));
        when(catalog.requireEvent(eventId)).thenReturn(new EventCatalogQuery.CatalogEvent(eventId, START, START.plusSeconds(172800)));
        when(membershipRepository.findAllByUserId(userId)).thenReturn(List.of(new PlanMembershipEntity(planId, userId, false, START)));
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setId(planId);
        plan.setEventId(eventId);
        when(shiftPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(volunteerProfileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(new VolunteerProfileEntity(userId, START)));
        when(timeConstraintRepository.save(any(TimeConstraintEntity.class))).thenAnswer(invocation -> {
            TimeConstraintEntity constraint = invocation.getArgument(0);
            constraint.setId(UUID.randomUUID());
            return constraint;
        });
        when(clock.instant()).thenReturn(START);

        TimeConstraintDetails result = constraints.declare(eventId,
            new TimeConstraintData(TimeConstraintType.EMERGENCY, START, START.plusSeconds(86400), "sick"));

        assertEquals(TimeConstraintType.EMERGENCY, result.type());
        verify(assignmentService).resolveEmergency(eventId, userId, START, START.plusSeconds(86400), userId);
    }

    @Test
    void emergencyMustCoverWholeDays() {
        assertThrows(ShiftValidationException.class,
            () -> new TimeConstraintData(TimeConstraintType.EMERGENCY, START, START.plusSeconds(3600), null));
    }

    @Test
    void unavailableCannotOverlapAnOccupiedAssignment() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(userId, false));
        when(catalog.requireEvent(eventId)).thenReturn(new EventCatalogQuery.CatalogEvent(eventId, START, START.plusSeconds(172800)));
        when(membershipRepository.findAllByUserId(userId)).thenReturn(List.of(new PlanMembershipEntity(planId, userId, false, START)));
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setId(planId);
        plan.setEventId(eventId);
        when(shiftPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(volunteerProfileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(new VolunteerProfileEntity(userId, START)));
        when(assignmentRepository.existsOccupiedAssignmentOverlappingShift(userId, START, START.plusSeconds(3600),
            AssignmentEligibilityService.OCCUPYING_STATUSES)).thenReturn(true);

        assertThrows(at.control.shiftcontrol.shift.error.ShiftConflictException.class,
            () -> constraints.declare(eventId, new TimeConstraintData(TimeConstraintType.UNAVAILABLE, START, START.plusSeconds(3600), null)));
    }
}
