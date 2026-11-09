package at.control.shiftcontrol.shift.dao.userprofile;

import java.util.Collection;
import java.util.Optional;

import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.dao.BasicDao;

public interface VolunteerDao extends BasicDao<Volunteer, String> {
    Collection<Volunteer> findAllByShiftPlan(long shiftPlanId);

    Collection<String> findAllIdsByShiftPlan(long shiftPlanId);

    Collection<Volunteer> findAllPlannersByShiftPlan(long id);

    Collection<Volunteer> findAllByEvent(long eventId);

    Collection<Volunteer> findAllPlannersByEvent(long eventId);

    Collection<Volunteer> findAllByVolunteerIds(Collection<String> volunteerIds);

    Collection<Volunteer> findAllByPlannerIds(Collection<String> plannerIds);

    Collection<Volunteer> findAllByShiftPlanAndVolunteerIds(long shiftPlanId, Collection<String> volunteerIds);

    Collection<Volunteer> findAllByEventAndVolunteerIds(long eventId, Collection<String> volunteerIds);

    Collection<Volunteer> findAllByShiftPlanAndPlannerIds(long shiftPlanId, Collection<String> plannerIds);

    Collection<Volunteer> findAllByEventAndPlannerIds(long eventId, Collection<String> plannerIds);

    Collection<Volunteer> findAllPaginated(long page, long size);

    Collection<Volunteer> findAllByShiftPlanPaginated(long page, long size, long shiftPlanId);

    long findAllSize();
    long findAllByShiftPlanSize(long shiftPlanId);

    Optional<Volunteer> findByCalendarShareId(String calendarShareId);

    boolean existsByCalendarShareId(String calendarShareId);
}
