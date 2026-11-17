package at.conrunner.schedule.change;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "schedule_change_entry")
public class ChangeEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "occurrence_id")
    private UUID occurrenceId;

    @Column(name = "reserved_occurrence_id")
    private UUID reservedOccurrenceId;

    @Column(name = "location_id")
    private UUID locationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChangeState state = ChangeState.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 32)
    private ChangeOperation operationType;

    @Column(name = "entry_order", nullable = false)
    private int entryOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload = JsonNodeFactory.instance.objectNode();

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion = 1;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "last_edited_by")
    private UUID lastEditedBy;
}
