package at.control.shiftcontrol.shift.lib.event.events.parts;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import at.shiftcontrol.lib.entity.AssignmentSwitchRequest;
import at.shiftcontrol.lib.type.TradeStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradePart {
    @NotNull
    private AssignmentPart offeringAssignment;
    @NotNull
    private AssignmentPart requestedAssignment;
    @NotNull
    private TradeStatus status;
    @NotNull
    private Instant createdAt;

    @NonNull
    public static TradePart of(@NonNull AssignmentSwitchRequest tradeRequest) {
        return new TradePart(
            AssignmentPart.of(tradeRequest.getOfferingAssignment()),
            AssignmentPart.of(tradeRequest.getRequestedAssignment()),
            tradeRequest.getStatus(),
            tradeRequest.getCreatedAt()
        );
    }
}
