package at.control.shiftcontrol.shift.dao.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.entity.Assignment;
import at.shiftcontrol.lib.entity.PositionSlot;
import at.shiftcontrol.lib.exception.NotFoundException;
import at.shiftcontrol.lib.type.AssignmentStatus;
import at.shiftcontrol.shiftservice.dao.AssignmentDao;
import at.shiftcontrol.shiftservice.repo.AssignmentRepository;

@RequiredArgsConstructor
@Component
public class AssignmentDaoImpl implements AssignmentDao {
    private final AssignmentRepository assignmentRepository;

    @Override
    public @NonNull String getName() {
        return "Assignment";
    }

    @Override
    public @NonNull Optional<Assignment> findById(Long id) {
        return assignmentRepository.findById(id);
    }

    public Optional<Assignment> findBySlotAndUser(long positionSlotId, String assignedUserId) {
        return assignmentRepository.findBySlotAndUser(positionSlotId, assignedUserId);
    }

    @Override
    public Collection<Assignment> findSignupRequestsByShiftPlanId(long shiftPlanId) {
        return assignmentRepository.findSignupRequestsByShiftPlanId(shiftPlanId, AssignmentStatus.REQUEST_FOR_ASSIGNMENT);
    }

    @Override
    public Assignment save(Assignment entity) {
        return assignmentRepository.save(entity);
    }

    @Override
    public Collection<Assignment> saveAll(Collection<Assignment> entities) {
        return assignmentRepository.saveAll(entities);
    }

    @Override
    public void delete(Assignment entity) {
        assignmentRepository.delete(entity);
    }

    @Override
    public Collection<Assignment> findAuctionsByShiftPlanId(long shiftPlanId) {
        return assignmentRepository.findAuctionsByShiftPlanId(shiftPlanId, AssignmentStatus.ACTIVE_AUCTION_STATES);
    }

    @Override
    public Collection<Assignment> findAuctionsByShiftPlanIdExcludingUser(long shiftPlanId, String userId) {
        return assignmentRepository.findAuctionsByShiftPlanIdExcludingUser(shiftPlanId, userId, AssignmentStatus.ACTIVE_AUCTION_STATES);
    }

    @Override
    public Collection<Assignment> getConflictingAssignments(String volunteerId, Instant startTime, Instant endTime) {
        return assignmentRepository.getConflictingAssignments(volunteerId, startTime, endTime);
    }

    @Override
    public Collection<Assignment> getConflictingAssignments(String volunteerId, PositionSlot slot) {
        return getConflictingAssignments(volunteerId, slot.getShift().getStartTime(), slot.getShift().getEndTime());
    }

    @Override
    public Collection<Assignment> getActiveAssignmentsOfSlot(long positionSlotId) {
        return assignmentRepository.getAssignmentsOfSlotNotInState(positionSlotId, AssignmentStatus.REQUEST_FOR_ASSIGNMENT);
    }

    @Override
    public Collection<Assignment> getConflictingAssignmentsExcludingShift(String volunteerId, Instant startTime, Instant endTime, long shiftId) {
        return assignmentRepository.getConflictingAssignmentsExcludingShift(volunteerId, startTime, endTime, shiftId);
    }

    @Override
    public Collection<Assignment> getConflictingAssignmentsExcludingSlot(String volunteerId, Instant startTime, Instant endTime, long slotId) {
        return assignmentRepository.getConflictingAssignmentsExcludingSlot(volunteerId, startTime, endTime, slotId);
    }

    @Override
    public Collection<Assignment> getConflictingAssignmentsExcludingSlot(String volunteerId, PositionSlot slot) {
        return getConflictingAssignmentsExcludingSlot(volunteerId, slot.getShift().getStartTime(), slot.getShift().getEndTime(), slot.getId());
    }

    @Override
    public @org.jspecify.annotations.NonNull Assignment getAssignmentForPositionSlotAndUser(long positionSlotId, String userId) {
        return assignmentRepository.findAssignmentForPositionSlotAndUser(positionSlotId, userId)
            .orElseThrow(() -> new NotFoundException("Not assigned to position slot."));
    }

    @Override
    public Optional<Assignment> findAssignmentForPositionSlotAndUser(long positionSlotId, String userId) {
        return assignmentRepository.findAssignmentForPositionSlotAndUser(positionSlotId, userId);
    }

    @Override
    public Collection<Assignment> getAssignmentForPositionSlotAndUsers(long positionSlotId, Collection<String> userIds) {
        return assignmentRepository.findAssignmentForPositionSlotAndUsers(positionSlotId, userIds);
    }

    @Override
    public Collection<Assignment> findAssignmentsForShiftPlanAndUser(long shiftPlanId, String userId) {
        return assignmentRepository.findAssignmentsForShiftPlanAndUser(shiftPlanId, userId);
    }

    @Override
    public Collection<Assignment> findActiveAssignmentsForShiftPlanAndUser(long shiftPlanId, String userId) {
        return assignmentRepository.findActiveAssignmentsForShiftPlanAndUser(shiftPlanId, userId, AssignmentStatus.REQUEST_FOR_ASSIGNMENT);
    }

    @Override
    public void deleteAll(Collection<Assignment> ids) {
        assignmentRepository.deleteAll(ids);
    }
}
