package at.conrunner.schedule.event;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleSignalsRepository extends JpaRepository<ScheduleSignalsEntity, UUID> {
}
