package at.conrunner.shift.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import at.conrunner.shift.entity.Event;
import at.conrunner.shift.dto.event.EventSearchDto;

public interface EventDao extends BasicDao<Event, Long> {
    List<Event> search(EventSearchDto searchDto);

    Collection<Event> getAllOpenEvents();

    Collection<Event> getOpenEventsByIds(Set<Long> ids);

    Collection<Event> findAll();

    boolean existsByNameIgnoreCase(String name);

    Optional<Event> findByName(String name);
}
