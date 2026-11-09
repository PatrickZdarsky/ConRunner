package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import at.shiftcontrol.lib.entity.AssignmentSwitchRequest;
import at.shiftcontrol.lib.event.BaseEvent;
import at.shiftcontrol.lib.event.EventType;
import at.shiftcontrol.lib.event.RoutingKeys;
import at.shiftcontrol.lib.event.events.parts.TradePart;

@Data
@EqualsAndHashCode(callSuper = true)
public class TradeEvent extends BaseEvent {
    private final TradePart trade;

    @JsonCreator
    public TradeEvent(
        @JsonProperty("eventType") EventType eventType,
        @JsonProperty("routingKey") String routingKey,
        @JsonProperty("trade") TradePart trade) {
        super(eventType, routingKey);
        this.trade = trade;
    }

    public static TradeEvent ofInternal(EventType eventType, String routingKey, AssignmentSwitchRequest trade) {
        return new TradeEvent(eventType, routingKey, TradePart.of(trade));
    }

    public static TradeEvent tradeCanceled(AssignmentSwitchRequest trade) {
        return ofInternal(EventType.TRADE_REQUEST_CREATED,
            RoutingKeys.format(RoutingKeys.TRADE_REQUEST_CREATED,
            Map.of("requestedVolunteerId", trade.getRequestedAssignment().getAssignedVolunteer().getId(),
                "offeringVolunteerId", trade.getOfferingAssignment().getAssignedVolunteer().getId())), trade
        ).withDescription("Trade request canceled between volunteers "
            + trade.getRequestedAssignment().getAssignedVolunteer().getId() + " and "
            + trade.getOfferingAssignment().getAssignedVolunteer().getId());
    }

    public static TradeEvent tradeDeclined(AssignmentSwitchRequest trade) {
        return ofInternal(EventType.TRADE_REQUEST_DECLINED,
            RoutingKeys.format(RoutingKeys.TRADE_REQUEST_DECLINED,
            Map.of("requestedVolunteerId", trade.getRequestedAssignment().getAssignedVolunteer().getId(),
                "offeringVolunteerId", trade.getOfferingAssignment().getAssignedVolunteer().getId())), trade
        ).withDescription("Trade request declined between volunteers "
            + trade.getRequestedAssignment().getAssignedVolunteer().getId() + " and "
            + trade.getOfferingAssignment().getAssignedVolunteer().getId());
    }
}
