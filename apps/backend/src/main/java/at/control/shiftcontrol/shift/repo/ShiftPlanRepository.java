package at.control.shiftcontrol.shift.repo;

import java.util.Collection;
import java.util.Set;

import at.shiftcontrol.lib.entity.ShiftPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftPlanRepository extends JpaRepository<ShiftPlan, Long>, JpaSpecificationExecutor<ShiftPlan> {
    @Query("SELECT sp FROM ShiftPlan sp WHERE sp.event.id = :eventId")
    Collection<ShiftPlan> findByEventId(Long eventId);

    @Query("""
        SELECT DISTINCT sp
        FROM ShiftPlan sp
        LEFT JOIN sp.planPlanners planner
        LEFT JOIN sp.planVolunteers volunteer
        WHERE (planner.id = :userId OR volunteer.id = :userId)
          AND sp.event.id = :eventId
        """)
    Collection<ShiftPlan> findAllUserRelatedShiftPlansInEvent(String userId, String eventId);

    @Query("""
        SELECT DISTINCT sp
        FROM ShiftPlan sp
        WHERE sp.id in :shiftPlanIds
        """)
    Collection<ShiftPlan> getByIds(Set<Long> shiftPlanIds);
}
