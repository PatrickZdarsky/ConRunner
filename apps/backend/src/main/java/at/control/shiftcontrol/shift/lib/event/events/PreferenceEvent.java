package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.lib.event.BaseEvent;
import at.control.shiftcontrol.shift.lib.event.EventType;
import at.control.shiftcontrol.shift.lib.event.RoutingKeys;
import at.control.shiftcontrol.shift.lib.event.events.parts.PositionSlotPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class PreferenceEvent extends BaseEvent {
    private final String volunteerId;
    private final int preferenceLevel;
    private final PositionSlotPart positionSlot;

    public PreferenceEvent(EventType eventType, String routingKey, String volunteerId, int preferenceLevel, PositionSlotPart positionSlot) {
        super(eventType, routingKey);
        this.volunteerId = volunteerId;
        this.preferenceLevel = preferenceLevel;
        this.positionSlot = positionSlot;
    }

    public static PreferenceEvent ofInternal(EventType eventType, String routingKey, String volunteerId, int preferenceLevel, PositionSlot positionSlot) {
        return new PreferenceEvent(eventType, routingKey, volunteerId, preferenceLevel, PositionSlotPart.of(positionSlot));
    }

    public static PreferenceEvent preferenceUpdated(String volunteerId, int preferenceLevel, PositionSlot positionSlot) {
        return ofInternal(EventType.POSITIONSLOT_PREFERENCE_UPDATED,
            RoutingKeys.format(RoutingKeys.POSITIONSLOT_PREFERENCE_UPDATED,
            Map.of("positionSlotId", String.valueOf(positionSlot.getId()),
                "volunteerId", volunteerId)), volunteerId, preferenceLevel, positionSlot)
            .withDescription("Preference updated for volunteer " + volunteerId + " on position slot " + positionSlot.getId());
    }
}
