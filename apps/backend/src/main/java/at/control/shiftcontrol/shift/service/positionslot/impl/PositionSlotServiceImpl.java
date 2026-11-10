package at.control.shiftcontrol.shift.service.positionslot.impl;

import java.util.UUID;
import at.control.shiftcontrol.communications.api.NotificationTypes;

import java.util.Collection;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import at.control.shiftcontrol.communications.api.NotificationRequest;
import at.control.shiftcontrol.communications.api.NotificationRequests;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.event.events.AssignmentEvent;
import at.control.shiftcontrol.shift.lib.event.events.PositionSlotEvent;
import at.control.shiftcontrol.shift.lib.event.events.PositionSlotVolunteerEvent;
import at.control.shiftcontrol.shift.lib.event.events.PreferenceEvent;
import at.control.shiftcontrol.platform.exception.BadRequestException;
import at.control.shiftcontrol.platform.exception.IllegalArgumentException;
import at.control.shiftcontrol.platform.exception.IllegalStateException;
import at.control.shiftcontrol.platform.exception.ValidationException;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.dao.AssignmentDao;
import at.control.shiftcontrol.shift.dao.AssignmentSwitchRequestDao;
import at.control.shiftcontrol.shift.dao.PositionSlotDao;
import at.control.shiftcontrol.shift.dao.ShiftDao;
import at.control.shiftcontrol.shift.dao.role.RoleDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotModificationDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotRequestDto;
import at.control.shiftcontrol.shift.mapper.AssignmentAssemblingMapper;
import at.control.shiftcontrol.shift.mapper.PositionSlotAssemblingMapper;
import at.control.shiftcontrol.shift.service.AssignmentService;
import at.control.shiftcontrol.shift.service.EligibilityService;
import at.control.shiftcontrol.shift.service.positionslot.PositionSlotService;
import at.control.shiftcontrol.shift.util.LockStatusHelper;
import at.control.shiftcontrol.shift.util.SecurityHelper;

@RequiredArgsConstructor
@Service
public class PositionSlotServiceImpl implements PositionSlotService {
    private final PositionSlotDao positionSlotDao;
    private final ShiftDao shiftDao;
    private final RoleDao roleDao;
    private final VolunteerDao volunteerDao;
    private final AssignmentDao assignmentDao;
    private final AssignmentSwitchRequestDao assignmentSwitchRequestDao;

    private final EligibilityService eligibilityService;
    private final AssignmentService assignmentService;
    private final PositionSlotAssemblingMapper positionSlotAssemblingMapper;
    private final SecurityHelper securityHelper;
    private final ApplicationEventPublisher publisher;
    private final NotificationRequests notificationRequests;
    private final AssignmentAssemblingMapper assignmentAssemblingMapper;

    @Override
    public PositionSlotDto findById(@NonNull Long positionSlotId) {
        PositionSlot positionSlot = positionSlotDao.getById(positionSlotId);
        securityHelper.assertUserIsInPlan(positionSlot);
        return positionSlotAssemblingMapper.assemble(positionSlot);
    }

    @Override
    @Transactional
    public AssignmentDto join(@NonNull Long positionSlotId, @NonNull UUID currentUserId, @NonNull PositionSlotRequestDto requestDto) {
        // get position slot and volunteer
        PositionSlot positionSlot = positionSlotDao.getById(positionSlotId);
        Volunteer volunteer = volunteerDao.getById(currentUserId);

        // check if plan is locked or supervised
        LockStatusHelper.assertIsSelfSignUpWithMessage(positionSlot, "join");

        // check access, already assigned, eligible and conflicts
        assertJoinPossible(positionSlot, volunteer);

        // assign volunteer to slot
        Assignment assignment = assignmentService.assign(positionSlot, volunteer, requestDto);

        // save and return
        return assignmentAssemblingMapper.assemble(assignment);
    }

    @Override
    @Transactional
    public void leave(@NonNull Long positionSlotId, @NonNull UUID volunteerId) {
        // get assignment
        Assignment assignment = assignmentDao.getAssignmentForPositionSlotAndUser(positionSlotId, volunteerId);

        // check if plan is locked or supervised
        LockStatusHelper.assertIsSelfSignUpWithMessage(assignment, "leave");

        // leave
        assignmentService.unassignInternal(assignment);
    }

    @Override
    @Transactional
    public AssignmentDto joinRequest(@NonNull Long positionSlotId, @NonNull UUID currentUserId) {
        // get position slot and volunteer
        PositionSlot positionSlot = positionSlotDao.getById(positionSlotId);
        Volunteer volunteer = volunteerDao.getById(currentUserId);

        // check if plan is supervised
        LockStatusHelper.assertIsSupervisedWithMessage(positionSlot, "join request");

        // check access, already assigned, eligible and conflicts
        assertJoinPossible(positionSlot, volunteer);

        // create assignment
        Optional<Assignment> existingRequest = assignmentDao.findAssignmentForPositionSlotAndUser(
            positionSlot.getId(), volunteer.getId());
        if (existingRequest.isPresent()) {
            throw new IllegalStateException("You already joined or requested join for this position!");
        }
        Assignment joinRequest = Assignment.of(positionSlot, volunteer, AssignmentStatus.REQUEST_FOR_ASSIGNMENT);
        joinRequest = assignmentDao.save(joinRequest);

        notifyPlanners(positionSlot, NotificationTypes.ASSIGNMENT_JOIN_REQUESTED);

        // publish event
        publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotJoinRequestCreated(positionSlot, currentUserId.toString()));
        return assignmentAssemblingMapper.assemble(joinRequest);
    }

    @Override
    @Transactional
    public void leaveRequest(@NonNull Long positionSlotId, @NonNull UUID currentUserId) {
        // get assignment
        Optional<Assignment> existingAssignment = assignmentDao.findAssignmentForPositionSlotAndUser(positionSlotId, currentUserId);
        if (existingAssignment.isEmpty() || AssignmentStatus.REQUEST_STATES.contains(existingAssignment.get().getStatus())) {
            throw new IllegalStateException("You already left or requested leaving this position!");
        }
        Assignment assignment = existingAssignment.get();

        // check if plan is locked or supervised
        LockStatusHelper.assertIsSupervisedWithMessage(assignment, "leave request");

        // update assignment
        assignment.setStatus(AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN);
        assignmentDao.save(assignment);

        notifyPlanners(assignment.getPositionSlot(), NotificationTypes.ASSIGNMENT_LEAVE_REQUESTED);

        // publish event
        publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotRequestLeave(assignment.getPositionSlot(), currentUserId.toString()));
    }

    @Override
    public void joinRequestWithdraw(@NonNull Long positionSlotId, @NonNull UUID currentUserId) {
        // get assignment
        Optional<Assignment> existingAssignment = assignmentDao.findAssignmentForPositionSlotAndUser(positionSlotId, currentUserId);
        // check status
        if (existingAssignment.isEmpty() || existingAssignment.get().getStatus() != AssignmentStatus.REQUEST_FOR_ASSIGNMENT) {
            throw new IllegalArgumentException("Assignment not in request status");
        }
        Assignment assignment = existingAssignment.get();
        LockStatusHelper.assertIsSupervisedWithMessage(assignment, "withdraw join request");

        // publish event
        publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotJoinRequestWithdrawn(assignment.getPositionSlot(), currentUserId.toString()));
        // delete assignment
        assignmentDao.delete(assignment);
    }

    @Override
    public void leaveRequestWithdraw(@NonNull Long positionSlotId, @NonNull UUID currentUserId) {
        // get assignment
        Optional<Assignment> existingAssignment = assignmentDao.findAssignmentForPositionSlotAndUser(positionSlotId, currentUserId);
        // check status
        if (existingAssignment.isEmpty() || existingAssignment.get().getStatus() != AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN) {
            throw new IllegalArgumentException("Assignment not in request status");
        }
        Assignment assignment = existingAssignment.get();
        LockStatusHelper.assertIsSupervisedWithMessage(assignment, "withdraw leave request");
        // change back to signed-up state
        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignmentDao.save(assignment);

        // publish event
        publisher.publishEvent(PositionSlotVolunteerEvent.positionSlotLeaveRequestWithdrawn(assignment.getPositionSlot(), currentUserId.toString()));
    }

    private void notifyPlanners(PositionSlot slot, String type) {
        var plan = slot.getShift().getShiftPlan();
        var triggerId = java.util.UUID.randomUUID();
        if (plan.getPlanPlanners() == null) return;
        plan.getPlanPlanners().stream().map(Volunteer::getId).distinct().forEach(id ->
            notificationRequests.request(new NotificationRequest(triggerId, id,
                type, String.valueOf(plan.getEvent().getId()))));
    }

    private void assertJoinPossible(PositionSlot positionSlot, Volunteer volunteer) {
        // check access to position slot
        securityHelper.assertUserIsVolunteer(positionSlot, true);

        // check if already assigned, eligible and conflicts
        eligibilityService.validateSignUpStateForJoin(positionSlot, volunteer);
    }

    @Override
    public Collection<AssignmentDto> getAssignments(@NonNull Long positionSlotId) {
        PositionSlot positionSlot = positionSlotDao.getById(positionSlotId);
        securityHelper.assertUserIsPlanner(positionSlot);
        return assignmentAssemblingMapper.assemble(positionSlot.getAssignments());
    }

    @Override
    public AssignmentDto getUserAssignment(@NonNull Long positionSlotId, @NonNull UUID volunteerId) {
        Assignment ass = assignmentDao.getAssignmentForPositionSlotAndUser(positionSlotId, volunteerId);
        securityHelper.assertVolunteerIsNotLockedInPlan(ass);
        return assignmentAssemblingMapper.assemble(ass);
    }

    @Override
    public AssignmentDto createAuction(@NonNull Long positionSlotId, @NonNull UUID currentUserId) {
        Assignment assignment = assignmentDao.getAssignmentForPositionSlotAndUser(positionSlotId, currentUserId);
        securityHelper.assertVolunteerIsNotLockedInPlan(assignment);
        // no security check necessary, because user is already assigned to position,
        //  if not, assignment would not be found
        if (assignment.getStatus() == AssignmentStatus.REQUEST_FOR_ASSIGNMENT) {
            throw new IllegalStateException("auction not possible, not assigned to slot");
        }
        if (AssignmentStatus.ACTIVE_AUCTION_STATES.contains(assignment.getStatus())) {
            throw new IllegalStateException("position is already up for auction");
        }

        // check for signup phase
        LockStatusHelper.assertIsSupervisedWithMessage(assignment, "auction");

        // create auction
        assignment.setStatus(AssignmentStatus.AUCTION);
        assignment = assignmentDao.save(assignment);

        publisher.publishEvent(AssignmentEvent.auctionCreated(assignment));
        return assignmentAssemblingMapper.assemble(assignment);
    }

    @Override
    @Transactional
    public AssignmentDto claimAuction(@NonNull Long positionSlotId, @NonNull UUID offeringUserId, @NonNull UUID currentUserId,
                                      @NonNull PositionSlotRequestDto requestDto) {
        // get auction-assignment
        Assignment auction = assignmentDao.getAssignmentForPositionSlotAndUser(positionSlotId, offeringUserId);
        if (!AssignmentStatus.ACTIVE_AUCTION_STATES.contains(auction.getStatus())) {
            throw new BadRequestException("Assignment is not up for auction");
        }
        securityHelper.assertVolunteerIsNotLockedInPlan(auction);
        // check if current user is volunteer in plan
        securityHelper.assertUserIsVolunteer(auction.getPositionSlot(), true);
        // get current user (volunteer)
        Volunteer currentUser = volunteerDao.getById(currentUserId);
        // check for trade not necessary
        // check if user is eligible for the auction position slot
        eligibilityService.validateSignUpStateForAuction(auction.getPositionSlot(), currentUser);
        // cancel existing trades
        assignmentSwitchRequestDao.cancelTradesForPositionSlot(positionSlotId, offeringUserId);
        // execute claim
        Assignment claimedAuction = assignmentService.claimAuction(auction, currentUser, requestDto);

        return assignmentAssemblingMapper.assemble(claimedAuction);
    }

    @Override
    public AssignmentDto cancelAuction(@NonNull Long positionSlotId, @NonNull UUID userId) {
        Assignment assignment = assignmentDao.getAssignmentForPositionSlotAndUser(positionSlotId, userId);
        if (!assignment.getAssignedVolunteer().getId().equals(userId)) {
            securityHelper.assertUserIsPlanner(assignment.getPositionSlot());
        } else {
            securityHelper.assertUserIsVolunteer(assignment.getPositionSlot(), true);
        }
        if (!AssignmentStatus.ACTIVE_AUCTION_STATES.contains(assignment.getStatus())) {
            throw new BadRequestException("Assignment is not up for auction");
        }
        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment = assignmentDao.save(assignment);
        publisher.publishEvent(AssignmentEvent.auctionCanceled(assignment));
        return assignmentAssemblingMapper.assemble(assignment);
    }

    @Override
    public void setPreference(@NonNull UUID currentUserId, long positionSlotId, int preference) {
        PositionSlot positionSlot = positionSlotDao.getById(positionSlotId);
        securityHelper.assertUserIsVolunteer(positionSlot, true);
        if (preference < -10 || preference > 10) {
            throw new IllegalArgumentException("Preference must be between -10 and 10");
        }
        positionSlotDao.setPreference(currentUserId, positionSlotId, preference);
        publisher.publishEvent(PreferenceEvent.preferenceUpdated(currentUserId.toString(), preference, positionSlot));
    }

    @Override
    public int getPreference(@NonNull UUID currentUserId, long positionSlotId) {
        PositionSlot positionSlot = positionSlotDao.getById(positionSlotId);
        securityHelper.assertUserIsInPlan(positionSlot);
        return positionSlotDao.getPreference(currentUserId, positionSlotId);
    }

    @Override
    public PositionSlotDto createPositionSlot(@NonNull Long shiftId, @NonNull PositionSlotModificationDto modificationDto) {
        //VERIFY
        var shift = shiftDao.getById(shiftId);
        securityHelper.assertUserIsPlanner(shift);
        var positionSlot = PositionSlot.builder()
            .shift(shift)
            .build();
        validateUniqueNameInShift(modificationDto.getName(), shift, null);
        validateModificationDtoAndSetPositionSlotFields(modificationDto, positionSlot);

        //ACT: save position slot
        positionSlot = positionSlotDao.save(positionSlot);

        //NOTIFY: publish event
        publisher.publishEvent(PositionSlotEvent.positionSlotCreated(positionSlot));
        return positionSlotAssemblingMapper.assemble(positionSlot);
    }

    private void validateUniqueNameInShift(@NotNull @Size(max = 50) String name, Shift shift, Long excludePositionSlotId) {
        shift.getSlots().stream()
            .filter(slot -> slot.getName().equalsIgnoreCase(name)
                && (excludePositionSlotId == null || slot.getId() != excludePositionSlotId))
            .findAny()
            .ifPresent(_ -> {
                throw new ValidationException("A position slot with the name '%s' already exists in shift '%s'".formatted(name, shift.getName()));
            });
    }

    @Override
    public PositionSlotDto updatePositionSlot(@NonNull Long positionSlotId, @NonNull PositionSlotModificationDto modificationDto) {
        //VERIFY
        var positionSlot = positionSlotDao.getById(positionSlotId);
        securityHelper.assertUserIsPlanner(positionSlot);
        validateUniqueNameInShift(modificationDto.getName(), positionSlot.getShift(), positionSlot.getId());
        validateModificationDtoAndSetPositionSlotFields(modificationDto, positionSlot);

        //ACT: save position slot
        positionSlot = positionSlotDao.save(positionSlot);

        //NOTIFY: publish event
        publisher.publishEvent(PositionSlotEvent.positionSlotUpdated(positionSlot));
        return positionSlotAssemblingMapper.assemble(positionSlot);
    }

    private void validateModificationDtoAndSetPositionSlotFields(PositionSlotModificationDto modificationDto, @NonNull PositionSlot positionSlot) {
        if (modificationDto == null) {
            throw new IllegalArgumentException("Modification data must be provided");
        }

        if (positionSlot.getAssignments() != null && !positionSlot.getAssignments().isEmpty()) {
            long assignedCount = positionSlot.getAssignments().stream()
                .filter(a -> a.getStatus() != AssignmentStatus.REQUEST_FOR_ASSIGNMENT)
                .count();

            if (modificationDto.getDesiredVolunteerCount() < assignedCount) {
                throw new ValidationException("Desired volunteer count cannot be less than currently assigned volunteers");
            }
        }


        positionSlot.setName(modificationDto.getName());
        positionSlot.setDescription(modificationDto.getDescription());
        positionSlot.setSkipAutoAssignment(modificationDto.isSkipAutoAssignment());
        if (StringUtils.isNotBlank(modificationDto.getRoleId())) {
            var role = roleDao.getById(ConvertUtil.idToLong(modificationDto.getRoleId()));
            positionSlot.setRole(role);
        } else {
            positionSlot.setRole(null);
        }
        positionSlot.setDesiredVolunteerCount(modificationDto.getDesiredVolunteerCount());
        positionSlot.setOverrideRewardPoints(modificationDto.getOverrideRewardPoints());
    }

    @Override
    public void deletePositionSlot(@NonNull Long positionSlotId) {
        var positionSlot = positionSlotDao.getById(positionSlotId);
        securityHelper.assertUserIsPlanner(positionSlot);

        var positionSlotEvent = PositionSlotEvent.positionSlotDeleted(positionSlot);
        positionSlotDao.delete(positionSlot);
        publisher.publishEvent(positionSlotEvent);
    }
}
