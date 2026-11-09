package at.control.shiftcontrol.shift.repo;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.dto.rows.PlanVolunteerIdRow;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    boolean existsByNameIgnoreCase(String name);

    @Query("""
        SELECT new at.control.shiftcontrol.shift.dto.rows.PlanVolunteerIdRow(
                p.id,
                p.name,
                v.id
            )
            FROM Volunteer v
            JOIN v.planningPlans p
            WHERE p.event.id = :eventId
              AND p IN (
                  SELECT vp
                  FROM Volunteer u
                  JOIN u.volunteeringPlans vp
                  WHERE u.id = :userId
              )
        """)
    Collection<PlanVolunteerIdRow> getPlannersForEventAndUser(long eventId, String userId);

    @Query("""
        SELECT DISTINCT e
        FROM Event e
        WHERE :now <= e.endTime
        """)
    Collection<Event> getAllOpenEvents(Instant now);

    @Query("""
    SELECT DISTINCT e
    FROM Event e
    WHERE :now <= e.endTime
      AND EXISTS (
            SELECT 1
            FROM Volunteer v
            JOIN v.volunteeringPlans vp
            WHERE v.id = :userId
              AND vp.event = e
        )
       OR EXISTS (
            SELECT 1
            FROM Volunteer v
            JOIN v.planningPlans pp
            WHERE v.id = :userId
              AND pp.event = e
        )
    """)
    Collection<Event> getAllOpenEventsForUser(String userId, Instant now);

    @Query("SELECT e FROM Event e WHERE e.name = :name")
    Optional<Event> findByName(String name);
}
