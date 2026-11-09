package at.control.shiftcontrol.shift.entity;

import java.time.Instant;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import at.control.shiftcontrol.shift.lib.type.RewardPointTransactionType;
import at.control.shiftcontrol.shift.lib.util.JsonMapConverter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reward_points_transaction",
    indexes = {
        @Index(name = "idx_rpt_volunteer", columnList = "volunteer_id"),
        @Index(name = "idx_rpt_volunteer_event", columnList = "volunteer_id,event_id"),
        @Index(name = "idx_rpt_volunteer_shiftplan", columnList = "volunteer_id,shift_plan_id"),
        @Index(name = "idx_rpt_slot", columnList = "position_slot_id")
    }
)
public class RewardPointsTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "volunteer_id", nullable = false)
    private String volunteerId;

    @Column(name = "event_id", nullable = false)
    private long eventId;

    @Column(name = "shift_plan_id")
    private Long shiftPlanId;

    @Column(name = "position_slot_id")
    private Long positionSlotId;

    @Column(nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardPointTransactionType type;

    @Column(name = "source_key", nullable = false, length = 255)
    private String sourceKey; // for idempotency (currently not used for unique constraint)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "CLOB")
    private Map<String, Object> metadata;
}
