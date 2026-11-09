package at.control.shiftcontrol.shift.lib.event.events.parts;

import java.time.Instant;
import java.util.Map;

import at.shiftcontrol.lib.entity.RewardPointsTransaction;
import at.shiftcontrol.lib.type.RewardPointTransactionType;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class RewardPointsTransactionPart {
    private String volunteerId;
    private String eventId;
    private Long positionSlotId;
    private int points;
    private RewardPointTransactionType type;

    private Instant createdAt;
    private Map<String, Object> metadata;


    @NonNull
    public static RewardPointsTransactionPart of(@NonNull RewardPointsTransaction transaction) {
        return RewardPointsTransactionPart.builder()
            .volunteerId(transaction.getVolunteerId())
            .eventId(String.valueOf(transaction.getEventId()))
            .positionSlotId(transaction.getPositionSlotId())
            .points(transaction.getPoints())
            .type(transaction.getType())
            .createdAt(transaction.getCreatedAt())
            .metadata(transaction.getMetadata())
            .build();
    }
}
