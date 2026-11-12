package at.conrunner.shift.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import at.conrunner.shift.entity.Location;
import at.conrunner.shift.dto.location.LocationSearchDto;

public interface LocationDao extends BasicDao<Location, Long> {
    Collection<Location> findAllByEventId(Long eventId);

    List<Location> search(long eventId, LocationSearchDto searchDto);

    Optional<Location> findByEventAndName(long eventId, String name);
}
