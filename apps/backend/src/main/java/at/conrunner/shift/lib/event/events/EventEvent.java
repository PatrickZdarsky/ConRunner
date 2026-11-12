package at.conrunner.shift.lib.event.events;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import at.conrunner.shift.entity.Event;
import at.conrunner.shift.lib.event.BaseEvent;
import at.conrunner.shift.lib.event.EventType;
import at.conrunner.shift.lib.event.RoutingKeys;
import at.conrunner.shift.lib.event.events.parts.EventPart;
import static at.conrunner.shift.lib.event.RoutingKeys.EVENT_CLONED;

@Data
@EqualsAndHashCode(callSuper = true)
public class EventEvent extends BaseEvent {
    private final EventPart event;

    public EventEvent(EventType eventType, String routingKey, EventPart event) {
        super(eventType, routingKey);
        this.event = event;
    }

    public static EventEvent ofInternal(EventType eventType, String routingKey, Event event) {
        return new EventEvent(eventType, routingKey, EventPart.of(event));
    }

    public static EventEvent eventCreated(Event event) {
        return ofInternal(EventType.EVENT_CREATED, RoutingKeys.EVENT_CREATED, event);
    }

    public static EventEvent eventUpdated(Event event) {
        return ofInternal(EventType.EVENT_UPDATED,
            RoutingKeys.format(RoutingKeys.EVENT_UPDATED,
            Map.of("eventId", String.valueOf(event.getId()))), event)
            .withDescription("Event updated: " + event.getName());
    }

    public static EventEvent eventDeleted(Event event) {
        return ofInternal(EventType.EVENT_DELETED,
            RoutingKeys.format(RoutingKeys.EVENT_DELETED, Map.of("eventId", String.valueOf(event.getId()))), event)
            .withDescription("Event deleted: " + event.getName());
    }

    public static EventEvent eventCloned(Event event, Long originalEventId) {
        return ofInternal(EventType.EVENT_CLONED,
            RoutingKeys.format(EVENT_CLONED, Map.of(
                "sourceEventId", String.valueOf(originalEventId),
                "newEventId", String.valueOf(event.getId()))),
            event).withDescription("Event cloned: " + event.getName());
    }

    public static EventEvent eventExported(Event event, String exportFormat) {
        return ofInternal(EventType.EVENT_EXPORTED,
                RoutingKeys.format(RoutingKeys.EVENT_EXPORTED, Map.of(
                "eventId", String.valueOf(event.getId()),
                "exportFormat", exportFormat)),
            event).withDescription("Event exported: " + event.getName() + " in format " + exportFormat);
    }

    public static EventEvent eventImported(Event event) {
        return ofInternal(EventType.EVENT_IMPORTED,
            RoutingKeys.format(RoutingKeys.EVENT_IMPORTED, Map.of(
            "eventId", String.valueOf(event.getId())
        )), event).withDescription("Event imported: " + event.getName());
    }
}
