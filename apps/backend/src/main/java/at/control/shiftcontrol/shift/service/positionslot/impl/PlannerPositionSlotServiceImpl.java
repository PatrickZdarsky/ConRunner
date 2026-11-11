package at.control.shiftcontrol.shift.service.positionslot.impl;

import java.util.UUID;
import at.control.shiftcontrol.communications.api.NotificationTypes;
import at.control.shiftcontrol.communications.api.NotificationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.context.ApplicationEventPublisher;
import at.control.shiftcontrol.communications.api.NotificationRequest;
import at.control.shiftcontrol.communications.api.NotificationRequests;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.event.events.PositionSlotVolunteerEvent;
import at.control.shiftcontrol.platform.exception.IllegalArgumentException;
import at.control.shiftcontrol.platform.exception.IllegalStateException;
import at.control.shiftcontrol.platform.exception.ValidationException;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dao.AssignmentDao;
import at.control.shiftcontrol.shift.dao.PositionSlotDao;
import at.control.shiftcontrol.shift.dao.ShiftPlanDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentAssignDto;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentDto;
import at.control.shiftcontrol.shift.dto.plannerdashboard.AssignmentFilterDto;
import at.control.shiftcontrol.shift.dto.plannerdashboard.AssignmentPlannerInfoDto;
import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;
import at.control.shiftcontrol.shift.mapper.AssignmentAssemblingMapper;
import at.control.shiftcontrol.shift.mapper.AssignmentPlannerInfoAssemblingMapper;
import at.control.shiftcontrol.shift.mapper.VolunteerAssemblingMapper;
import at.control.shiftcontrol.shift.service.AssignmentService;
import at.control.shiftcontrol.shift.service.EligibilityService;
import at.control.shiftcontrol.shift.service.positionslot.PlannerPositionSlotService;
import at.control.shiftcontrol.identity.UserService;
import at.control.shiftcontrol.shift.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class PlannerPositionSlotServiceImpl implements PlannerPositionSlotService {
    private final SecurityHelper securityHelper;
    private final AssignmentService assignmentService;
    private final ShiftPlanDao shiftPlanDao;
    private final AssignmentDao assignmentDao;
    private final PositionSlotDao positionSlotDao;
    private final VolunteerDao volunteerDao;
    private final ApplicationEventPublisher publisher;
    private final NotificationRequests notificationRequests;
    private final EligibilityService eligibilityService;
    private final UserService userService;
    private final AssignmentAssemblingMapper assignmentAssemblingMapper;
    private final AssignmentPlannerInfoAssemblingMapper assignmentRequestAssemblingMapper;

    @Override
    public Collection<AssignmentPlannerInfoDto> getSlots(long shiftPlanId, AssignmentFilterDto filterDto) {
        var plan = shiftPlanDao.getById(shiftPlanId);
        securityHelper.assertUserIsPlanner(plan);
        return assignmentRequestAssemblingMapper.toAssignmentPlannerInfoDto(plan.getShifts(), filterDto)
            .stream()
            .filter(assignmentPlannerInfoDto -> !assignmentPlannerInfoDto.getSlots().isEmpty())
            .toList();
    }

    @Override
    @Transactional
    public void acceptRequest(long shiftPlanId, long positionSlotId, UUID userId) {
        Assignment assignment = assignmentDao.getAssignmentForPositionSlotAndUser(positionSlotId, userId);
        securityHelper.assertUserIsPlanner(assignment.getPositionSlot());

        switch (assignment.getStatus()) {
            case ACCEPTED, AUCTION -> throw new IllegalArgumentException("Assignment is not acceptable");
            case AUCTION_REQUEST_FOR_UNASSIGN -> {
                assignmentService.unassignInternal(assignment);
                notifyVolunteer(assignment.getPositionSlot(), userId, NotificationTypes.ASSIGNMENT_LEAVE_ACCEPTED);
                publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotLeaveRequestAccepted(assignment.getPositionSlot(), userId.toString()));
            }
            case REQUEST_FOR_ASSIGNMENT -> {
                if (!eligibilityService.hasCapacity(assignment.getPositionSlot())) {
                    throw new IllegalArgumentException("Slot is already full");
                }
                assignmentService.accept(assignment);
                notifyVolunteer(assignment.getPositionSlot(), userId, NotificationTypes.ASSIGNMENT_JOIN_ACCEPTED);
                publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotJoinRequestAccepted(assignment.getPositionSlot(), userId.toString()));
            }
            default -> throw new IllegalStateException("Unexpected value: " + assignment.getStatus());
        }
    }

    @Override
    @Transactional
    public void declineRequest(long shiftPlanId, long positionSlotId, UUID userId) {
        Assignment assignment = assignmentDao.getAssignmentForPositionSlotAndUser(positionSlotId, userId);
        securityHelper.assertUserIsPlanner(assignment.getPositionSlot());

        switch (assignment.getStatus()) {
            case ACCEPTED, AUCTION -> throw new IllegalArgumentException("Assignment is not declineable");
            case AUCTION_REQUEST_FOR_UNASSIGN -> {
                acceptAssignment(assignment);
                notifyVolunteer(assignment.getPositionSlot(), userId, NotificationTypes.ASSIGNMENT_LEAVE_DECLINED);
                publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotLeaveRequestDeclined(assignment.getPositionSlot(), userId.toString()));
            }
            case REQUEST_FOR_ASSIGNMENT -> {
                assignmentDao.delete(assignment);
                notifyVolunteer(assignment.getPositionSlot(), userId, NotificationTypes.ASSIGNMENT_JOIN_DECLINED);
                publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotJoinRequestDeclined(assignment.getPositionSlot(), userId.toString()));
            }
            default -> throw new IllegalStateException("Unexpected value: " + assignment.getStatus());
        }
    }

    private void notifyVolunteer(PositionSlot slot, UUID userId, String type) {
        notificationRequests.request(new NotificationRequest(java.util.UUID.randomUUID(), userId,
            type, NotificationContext.encode(String.valueOf(slot.getShift().getShiftPlan().getEvent().getId()), slot.getName(), null)));
    }

    @Override
    @Transactional
    public Collection<VolunteerDto> getAssignableUsers(String positionSlotId) {
        PositionSlot positionSlot = positionSlotDao.getById(
            ConvertUtil.idToLong(positionSlotId));

        // check access to plan
        securityHelper.assertUserIsPlanner(positionSlot);

        // get all volunteers with access to slot
        Collection<Volunteer> volunteers =
            volunteerDao.findAllByShiftPlan(positionSlot.getShift().getShiftPlan().getId());

        // check if not already signed up, eligible and no conflicts
        volunteers = volunteers.stream().filter(v -> isAssignable(positionSlot, v)).toList();

        var users = userService.getUsers(volunteers.stream().map(Volunteer::getId).toList());
        return VolunteerAssemblingMapper.toDtoFromUser(users);
    }

    private boolean isAssignable(PositionSlot positionSlot, Volunteer volunteer) {
        // check if assignable
        boolean eligible = eligibilityService.isEligibleAndNotSignedUp(positionSlot, volunteer);
        // check for conflicts
        Collection<Assignment> conflicts = eligibilityService.getConflictingAssignmentsExcludingShift(
            volunteer.getId(), positionSlot, positionSlot.getShift().getId());

        return eligible && conflicts.isEmpty();
    }

    @Override
    @Transactional
    public Collection<AssignmentDto> assignUsersToSlot(AssignmentAssignDto assignmentAssignDto) {
        PositionSlot positionSlot = positionSlotDao.getById(
            ConvertUtil.idToLong(assignmentAssignDto.getPositionSlotId()));

        // check access to plan
        securityHelper.assertUserIsPlanner(positionSlot);

        // ignore lock status

        // get all requested volunteers with access to slot
        Collection<Volunteer> volunteers = volunteerDao.findAllByShiftPlanAndVolunteerIds(
            positionSlot.getShift().getShiftPlan().getId(),
            assignmentAssignDto.getVolunteerIds());

        // check if not already signed up, eligible and no conflicts
        volunteers = volunteers.stream().filter(v -> isAssignable(positionSlot, v)).toList();

        // assign volunteers to slot
        Collection<Assignment> assignments = new ArrayList<>(volunteers.size());
        Iterator<Volunteer> iterator = volunteers.stream().iterator();
        while (iterator.hasNext()) {
            if (!eligibilityService.hasCapacity(positionSlot)) {
                throw new ValidationException("Slot is already full");
            }
            Assignment assignment = null;
            var nextVolunteer = iterator.next();
            try {
                assignment = assignmentDao.getAssignmentForPositionSlotAndUser(
                    positionSlot.getId(), nextVolunteer.getId());
            } catch (Exception _) {
                // ignore
            }
            if (assignment == null) {
                assignment = Assignment.of(positionSlot, nextVolunteer, AssignmentStatus.REQUEST_FOR_ASSIGNMENT);
            }
            assignments.add(
                assignmentService.accept(assignment)
            );
        }

        return assignmentAssemblingMapper.assemble(assignments);
    }

    @Override
    @Transactional
    public void unAssignUsersFromSlot(AssignmentAssignDto assignmentAssignDto) {
        PositionSlot positionSlot = positionSlotDao.getById(
            ConvertUtil.idToLong(assignmentAssignDto.getPositionSlotId()));

        // check access to plan
        securityHelper.assertUserIsPlanner(positionSlot);

        // ignore lock status

        Collection<Assignment> assignments =
            assignmentDao.getAssignmentForPositionSlotAndUsers(positionSlot.getId(), assignmentAssignDto.getVolunteerIds());
        assignments.forEach(assignmentService::unassignInternal);
    }

    private void acceptAssignment(Assignment assignment) {
        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignmentDao.save(assignment);
    }
}
