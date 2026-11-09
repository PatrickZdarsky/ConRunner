package at.control.shiftcontrol.shift.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.lib.type.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentSwitchRequestRepository extends JpaRepository<AssignmentSwitchRequest, Long> {
    @Query("""
            SELECT a FROM AssignmentSwitchRequest a
            WHERE a.offeringAssignment.id = :offeredAssignmentId
            AND a.requestedAssignment.id = :requestedAssignmentId
        """)
    List<AssignmentSwitchRequest> findByAssignmentIds(long offeredAssignmentId, long requestedAssignmentId);

    @Query("""
            SELECT a FROM AssignmentSwitchRequest a
            WHERE a.offeringAssignment.positionSlot.id = :offeredSlotId
            AND a.offeringAssignment.assignedVolunteer.id = :offeringUserId
            AND a.requestedAssignment.positionSlot.id = :requestedSlotId
            AND a.requestedAssignment.assignedVolunteer.id = :requestedUserId
        """)
    Optional<AssignmentSwitchRequest> findBySlotsAndUsers(long offeredSlotId, String offeringUserId,
                                                          long requestedSlotId, String requestedUserId);

    @Modifying
    @Query("""
            UPDATE AssignmentSwitchRequest a
            SET a.status = :status
            WHERE EXISTS (
                SELECT 1
                FROM Assignment oa
                WHERE oa = a.offeringAssignment
                  AND oa.positionSlot.id = :positionSlotId
                  AND oa.assignedVolunteer.id = :assignedUser
            )
            OR EXISTS (
                SELECT 1
                FROM Assignment ra
                WHERE ra = a.requestedAssignment
                  AND ra.positionSlot.id = :positionSlotId
                  AND ra.assignedVolunteer.id = :assignedUser
            )
        """)
    void cancelTradesForAssignment(Long positionSlotId, String assignedUser, TradeStatus status);

    @Query("""
            SELECT a FROM AssignmentSwitchRequest a
            WHERE a.requestedAssignment.positionSlot.id = :positionSlotId
            AND a.offeringAssignment.assignedVolunteer.id = :userId
            AND a.status = :status
        """)
    Collection<AssignmentSwitchRequest> findOpenTradesForRequestedPositionAndOfferingUser(long positionSlotId, String userId, TradeStatus status);

    @Query("""
            SELECT a FROM AssignmentSwitchRequest a
            WHERE (a.offeringAssignment.positionSlot.shift.shiftPlan.id = :shiftPlanId
            AND a.offeringAssignment.assignedVolunteer.id = :userId)
            OR (a.requestedAssignment.positionSlot.shift.shiftPlan.id = :shiftPlanId
            AND a.requestedAssignment.assignedVolunteer.id = :userId)
        """)
    Collection<AssignmentSwitchRequest> findTradesForShiftPlanAndUser(long shiftPlanId, String userId);

    @Modifying
    @Query("""
            UPDATE AssignmentSwitchRequest a
            SET a.status = :status
            WHERE a.offeringAssignment IN (
                SELECT oa
                FROM Assignment oa
                WHERE oa.positionSlot.id = :positionSlotId
            )
            AND a.requestedAssignment IN (
                SELECT ra
                FROM Assignment ra
                WHERE ra.assignedVolunteer.id = :userId
            )
        """)
    void cancelTradesForOfferedPositionAndRequestedUser(long positionSlotId, String userId, TradeStatus status);

    @Query("""
        select r
        from AssignmentSwitchRequest r
        where r.status = :status
          and r.offeringAssignment.positionSlot.id = :slotId
          and r.requestedAssignment.assignedVolunteer.id = :userId
        """)
    Collection<AssignmentSwitchRequest> findOpenTradesForOfferingPositionAndRequestedOwner(
        @Param("slotId") long positionSlotId,
        @Param("userId") String userId,
        @Param("status") TradeStatus status
    );
}
