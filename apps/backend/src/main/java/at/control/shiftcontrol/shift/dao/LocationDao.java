package at.control.shiftcontrol.shift.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import at.shiftcontrol.lib.entity.Location;
import at.shiftcontrol.shiftservice.dto.location.LocationSearchDto;

public interface LocationDao extends BasicDao<Location, Long> {
    Collection<Location> findAllByEventId(Long eventId);

    List<Location> search(long eventId, LocationSearchDto searchDto);

    Optional<Location> findByEventAndName(long eventId, String name);
}
