package at.conrunner.eventcatalog.session;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    boolean existsByEventIdAndNameIgnoreCase(UUID eventId, String name);

    List<SessionEntity> findAllByEventIdOrderByStartTimeAsc(UUID eventId);
}
