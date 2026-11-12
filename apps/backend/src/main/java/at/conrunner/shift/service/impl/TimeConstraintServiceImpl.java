package at.conrunner.shift.service.impl;

import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.audit.AssignmentAudit;
import at.conrunner.shift.audit.OperationsAudit;

import java.util.UUID;
import at.conrunner.communications.api.NotificationTypes;
import at.conrunner.communications.api.NotificationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.TimeConstraint;
import at.conrunner.shift.lib.event.events.PositionSlotVolunteerEvent;
import at.conrunner.shift.lib.event.events.TimeConstraintEvent;
import at.conrunner.platform.exception.BadRequestException;
import at.conrunner.platform.exception.ConflictException;
import at.conrunner.platform.exception.IllegalStateException;
import at.conrunner.shift.lib.type.AssignmentStatus;
import at.conrunner.shift.lib.type.LockStatus;
import at.conrunner.shift.lib.type.TimeConstraintType;
import at.conrunner.shift.dao.AssignmentDao;
import at.conrunner.shift.dao.EventDao;
import at.conrunner.shift.dao.TimeConstraintDao;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.dto.TimeConstraintCreateDto;
import at.conrunner.shift.dto.TimeConstraintDto;
import at.conrunner.shift.mapper.TimeConstraintMapper;
import at.conrunner.shift.service.AssignmentService;
import at.conrunner.shift.service.TimeConstraintService;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import at.conrunner.communications.api.NotificationRequest;
import at.conrunner.communications.api.NotificationRequests;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeConstraintServiceImpl implements TimeConstraintService {
    private final TimeConstraintDao timeConstraintDao;
    private final AssignmentAudit audit;
    private final OperationsAudit operationsAudit;
    private final AssignmentDao assignmentDao;
    private final EventDao eventDao;
    private final VolunteerDao volunteerDao;
    private final ApplicationEventPublisher publisher;
    private final NotificationRequests notificationRequests;
    private final AssignmentService assignmentService;

    private static final long SECONDS_PER_DAY = Duration.ofDays(1).getSeconds();

    @Override
    public @org.jspecify.annotations.NonNull Collection<TimeConstraintDto> getTimeConstraints(@NonNull UUID userId, long eventId) {
        return TimeConstraintMapper.toDto(timeConstraintDao.searchByVolunteerAndEvent(userId, eventId));
    }

    @Override
    @Transactional
    public @org.jspecify.annotations.NonNull TimeConstraintDto createTimeConstraint(@NonNull TimeConstraintCreateDto createDto, @NonNull UUID userId, long eventId) {
        //VALIDATION: Validate date range
        Instant from = createDto.getFrom();
        Instant to = createDto.getTo();
        validateTimespan(createDto.getType(), from, to);

        // get event and volunteer
        var event = eventDao.getById(eventId);
        var volunteer = volunteerDao.getById(userId);

        //VALIDATION: Check that volunteer is part of the event via any of their shift plans
        var userPlans = volunteer.getVolunteeringPlans();
        var volunteerIsInEvent = userPlans != null && userPlans.stream().anyMatch(plan -> plan.getEvent().getId() == eventId);
        if (!volunteerIsInEvent) {
            log.error("Volunteer with id: {} is not part of event with id: {}", userId, eventId);
            throw new ConflictException("Volunteer is not part of event.");
        }

        //VALIDATION: Check for overlapping constraints/assignments
        switch (createDto.getType()) {
            case UNAVAILABLE -> {
                // Check for overlapping time constraints for this volunteer+event
                var existingConstraints = timeConstraintDao.searchByVolunteerAndEvent(userId, eventId);
                checkForConstraintOverlaps(createDto, existingConstraints);
                checkForAssignmentOverlaps(userId, from, to);
            }
            case EMERGENCY -> {
                // Check for overlapping time constraints for this volunteer+event+type
                var existingConstraints = timeConstraintDao.searchByVolunteerAndEventAndType(userId, eventId, TimeConstraintType.EMERGENCY);
                checkForConstraintOverlaps(createDto, existingConstraints);
                // todo add trust alert
            }
            default -> throw new IllegalStateException("Unexpected value: " + createDto.getType());
        }
        //ACT: Create and save entity
        var entity = timeConstraintDao.save(TimeConstraintMapper.fromCreateDto(createDto, volunteer, event));

        //ACT: Do extra tasks for emergency constraints
        if (createDto.getType() == TimeConstraintType.EMERGENCY) {
            handleEmergencyConflictingAssignments(entity);
        }

        //NOTIFY: Publish event
        operationsAudit.constraint(entity, true);
        publisher.publishEvent(TimeConstraintEvent.timeConstraintCreated(entity));
        return TimeConstraintMapper.toDto(entity);
    }

    /**
     * Handles conflicting assignments for an EMERGENCY time constraint by unassigning or marking them for unassignment.
     *
     * @param emergencyConstraint The EMERGENCY time constraint that may conflict with existing assignments.
     */
    protected void handleEmergencyConflictingAssignments(@NonNull TimeConstraint emergencyConstraint) {
        var volunteerId = emergencyConstraint.getVolunteer().getId();
        var from = emergencyConstraint.getStartTime();
        var to = emergencyConstraint.getEndTime();

        var conflictingAssignments = assignmentDao.getConflictingAssignments(volunteerId, from, to);
        for (var assignment : conflictingAssignments) {
            if (assignment.getPositionSlot().getShift().getShiftPlan().getLockStatus() == LockStatus.SELF_SIGNUP) {
                log.info("Unassigning assignment id={} due to conflicting EMERGENCY time constraint id={}",
                    assignment.getId(), emergencyConstraint.getId());
                assignmentService.unassignInternal(assignment);
            } else {
                log.info("Setting assignment id={} to AUCTION_REQUEST_FOR_UNASSIGN due to conflicting EMERGENCY time constraint id={}",
                    assignment.getId(), emergencyConstraint.getId());

                var before = audit.snapshot(assignment);
                assignment.setStatus(AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN);
                assignment = assignmentDao.save(assignment);
                audit.emergencyLeaveRequested(assignment, before);

                var plan = assignment.getPositionSlot().getShift().getShiftPlan();
                var trigger = java.util.UUID.randomUUID();
                var slotName = assignment.getPositionSlot().getName();
                if (plan.getPlanPlanners() != null) plan.getPlanPlanners().stream()
                    .map(Volunteer::getId).distinct().forEach(id ->
                        notificationRequests.request(new NotificationRequest(trigger, id,
                            NotificationTypes.ASSIGNMENT_LEAVE_REQUESTED, NotificationContext.encode(plan.getEvent().getId() + ":" + plan.getId(), slotName, null))));

                // publish event
                publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotRequestLeave(
                    assignment.getPositionSlot(), assignment.getAssignedVolunteer().getId().toString()));
            }
        }
    }

    private static void validateTimespan(@NonNull TimeConstraintType type, Instant from, Instant to) {
        if (from == null || to == null) {
            throw new BadRequestException("'from' and 'to' timestamps must not be null");
        }

        if (!from.isBefore(to)) {
            throw new ConflictException("Invalid time range: 'from' must be before 'to'");
        }

        if (type == TimeConstraintType.EMERGENCY) {
            Duration duration = Duration.between(from, to);

            long seconds = duration.getSeconds();
            if (seconds <= 0 || seconds % SECONDS_PER_DAY != 0) {
                throw new BadRequestException("EMERGENCY time constraints must span a positive multiple of 24 hours");
            }
        }
    }

    @Override
    @Transactional
    public void delete(long timeConstraintId) {
        var timeConstraint = timeConstraintDao.getById(timeConstraintId);

        operationsAudit.constraint(timeConstraint, false);
        var timeConstraintEvent = TimeConstraintEvent.timeConstraintDeleted(timeConstraint);
        timeConstraintDao.delete(timeConstraint);
        publisher.publishEvent(timeConstraintEvent);
    }

    static void checkForConstraintOverlaps(@NonNull TimeConstraintCreateDto createDto,
                                           @NonNull Collection<TimeConstraint> existingConstraints) {
        for (var constraint : existingConstraints) {
            if (createDto.getFrom().isBefore(constraint.getEndTime())
                && createDto.getTo().isAfter(constraint.getStartTime())) {
                log.error("New time constraint from {} to {} overlaps with existing time constraint id={} from {} to {}",
                    createDto.getFrom(), createDto.getTo(),
                    constraint.getId(), constraint.getStartTime(), constraint.getEndTime());
                throw new ConflictException("New time constraint overlaps with existing time constraint.");
            }
        }
    }

    private void checkForAssignmentOverlaps(@NonNull UUID userId, Instant from, Instant to) {
        var existingAssignments = assignmentDao.getConflictingAssignments(userId, from, to);
        if (!existingAssignments.isEmpty()) {
            log.error("New time constraint from {} to {} overlaps with existing assignments ids={}",
                from, to,
                existingAssignments.stream().map(Assignment::getId).toList());
            throw new ConflictException("New time constraint overlaps with existing assignments.");
        }
    }
}
