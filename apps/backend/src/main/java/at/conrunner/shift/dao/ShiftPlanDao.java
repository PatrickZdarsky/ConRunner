package at.conrunner.shift.dao;

import java.util.UUID;
import java.util.Collection;
import java.util.Set;

import at.conrunner.shift.dto.rows.PlanVolunteerIdRow;
import at.conrunner.shift.entity.ShiftPlan;

public interface ShiftPlanDao extends BasicDao<ShiftPlan, Long> {
    Collection<ShiftPlan> findByEventId(Long eventId);

    boolean existsInEvent(long eventId, Set<Long> planIds);

    Set<Long> findEventIdsForUser(UUID userId);

    Collection<PlanVolunteerIdRow> getPlannersForEventAndUser(long eventId, UUID userId);

    Collection<ShiftPlan> findAll();

    Collection<ShiftPlan> findAllUserRelatedShiftPlansInEvent(UUID userId, String eventId);

    Collection<ShiftPlan> getByIds(Set<Long> shiftPlanIds);
}
