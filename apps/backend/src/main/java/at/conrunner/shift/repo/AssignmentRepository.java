package at.conrunner.shift.repo;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.lib.type.AssignmentStatus;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    @Query("""
            SELECT a FROM Assignment a
            WHERE a.positionSlot.id = :positionSlotId
            AND a.assignedVolunteer.id = :assignedUser
        """)
    Optional<Assignment> findBySlotAndUser(long positionSlotId, java.util.UUID assignedUser);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.shift.shiftPlan.id = :spId AND a.status = :requestUnassign
        """)
    Collection<Assignment> findSignupRequestsByShiftPlanId(long spId, AssignmentStatus requestUnassign);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.shift.shiftPlan.id = :spId AND a.status IN :auctionStates
        """)
    Collection<Assignment> findAuctionsByShiftPlanId(long spId, Set<AssignmentStatus> auctionStates);

    // TODO delete after trade and auction are fully implemented and method is still not used
    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.shift.shiftPlan.id = :spId
        AND a.status IN :auctionStates
        AND a.assignedVolunteer.id != :userId
        """)
    Collection<Assignment> findAuctionsByShiftPlanIdExcludingUser(long spId, java.util.UUID userId, Set<AssignmentStatus> auctionStates);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.assignedVolunteer.id = :volunteerId
        AND a.positionSlot.shift.endTime > :startTime
        AND a.positionSlot.shift.startTime < :endTime
        """)
    Collection<Assignment> getConflictingAssignments(java.util.UUID volunteerId, Instant startTime, Instant endTime);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.assignedVolunteer.id = :volunteerId
        AND a.positionSlot.shift.id <> :shiftId
        AND a.positionSlot.shift.endTime > :startTime
        AND a.positionSlot.shift.startTime < :endTime
        """)
    Collection<Assignment> getConflictingAssignmentsExcludingShift(java.util.UUID volunteerId, Instant startTime, Instant endTime, long shiftId);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.assignedVolunteer.id = :volunteerId
        AND a.positionSlot.id <> :slotId
        AND a.positionSlot.shift.endTime > :startTime
        AND a.positionSlot.shift.startTime < :endTime
        """)
    Collection<Assignment> getConflictingAssignmentsExcludingSlot(java.util.UUID volunteerId, Instant startTime, Instant endTime, long slotId);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.id = :positionSlotId AND a.status != :excludedStatus
        """)
    Collection<Assignment> getAssignmentsOfSlotNotInState(long positionSlotId, AssignmentStatus excludedStatus);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.id = :positionSlotId AND a.assignedVolunteer.id = :userId
        """)
    Optional<Assignment> findAssignmentForPositionSlotAndUser(long positionSlotId, java.util.UUID userId);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.id = :positionSlotId AND a.assignedVolunteer.id IN :userIds
        """)
    Collection<Assignment> findAssignmentForPositionSlotAndUsers(long positionSlotId, Collection<java.util.UUID> userIds);


    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.shift.shiftPlan.id = :shiftPlanId
        AND a.assignedVolunteer.id = :userIds
        AND a.status != :status
        """)
    Collection<Assignment> findActiveAssignmentsForShiftPlanAndUser(long shiftPlanId, java.util.UUID userIds, AssignmentStatus status);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.shift.shiftPlan.id = :shiftPlanId AND a.assignedVolunteer.id = :userId
        """)
    Collection<Assignment> findAssignmentsForShiftPlanAndUser(long shiftPlanId, java.util.UUID userId);

    @Query("""
        SELECT DISTINCT a
        FROM Assignment a
        JOIN FETCH a.positionSlot ps
        JOIN FETCH ps.shift s
        JOIN FETCH s.shiftPlan sp
        JOIN FETCH sp.event e
        LEFT JOIN FETCH s.location l
        WHERE a.assignedVolunteer.id = :volunteerId
        AND a.status IN :statuses
        ORDER BY s.startTime, s.id, ps.id
        """)
    Collection<Assignment> findCalendarAssignmentsByVolunteerId(java.util.UUID volunteerId, Collection<AssignmentStatus> statuses);
}
