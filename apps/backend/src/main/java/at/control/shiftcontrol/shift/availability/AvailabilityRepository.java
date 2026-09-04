package at.control.shiftcontrol.shift.availability;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityRepository extends JpaRepository<AvailabilityEntity, UUID> {
    @Query("""
        select (count(availability) > 0)
        from AvailabilityEntity availability
        where availability.eventId = :eventId
          and availability.userId = :userId
          and availability.startTime < :endTime
          and availability.endTime > :startTime
        """)
    boolean existsOverlapping(UUID eventId, UUID userId, Instant startTime, Instant endTime);
}
