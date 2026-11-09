package at.control.shiftcontrol.shift.repo;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    @Query("""
            SELECT a FROM Assignment a
            WHERE a.positionSlot.id = :positionSlotId
            AND a.assignedVolunteer.id = :assignedUser
        """)
    Optional<Assignment> findBySlotAndUser(long positionSlotId, String assignedUser);

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
    Collection<Assignment> findAuctionsByShiftPlanIdExcludingUser(long spId, String userId, Set<AssignmentStatus> auctionStates);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.assignedVolunteer.id = :volunteerId
        AND a.positionSlot.shift.endTime > :startTime
        AND a.positionSlot.shift.startTime < :endTime
        """)
    Collection<Assignment> getConflictingAssignments(String volunteerId, Instant startTime, Instant endTime);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.assignedVolunteer.id = :volunteerId
        AND a.positionSlot.shift.id <> :shiftId
        AND a.positionSlot.shift.endTime > :startTime
        AND a.positionSlot.shift.startTime < :endTime
        """)
    Collection<Assignment> getConflictingAssignmentsExcludingShift(String volunteerId, Instant startTime, Instant endTime, long shiftId);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.assignedVolunteer.id = :volunteerId
        AND a.positionSlot.id <> :slotId
        AND a.positionSlot.shift.endTime > :startTime
        AND a.positionSlot.shift.startTime < :endTime
        """)
    Collection<Assignment> getConflictingAssignmentsExcludingSlot(String volunteerId, Instant startTime, Instant endTime, long slotId);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.id = :positionSlotId AND a.status != :excludedStatus
        """)
    Collection<Assignment> getAssignmentsOfSlotNotInState(long positionSlotId, AssignmentStatus excludedStatus);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.id = :positionSlotId AND a.assignedVolunteer.id = :userId
        """)
    Optional<Assignment> findAssignmentForPositionSlotAndUser(long positionSlotId, String userId);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.id = :positionSlotId AND a.assignedVolunteer.id IN :userIds
        """)
    Collection<Assignment> findAssignmentForPositionSlotAndUsers(long positionSlotId, Collection<String> userIds);


    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.shift.shiftPlan.id = :shiftPlanId
        AND a.assignedVolunteer.id IN :userIds
        AND a.status != :status
        """)
    Collection<Assignment> findActiveAssignmentsForShiftPlanAndUser(long shiftPlanId, String userIds, AssignmentStatus status);

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.positionSlot.shift.shiftPlan.id = :shiftPlanId AND a.assignedVolunteer.id = :userId
        """)
    Collection<Assignment> findAssignmentsForShiftPlanAndUser(long shiftPlanId, String userId);

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
    Collection<Assignment> findCalendarAssignmentsByVolunteerId(String volunteerId, Collection<AssignmentStatus> statuses);
}
