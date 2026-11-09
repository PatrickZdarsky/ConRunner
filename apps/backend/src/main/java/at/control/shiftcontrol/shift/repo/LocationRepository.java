package at.control.shiftcontrol.shift.repo;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import at.shiftcontrol.lib.entity.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long>, JpaSpecificationExecutor<Location> {
    @Query("SELECT l FROM Location l WHERE l.event.id = :eventId")
    Collection<Location> findAllByEventId(Long eventId);

    @Query("SELECT l FROM Location l WHERE l.event.id = :eventId AND l.name = :name")
    Optional<Location> findByEventAndName(long eventId, String name);
}
