package at.control.shiftcontrol.shift.availability;

import java.time.Clock;
import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.api.EventCatalogQuery;
import at.control.shiftcontrol.shift.assignment.StaffingAssignmentRepository;
import at.control.shiftcontrol.shift.error.ShiftAccessDeniedException;
import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.error.ShiftNotFoundException;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.CurrentShiftActor;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Normal availability is event-scoped. The volunteer-profile lock serializes it with assignment
 * commands for that volunteer, so a conflicting availability and assignment cannot both succeed.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AvailabilityService {
    private final AvailabilityRepository availabilityRepository;
    private final StaffingAssignmentRepository assignmentRepository;
    private final VolunteerProfileService volunteerProfileService;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final EventCatalogQuery eventCatalogQuery;
    private final PlanAccess planAccess;
    private final Clock clock;

    @Transactional
    public AvailabilityDetails declare(UUID eventId, AvailabilityData data) {
        CurrentShiftActor.ShiftActor actor = planAccess.currentActor();
        return declare(eventId, actor.userId(), data, null, actor.userId());
    }

    /**
     * An event-level constraint can affect several plans, so only the platform capability may make
     * a conflicting declaration for another volunteer. The reason is persisted with the exception.
     */
    @Transactional
    public AvailabilityDetails declareOverride(UUID eventId, UUID volunteerId, AvailabilityData data, String overrideReason) {
        planAccess.requirePlatformAdmin();
        return declare(eventId, volunteerId, data, requiredReason(overrideReason), planAccess.currentActor().userId());
    }

    public AvailabilityDetails get(UUID availabilityId) {
        AvailabilityEntity availability = availabilityRepository.findById(availabilityId)
            .orElseThrow(() -> new ShiftNotFoundException("Availability not found: " + availabilityId));
        CurrentShiftActor.ShiftActor actor = planAccess.currentActor();
        if (!actor.platformAdmin() && !actor.userId().equals(availability.getUserId())) {
            throw new ShiftAccessDeniedException("Availability belongs to another volunteer");
        }
        return toDetails(availability);
    }

    @Transactional
    public void remove(UUID availabilityId) {
        AvailabilityEntity availability = availabilityRepository.findById(availabilityId)
            .orElseThrow(() -> new ShiftNotFoundException("Availability not found: " + availabilityId));
        CurrentShiftActor.ShiftActor actor = planAccess.currentActor();
        if (!actor.platformAdmin() && !actor.userId().equals(availability.getUserId())) {
            throw new ShiftAccessDeniedException("Availability belongs to another volunteer");
        }
        lockVolunteer(availability.getUserId());
        availabilityRepository.delete(availability);
    }

    private AvailabilityDetails declare(UUID eventId, UUID volunteerId, AvailabilityData data, String overrideReason,
                                        UUID createdByUserId) {
        var event = eventCatalogQuery.requireEvent(eventId);
        if (data.startTime().isBefore(event.startTime()) || data.endTime().isAfter(event.endTime())) {
            throw new ShiftValidationException("Availability time range must be within its event");
        }
        lockVolunteer(volunteerId);
        boolean overlapsAvailability = availabilityRepository.existsOverlapping(eventId, volunteerId, data.startTime(), data.endTime());
        boolean overlapsAssignment = assignmentRepository.existsOccupiedAssignmentOverlapping(
            eventId, volunteerId, data.startTime(), data.endTime());
        if ((overlapsAvailability || overlapsAssignment) && overrideReason == null) {
            throw new ShiftConflictException(overlapsAvailability
                ? "Availability overlaps an existing availability declaration"
                : "Availability overlaps an occupied assignment");
        }

        AvailabilityEntity availability = new AvailabilityEntity();
        availability.setEventId(eventId);
        availability.setUserId(volunteerId);
        availability.setStartTime(data.startTime());
        availability.setEndTime(data.endTime());
        availability.setNote(data.note());
        availability.setCreatedByUserId(createdByUserId);
        availability.setAuthorizedOverrideReason(overrideReason);
        availability.setCreatedAt(clock.instant());
        return toDetails(availabilityRepository.save(availability));
    }

    private void lockVolunteer(UUID userId) {
        volunteerProfileService.ensureProfile(userId);
        volunteerProfileRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new ShiftNotFoundException("Volunteer profile not found: " + userId));
    }

    private static String requiredReason(String value) {
        if (value == null || value.isBlank()) {
            throw new ShiftValidationException("An authorized override reason is required");
        }
        String reason = value.trim();
        if (reason.length() > 1024) {
            throw new ShiftValidationException("An authorized override reason must be at most 1024 characters");
        }
        return reason;
    }

    private static AvailabilityDetails toDetails(AvailabilityEntity availability) {
        return new AvailabilityDetails(availability.getId(), availability.getEventId(), availability.getUserId(),
            availability.getStartTime(), availability.getEndTime(), availability.getNote(), availability.getCreatedByUserId(),
            availability.getAuthorizedOverrideReason(), availability.getCreatedAt());
    }
}
