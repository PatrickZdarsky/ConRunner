package at.conrunner.schedule.session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    List<SessionEntity> findAllByEventIdAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAscIdAsc(
            UUID eventId, Instant rangeEnd, Instant rangeStart);

    boolean existsByEventIdAndNameIgnoreCase(UUID eventId, String name);

    List<SessionEntity> findAllByEventIdOrderByStartTimeAsc(UUID eventId);
}
