package at.control.shiftcontrol.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.control.shiftcontrol.shift.entity.Location;
import at.control.shiftcontrol.shift.lib.event.BaseEvent;
import at.control.shiftcontrol.shift.lib.event.EventType;
import at.control.shiftcontrol.shift.lib.event.RoutingKeys;
import at.control.shiftcontrol.shift.lib.event.events.parts.LocationPart;

@Data
@EqualsAndHashCode(callSuper = true)
public class LocationEvent extends BaseEvent {
    private final LocationPart location;

    public LocationEvent(EventType eventType, String routingKey, LocationPart location) {
        super(eventType, routingKey);
        this.location = location;
    }

    public static LocationEvent ofInternal(EventType eventType, String routingKey, Location location) {
        return new LocationEvent(eventType, routingKey, LocationPart.of(location));
    }

    public static LocationEvent locationCreated(Location location) {
        return ofInternal(EventType.LOCATION_CREATED, RoutingKeys.LOCATION_CREATED, location);
    }

    public static LocationEvent locationUpdated(Location location) {
        return ofInternal(EventType.LOCATION_UPDATED,
            RoutingKeys.format(RoutingKeys.LOCATION_UPDATED,
            Map.of("locationId", String.valueOf(location.getId()))), location)
            .withDescription("Location updated: " + location.getName());
    }

    public static LocationEvent locationDeleted(Location location) {
        return ofInternal(EventType.LOCATION_DELETED,
            RoutingKeys.format(RoutingKeys.LOCATION_DELETED,
            Map.of("locationId", String.valueOf(location.getId()))), location)
            .withDescription("Location deleted: " + location.getName());
    }
}
