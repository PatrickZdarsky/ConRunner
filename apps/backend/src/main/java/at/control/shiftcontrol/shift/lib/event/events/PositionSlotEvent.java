package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.lib.event.BaseEvent;
import at.control.shiftcontrol.shift.lib.event.EventType;
import at.control.shiftcontrol.shift.lib.event.RoutingKeys;
import at.control.shiftcontrol.shift.lib.event.events.parts.PositionSlotPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class PositionSlotEvent extends BaseEvent {
    private final PositionSlotPart positionSlot;

    @JsonCreator
    public PositionSlotEvent(
        @JsonProperty("eventType") EventType eventType,
        @JsonProperty("routingKey") String routingKey,
        @JsonProperty("positionSlot") PositionSlotPart positionSlot) {
        super(eventType, routingKey);
        this.positionSlot = positionSlot;
    }

    public static PositionSlotEvent ofInternal(EventType eventType, String routingKey, PositionSlot positionSlot) {
        return new PositionSlotEvent(eventType, routingKey, PositionSlotPart.of(positionSlot));
    }

    public static PositionSlotEvent positionSlotCreated(PositionSlot positionSlot) {
        return ofInternal(EventType.POSITIONSLOT_CREATED, RoutingKeys.POSITIONSLOT_CREATED, positionSlot)
            .withDescription("New position slot created: " + positionSlot.getName());
    }

    public static PositionSlotEvent positionSlotUpdated(PositionSlot positionSlot) {
        return ofInternal(EventType.POSITIONSLOT_UPDATED,
            RoutingKeys.format(RoutingKeys.POSITIONSLOT_UPDATED,
            Map.of("positionSlotId", String.valueOf(positionSlot.getId()))), positionSlot)
            .withDescription("Position slot updated: " + positionSlot.getName());
    }

    public static PositionSlotEvent positionSlotDeleted(PositionSlot positionSlot) {
        return ofInternal(EventType.POSITIONSLOT_DELETED,
            RoutingKeys.format(RoutingKeys.POSITIONSLOT_DELETED,
            Map.of("positionSlotId", String.valueOf(positionSlot.getId()))), positionSlot)
            .withDescription("Position slot deleted: " + positionSlot.getName());
    }
}
