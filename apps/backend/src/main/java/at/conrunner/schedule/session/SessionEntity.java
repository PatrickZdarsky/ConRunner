package at.conrunner.schedule.session;

import java.time.Instant;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import at.conrunner.schedule.ScheduleRecordOrigin;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "eventcatalog_session")
public class SessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "read_only", nullable = false)
    private boolean readOnly;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScheduleRecordOrigin origin = ScheduleRecordOrigin.LOCAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "effective_status", nullable = false, length = 16)
    private SessionStatus effectiveStatus = SessionStatus.SCHEDULED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "effective_content", nullable = false, columnDefinition = "jsonb")
    private JsonNode effectiveContent = JsonNodeFactory.instance.objectNode();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_removal_snapshot", columnDefinition = "jsonb")
    private JsonNode sourceRemovalSnapshot;

    @Version
    @Column(nullable = false)
    private long version;
}
