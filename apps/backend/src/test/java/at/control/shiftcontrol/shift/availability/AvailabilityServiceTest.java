package at.control.shiftcontrol.shift.availability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.api.EventCatalogQuery;
import at.control.shiftcontrol.shift.assignment.StaffingAssignmentRepository;
import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.plan.CurrentShiftActor;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileEntity;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {
    private static final Instant START = Instant.parse("2026-05-01T12:00:00Z");

    @Mock private AvailabilityRepository availabilityRepository;
    @Mock private StaffingAssignmentRepository assignmentRepository;
    @Mock private VolunteerProfileService volunteerProfileService;
    @Mock private VolunteerProfileRepository volunteerProfileRepository;
    @Mock private EventCatalogQuery eventCatalogQuery;
    @Mock private PlanAccess planAccess;
    @Mock private Clock clock;

    @InjectMocks private AvailabilityService availability;

    @Test
    void volunteerCannotDeclareAvailabilityOverAnOccupiedAssignmentWithoutAnOverride() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        configure(eventId, userId);
        when(assignmentRepository.existsOccupiedAssignmentOverlapping(eventId, userId, START, START.plusSeconds(3600))).thenReturn(true);

        assertThrows(ShiftConflictException.class,
            () -> availability.declare(eventId, new AvailabilityData(START, START.plusSeconds(3600), null)));
    }

    @Test
    void platformOverrideStoresItsExplicitReason() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        configure(eventId, userId);
        when(availabilityRepository.save(any(AvailabilityEntity.class))).thenAnswer(invocation -> {
            AvailabilityEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        when(clock.instant()).thenReturn(START);

        AvailabilityDetails result = availability.declareOverride(eventId, userId,
            new AvailabilityData(START, START.plusSeconds(3600), "Medical appointment"), "Authorized by operations");

        assertEquals("Authorized by operations", result.authorizedOverrideReason());
    }

    private void configure(UUID eventId, UUID userId) {
        when(planAccess.currentActor()).thenReturn(new CurrentShiftActor.ShiftActor(userId, true));
        when(eventCatalogQuery.requireEvent(eventId)).thenReturn(new EventCatalogQuery.CatalogEvent(
            eventId, START.minusSeconds(3600), START.plusSeconds(7200)));
        when(volunteerProfileRepository.findByIdForUpdate(userId))
            .thenReturn(Optional.of(new VolunteerProfileEntity(userId, START)));
        when(availabilityRepository.existsOverlapping(eventId, userId, START, START.plusSeconds(3600))).thenReturn(false);
        when(assignmentRepository.existsOccupiedAssignmentOverlapping(eventId, userId, START, START.plusSeconds(3600))).thenReturn(false);
    }
}
