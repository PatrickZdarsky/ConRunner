package at.conrunner.schedule.event;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "schedule_signals")
public class ScheduleSignalsEntity {
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "collaboration_revision", nullable = false)
    private long collaborationRevision;

    @Column(name = "sync_revision", nullable = false)
    private long syncRevision;
}
