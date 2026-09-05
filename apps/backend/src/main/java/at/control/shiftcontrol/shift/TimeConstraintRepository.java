package at.control.shiftcontrol.shift;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TimeConstraintRepository extends JpaRepository<TimeConstraintEntity, UUID> {
    Collection<TimeConstraintEntity> findAllByEventIdAndUserId(UUID eventId, UUID userId);

    @Query("""
        select (count(constraint) > 0) from TimeConstraintEntity constraint
        where constraint.eventId = :eventId and constraint.userId = :userId
          and constraint.startTime < :endTime and constraint.endTime > :startTime
        """)
    boolean existsOverlapping(UUID eventId, UUID userId, Instant startTime, Instant endTime);

    @Query("""
        select (count(constraint) > 0) from TimeConstraintEntity constraint
        where constraint.eventId = :eventId and constraint.userId = :userId and constraint.type = :type
          and constraint.startTime < :endTime and constraint.endTime > :startTime
        """)
    boolean existsOverlappingByType(UUID eventId, UUID userId, TimeConstraintType type,
                                    Instant startTime, Instant endTime);
}
