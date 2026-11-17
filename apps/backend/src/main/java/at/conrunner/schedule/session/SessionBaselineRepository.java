package at.conrunner.schedule.session;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionBaselineRepository extends JpaRepository<SessionBaselineEntity, UUID> {
    List<SessionBaselineEntity> findAllByEventId(UUID eventId);
}
