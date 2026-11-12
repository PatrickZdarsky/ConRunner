package at.conrunner.shift.repo;

import java.util.List;

import at.conrunner.shift.entity.RewardPointsTransaction;
import at.conrunner.shift.dto.rewardpoints.EventPointsInternalDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RewardPointTransactionRepository extends JpaRepository<RewardPointsTransaction, Long> {
    /**
     * Global points for a volunteer: SUM(points).
     */
    @Query("""
            select coalesce(sum(t.points), 0)
            from RewardPointsTransaction t
            where t.volunteerId = :volunteerId
        """)
    long sumPointsByVolunteer(String volunteerId);

    /**
     * Points for a volunteer within one event: SUM(points) filtered by event.
     */
    @Query("""
            select coalesce(sum(t.points), 0)
            from RewardPointsTransaction t
            where t.volunteerId = :volunteerId
              and t.eventId = :eventId
        """)
    long sumPointsByVolunteerAndEvent(String volunteerId, Long eventId);

    @Query("""
            select coalesce(sum(t.points), 0)
            from RewardPointsTransaction t
            where t.volunteerId = :volunteerId
              and t.shiftPlanId = :shiftPlanId
        """)
    long sumPointsByVolunteerAndShiftPlan(String volunteerId, Long shiftPlanId);

    /**
     * Points per event for a volunteer: GROUP BY event.
     */
    @Query("""
            select new at.conrunner.shift.dto.rewardpoints.EventPointsInternalDto(
                t.eventId, coalesce(sum(t.points), 0)
            )
            from RewardPointsTransaction t
            where t.volunteerId = :volunteerId
            group by t.eventId
            order by t.eventId
        """)
    List<EventPointsInternalDto> sumPointsForUserGroupedByEvent(String volunteerId);

    List<RewardPointsTransaction> findAllByVolunteerIdOrderByCreatedAtAsc(String volunteerId);
}
