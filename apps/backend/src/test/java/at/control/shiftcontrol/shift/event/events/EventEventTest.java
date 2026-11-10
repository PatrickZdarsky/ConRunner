package at.control.shiftcontrol.shift.event.events;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.lib.event.events.EventEvent;
import at.control.shiftcontrol.shift.lib.event.events.parts.EventPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class EventEventTest {

    @Test
    void ofInternal() {
        Event event = mock(Event.class);
        String routingKey = "routingKey";

        EventPart eventPart = mock(EventPart.class);
        try (var eventPartMock = org.mockito.Mockito.mockStatic(EventPart.class)) {
            eventPartMock.when(() -> EventPart.of(event)).thenReturn(eventPart);

            EventEvent eventEvent = EventEvent.ofInternal(null, routingKey, event);

            assertEquals(eventPart, eventEvent.getEvent());
            assertEquals(routingKey, eventEvent.getRoutingKey());
        }
    }
}

