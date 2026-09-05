package at.control.shiftcontrol.shift;

import java.time.Clock;
import java.util.UUID;

import at.control.shiftcontrol.audit.AuditLog;
import at.control.shiftcontrol.eventcatalog.api.EventCatalogQuery;
import at.control.shiftcontrol.shift.assignment.AssignmentEligibilityService;
import at.control.shiftcontrol.shift.assignment.AssignmentRepository;
import at.control.shiftcontrol.shift.assignment.AssignmentService;
import at.control.shiftcontrol.shift.error.ShiftAccessDeniedException;
import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.error.ShiftNotFoundException;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileRepository;
import at.control.shiftcontrol.shift.volunteer.VolunteerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Event-scoped availability and in-event emergency absence. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TimeConstraintService {
    private final TimeConstraintRepository timeConstraintRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentService assignmentService;
    private final VolunteerProfileService volunteerProfileService;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final PlanMembershipRepository membershipRepository;
    private final ShiftPlanRepository shiftPlanRepository;
    private final EventCatalogQuery eventCatalogQuery;
    private final PlanAccess planAccess;
    private final AuditLog auditLog;
    private final Clock clock;

    @Transactional
    public TimeConstraintDetails declare(UUID eventId, TimeConstraintData data) {
        UUID volunteerId = planAccess.currentActor().userId();
        return declareFor(eventId, volunteerId, data, volunteerId);
    }

    /** Platform administrators may record an absence reported by another volunteer. */
    @Transactional
    public TimeConstraintDetails declareForVolunteer(UUID eventId, UUID volunteerId, TimeConstraintData data) {
        planAccess.requirePlatformAdmin();
        return declareFor(eventId, volunteerId, data, planAccess.currentActor().userId());
    }

    public TimeConstraintDetails get(UUID timeConstraintId) {
        TimeConstraintEntity constraint = requireConstraint(timeConstraintId);
        var actor = planAccess.currentActor();
        if (!actor.platformAdmin() && !actor.userId().equals(constraint.getUserId())) {
            throw new ShiftAccessDeniedException("Time constraint belongs to another volunteer");
        }
        return toDetails(constraint);
    }

    @Transactional
    public void remove(UUID timeConstraintId) {
        TimeConstraintEntity constraint = requireConstraint(timeConstraintId);
        var actor = planAccess.currentActor();
        if (!actor.platformAdmin() && !actor.userId().equals(constraint.getUserId())) {
            throw new ShiftAccessDeniedException("Time constraint belongs to another volunteer");
        }
        lockVolunteer(constraint.getUserId());
        timeConstraintRepository.delete(constraint);
        auditLog.record(actor.userId(), "TIME_CONSTRAINT_REMOVED", "TIME_CONSTRAINT", constraint.getId(),
            constraint.getType().name(), null, null);
    }

    private TimeConstraintDetails declareFor(UUID eventId, UUID volunteerId, TimeConstraintData data, UUID actorUserId) {
        var event = eventCatalogQuery.requireEvent(eventId);
        if (data.startTime().isBefore(event.startTime()) || data.endTime().isAfter(event.endTime())) {
            throw new ShiftValidationException("Time constraint range must be within its event");
        }
        if (!isVolunteerInEvent(volunteerId, eventId)) {
            throw new ShiftConflictException("Volunteer is not part of this event");
        }
        lockVolunteer(volunteerId);
        if (data.type() == TimeConstraintType.UNAVAILABLE) {
            if (timeConstraintRepository.existsOverlapping(eventId, volunteerId, data.startTime(), data.endTime())) {
                throw new ShiftConflictException("Time constraint overlaps an existing time constraint");
            }
            if (assignmentRepository.existsOccupiedAssignmentOverlappingShift(volunteerId, data.startTime(), data.endTime(),
                AssignmentEligibilityService.OCCUPYING_STATUSES)) {
                throw new ShiftConflictException("Time constraint overlaps an occupied assignment");
            }
        } else if (timeConstraintRepository.existsOverlappingByType(eventId, volunteerId, TimeConstraintType.EMERGENCY,
            data.startTime(), data.endTime())) {
            throw new ShiftConflictException("Emergency time constraint overlaps an existing emergency time constraint");
        }

        TimeConstraintEntity constraint = new TimeConstraintEntity();
        constraint.setEventId(eventId);
        constraint.setUserId(volunteerId);
        constraint.setType(data.type());
        constraint.setStartTime(data.startTime());
        constraint.setEndTime(data.endTime());
        constraint.setNote(data.note());
        constraint.setCreatedAt(clock.instant());
        constraint = timeConstraintRepository.save(constraint);
        auditLog.record(actorUserId, "TIME_CONSTRAINT_CREATED", "TIME_CONSTRAINT", constraint.getId(), null,
            data.type().name(), data.note());
        if (data.type() == TimeConstraintType.EMERGENCY) {
            assignmentService.resolveEmergency(eventId, volunteerId, data.startTime(), data.endTime(), actorUserId);
        }
        return toDetails(constraint);
    }

    private boolean isVolunteerInEvent(UUID volunteerId, UUID eventId) {
        return membershipRepository.findAllByUserId(volunteerId).stream()
            .map(membership -> shiftPlanRepository.findById(membership.getPlanId()).orElse(null))
            .anyMatch(plan -> plan != null && eventId.equals(plan.getEventId()));
    }

    private TimeConstraintEntity requireConstraint(UUID timeConstraintId) {
        return timeConstraintRepository.findById(timeConstraintId)
            .orElseThrow(() -> new ShiftNotFoundException("Time constraint not found: " + timeConstraintId));
    }

    private void lockVolunteer(UUID userId) {
        volunteerProfileService.ensureProfile(userId);
        volunteerProfileRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new ShiftNotFoundException("Volunteer profile not found: " + userId));
    }

    private static TimeConstraintDetails toDetails(TimeConstraintEntity constraint) {
        return new TimeConstraintDetails(constraint.getId(), constraint.getEventId(), constraint.getUserId(), constraint.getType(),
            constraint.getStartTime(), constraint.getEndTime(), constraint.getNote(), constraint.getCreatedAt());
    }
}
