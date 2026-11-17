package at.conrunner.schedule.location;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import at.conrunner.schedule.ScheduleRecordOrigin;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "eventcatalog_location")
public class LocationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(length = 1024)
    private String url;

    @Column(name = "read_only", nullable = false)
    private boolean readOnly;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScheduleRecordOrigin origin = ScheduleRecordOrigin.LOCAL;

    @Column(nullable = false)
    private boolean retired;

    @Column(name = "sort_position", nullable = false)
    private int sortPosition;

    @Version
    @Column(nullable = false)
    private long version;
}
