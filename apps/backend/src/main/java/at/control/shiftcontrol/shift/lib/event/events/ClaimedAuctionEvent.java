package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.lib.event.EventType;
import at.control.shiftcontrol.shift.lib.event.RoutingKeys;
import at.control.shiftcontrol.shift.lib.event.events.parts.AssignmentPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class ClaimedAuctionEvent extends AssignmentEvent {
    private final String oldVolunteerId;

    @JsonCreator
    public ClaimedAuctionEvent(
        @JsonProperty("eventType") EventType eventType,
        @JsonProperty("routingKey") String routingKey,
        @JsonProperty("assignment") AssignmentPart assignment,
        @JsonProperty("oldVolunteerId") String oldVolunteerId) {
        super(eventType, routingKey, assignment);
        this.oldVolunteerId = oldVolunteerId;
    }

    public static ClaimedAuctionEvent of(EventType eventType, String routingKey, Assignment assignment, String oldVolunteer) {
        return new ClaimedAuctionEvent(eventType, routingKey, AssignmentPart.of(assignment), oldVolunteer);
    }

    public static ClaimedAuctionEvent auctionClaimed(Assignment auction, Assignment oldAuction, String oldVolunteerId) {
        return of(EventType.AUCTION_CLAIMED,
            RoutingKeys.format(RoutingKeys.AUCTION_CLAIMED, Map.of(
                "positionSlotId", String.valueOf(oldAuction.getPositionSlot().getId()),
                "oldVolunteerId", oldVolunteerId)), auction, oldVolunteerId
        ).withDescription("Auction claimed for position slot ID "
            + auction.getPositionSlot().getId() + " by volunteer ID "
            + auction.getAssignedVolunteer().getId());
    }
}
