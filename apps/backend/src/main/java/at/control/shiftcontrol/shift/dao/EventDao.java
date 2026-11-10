package at.control.shiftcontrol.shift.dao;

import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.dto.event.EventSearchDto;
import at.control.shiftcontrol.shift.dto.rows.PlanVolunteerIdRow;

public interface EventDao extends BasicDao<Event, Long> {
    List<Event> search(EventSearchDto searchDto);

    Collection<Event> getAllOpenEvents();

    Collection<Event> getAllOpenEventsForUser(UUID userId);

    Collection<Event> findAll();

    boolean existsByNameIgnoreCase(String name);

    Collection<PlanVolunteerIdRow> getPlannersForEventAndUser(long eventId, UUID userId);

    Optional<Event> findByName(String name);
}
