package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.shiftcontrol.lib.entity.RewardPointsShareToken;
import at.shiftcontrol.lib.event.BaseEvent;
import at.shiftcontrol.lib.event.EventType;
import at.shiftcontrol.lib.event.RoutingKeys;
import at.shiftcontrol.shiftservice.event.events.parts.RewardPointsShareTokenPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class RewardPointsShareTokenEvent extends BaseEvent {
    private final RewardPointsShareTokenPart rewardPointsShareTokenPart;

    public RewardPointsShareTokenEvent(EventType eventType, String routingKey, RewardPointsShareTokenPart rewardPointsShareTokenPart) {
        super(eventType, routingKey);
        this.rewardPointsShareTokenPart = rewardPointsShareTokenPart;
    }

    public static RewardPointsShareTokenEvent ofInternal(EventType eventType, String routingKey, RewardPointsShareToken shareToken) {
        return new RewardPointsShareTokenEvent(eventType, routingKey, RewardPointsShareTokenPart.of(shareToken));
    }

    public static RewardPointsShareTokenEvent shareTokenCreated(RewardPointsShareToken shareToken) {
        return ofInternal(EventType.REWARDPOINTS_SHARETOKEN_CREATED,
            RoutingKeys.format(RoutingKeys.REWARDPOINTS_SHARETOKEN_CREATED,
            Map.of("shareTokenId", String.valueOf(shareToken.getId()))), shareToken)
            .withDescription("New reward points share token created: " + shareToken.getId());
    }

    public static RewardPointsShareTokenEvent shareTokenDeleted(RewardPointsShareToken shareToken) {
        return ofInternal(EventType.REWARDPOINTS_SHARETOKEN_DELETED,
            RoutingKeys.format(RoutingKeys.REWARDPOINTS_SHARETOKEN_DELETED,
                Map.of("shareTokenId", String.valueOf(shareToken.getId()))), shareToken)
            .withDescription("Reward points share token deleted: " + shareToken.getId());
    }
}
