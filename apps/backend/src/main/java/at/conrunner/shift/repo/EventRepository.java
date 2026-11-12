package at.conrunner.shift.repo;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import at.conrunner.shift.entity.Event;

@Repository("shiftEventRepository")
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    boolean existsByNameIgnoreCase(String name);

    @Query("""
        SELECT DISTINCT e
        FROM Event e
        WHERE :now <= e.endTime
        """)
    Collection<Event> getAllOpenEvents(Instant now);

    @Query("""
        SELECT e FROM Event e WHERE e.id IN :ids AND e.endTime >= :now
        """)
    Collection<Event> getOpenEventsByIds(Set<Long> ids, Instant now);

    @Query("SELECT e FROM Event e WHERE e.name = :name")
    Optional<Event> findByName(String name);
}
