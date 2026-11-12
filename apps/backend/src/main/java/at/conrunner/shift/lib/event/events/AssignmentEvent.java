package at.conrunner.shift.lib.event.events;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.lib.event.BaseEvent;
import at.conrunner.shift.lib.event.EventType;
import at.conrunner.shift.lib.event.RoutingKeys;
import at.conrunner.shift.lib.event.events.parts.AssignmentPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class AssignmentEvent extends BaseEvent {
    private final AssignmentPart assignment;

    @JsonCreator
    public AssignmentEvent(
        @JsonProperty("eventType") EventType eventType,
        @JsonProperty("routingKey") String routingKey,
        @JsonProperty("assignment") AssignmentPart assignment) {
        super(eventType, routingKey);
        this.assignment = assignment;
    }

    public static AssignmentEvent of(EventType eventType, String routingKey, Assignment assignment) {
        return new AssignmentEvent(eventType, routingKey, AssignmentPart.of(assignment));
    }

    public static AssignmentEvent auctionClaimed(Assignment auction, Assignment oldAuction, String oldVolunteerId) {
        return of(EventType.AUCTION_CLAIMED,
            RoutingKeys.format(RoutingKeys.AUCTION_CLAIMED, Map.of(
            "positionSlotId", String.valueOf(oldAuction.getPositionSlot().getId()),
            "oldVolunteerId", oldVolunteerId)), auction
        ).withDescription("Auction claimed for position slot ID "
            + auction.getPositionSlot().getId() + " by volunteer ID "
            + auction.getAssignedVolunteer().getId());
    }

    public static AssignmentEvent auctionCreated(Assignment auction) {
        return of(EventType.AUCTION_CREATED,
            RoutingKeys.format(RoutingKeys.AUCTION_CREATED,
                Map.of("positionSlotId", String.valueOf(auction.getPositionSlot().getId()))
            ), auction).withDescription("New auction created for position slot ID "
            + auction.getPositionSlot().getId());
    }

    public static AssignmentEvent auctionCanceled(Assignment auction) {
        return of(EventType.AUCTION_CANCELED,
            RoutingKeys.format(RoutingKeys.AUCTION_CANCELED,
                Map.of("positionSlotId", String.valueOf(auction.getPositionSlot().getId()))
            ), auction).withDescription("Auction canceled for position slot ID "
            + auction.getPositionSlot().getId());
    }
}
