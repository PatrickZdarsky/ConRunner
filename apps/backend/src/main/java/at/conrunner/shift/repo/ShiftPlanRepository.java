package at.conrunner.shift.repo;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import at.conrunner.shift.dto.rows.PlanVolunteerIdRow;
import at.conrunner.shift.entity.ShiftPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftPlanRepository extends JpaRepository<ShiftPlan, Long>, JpaSpecificationExecutor<ShiftPlan> {
    @Query("""
        SELECT new at.conrunner.shift.dto.rows.PlanVolunteerIdRow(
                p.id,
                p.name,
                v.id
            )
            FROM Volunteer v
            JOIN v.planningPlans p
            WHERE p.event.id = :eventId
              AND p IN (
                  SELECT vp
                  FROM Volunteer u
                  JOIN u.volunteeringPlans vp
                  WHERE u.id = :userId
              )
        """)
    Collection<PlanVolunteerIdRow> getPlannersForEventAndUser(long eventId, UUID userId);

    @Query("""
        SELECT plan.event.id
        FROM Volunteer v
        JOIN v.volunteeringPlans plan
        WHERE v.id = :userId
        UNION
        SELECT plan.event.id
        FROM Volunteer v
        JOIN v.planningPlans plan
        WHERE v.id = :userId
        """)
    Set<Long> findEventIdsForUser(UUID userId);

    @Query("SELECT sp FROM ShiftPlan sp WHERE sp.event.id = :eventId")
    Collection<ShiftPlan> findByEventId(Long eventId);

    boolean existsByEventIdAndIdIn(long eventId, Set<Long> planIds);

    @Query("""
        SELECT DISTINCT sp
        FROM ShiftPlan sp
        LEFT JOIN sp.planPlanners planner
        LEFT JOIN sp.planVolunteers volunteer
        WHERE (planner.id = :userId OR volunteer.id = :userId)
          AND sp.event.id = :eventId
        """)
    Collection<ShiftPlan> findAllUserRelatedShiftPlansInEvent(java.util.UUID userId, String eventId);

    @Query("""
        SELECT DISTINCT sp
        FROM ShiftPlan sp
        WHERE sp.id in :shiftPlanIds
        """)
    Collection<ShiftPlan> getByIds(Set<Long> shiftPlanIds);
}
