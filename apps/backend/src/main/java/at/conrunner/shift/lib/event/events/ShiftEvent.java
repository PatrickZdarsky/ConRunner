package at.conrunner.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.lib.event.BaseEvent;
import at.conrunner.shift.lib.event.EventType;
import at.conrunner.shift.lib.event.RoutingKeys;
import at.conrunner.shift.lib.event.events.parts.ShiftPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShiftEvent extends BaseEvent {
    private final ShiftPart shift;

    public ShiftEvent(EventType eventType, String routingKey, ShiftPart shift) {
        super(eventType, routingKey);
        this.shift = shift;
    }

    public static ShiftEvent ofInternal(EventType eventType, String routingKey, Shift shift) {
        return new ShiftEvent(eventType, routingKey, ShiftPart.of(shift));
    }

    public static ShiftEvent shiftCreated(Shift shift) {
        return ofInternal(EventType.SHIFT_CREATED, RoutingKeys.SHIFT_CREATED, shift)
            .withDescription("New shift created: " + shift.getId());
    }

    public static ShiftEvent shiftUpdated(Shift shift) {
        return ofInternal(EventType.SHIFT_UPDATED, RoutingKeys.format(RoutingKeys.SHIFT_UPDATED, Map.of("shiftId", String.valueOf(shift.getId()))), shift)
            .withDescription("Shift updated: " + shift.getId());
    }

    public static ShiftEvent shiftDeleted(Shift shift) {
        return ofInternal(EventType.SHIFT_DELETED, RoutingKeys.format(RoutingKeys.SHIFT_DELETED, Map.of("shiftId", String.valueOf(shift.getId()))), shift)
            .withDescription("Shift deleted: " + shift.getId());
    }
}
