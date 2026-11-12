package at.conrunner.shift.repo;

import java.util.Collection;
import java.util.Optional;

import at.conrunner.shift.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {
    @Query("""
        SELECT a FROM Activity a
        WHERE a.location.id = :locationId
        """)
    Collection<Activity> findAllByLocationId(Long locationId);

    @Query("""
        SELECT a FROM Activity a
        WHERE a.event.id = :eventId
        """)
    Collection<Activity> findAllByEventId(Long eventId);


    @Query("""
        SELECT a
        FROM Activity a
        WHERE a.location IS NULL
        AND a.event.id = :eventId
        """)
    Collection<Activity> findAllWithoutLocationByEventId(Long eventId);

    @Query("""
        SELECT a
        FROM Activity a
        WHERE a.event.id = :eventId
        AND a.name = :name
        """)
    Optional<Activity> findByEventAndName(Long eventId, String name);
}
