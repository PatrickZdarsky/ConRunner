package at.control.shiftcontrol.shift.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import at.shiftcontrol.lib.entity.Event;
import at.shiftcontrol.shiftservice.dto.event.EventSearchDto;
import at.shiftcontrol.shiftservice.dto.rows.PlanVolunteerIdRow;

public interface EventDao extends BasicDao<Event, Long> {
    List<Event> search(EventSearchDto searchDto);

    Collection<Event> getAllOpenEvents();

    Collection<Event> getAllOpenEventsForUser(String userId);

    Collection<Event> findAll();

    boolean existsByNameIgnoreCase(String name);

    Collection<PlanVolunteerIdRow> getPlannersForEventAndUser(long eventId, String userId);

    Optional<Event> findByName(String name);
}
