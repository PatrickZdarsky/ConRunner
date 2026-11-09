package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.shiftcontrol.lib.entity.RewardPointsTransaction;
import at.shiftcontrol.lib.event.BaseEvent;
import at.shiftcontrol.lib.event.EventType;
import at.shiftcontrol.lib.event.RoutingKeys;
import at.shiftcontrol.lib.event.events.parts.RewardPointsTransactionPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class RewardPointTransactionEvent extends BaseEvent {
    private final RewardPointsTransactionPart rewardPointsTransactionPart;

    public RewardPointTransactionEvent(EventType eventType, String routingKey, RewardPointsTransactionPart rewardPointsTransactionPart) {
        super(eventType, routingKey);
        this.rewardPointsTransactionPart = rewardPointsTransactionPart;
    }

    public static RewardPointTransactionEvent ofInternal(EventType eventType, String routingKey, RewardPointsTransaction transaction) {
        return new RewardPointTransactionEvent(eventType, routingKey, RewardPointsTransactionPart.of(transaction));
    }

    public static RewardPointTransactionEvent transactionCreated(RewardPointsTransaction transaction) {
        return ofInternal(EventType.REWARDPOINTS_TRANSACTION_CREATED,
            RoutingKeys.format(RoutingKeys.REWARDPOINTS_TRANSACTION_CREATED, Map.of(
            "volunteerId", transaction.getVolunteerId(),
            "transactionId", transaction.getId())), transaction)
        .withDescription("Reward points transaction created for volunteer ID "
            + transaction.getVolunteerId() + " with transaction ID "
            + transaction.getId());
    }

    public static RewardPointTransactionEvent transactionFailed(RewardPointsTransaction transaction) {
        return ofInternal(EventType.REWARDPOINTS_TRANSACTION_FAILED,
            RoutingKeys.format(RoutingKeys.REWARDPOINTS_TRANSACTION_FAILED, Map.of(
            "volunteerId", transaction.getVolunteerId())), transaction
        ).withDescription("Reward points transaction failed for volunteer ID "
            + transaction.getVolunteerId() + " with transaction ID "
            + transaction.getId());
    }
}
