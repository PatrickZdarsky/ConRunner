package at.conrunner.shift.event.events;

import at.conrunner.shift.entity.Activity;
import at.conrunner.shift.lib.event.events.ActivityEvent;
import at.conrunner.shift.lib.event.events.parts.ActivityPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

class ActivityEventTest {

    @Test
    void ofInternal() {
        Activity activity = mock(Activity.class);
        String routingKey = "routingKey";

        ActivityPart activityPart = mock(ActivityPart.class);
        try (var activityPartMock = org.mockito.Mockito.mockStatic(ActivityPart.class)) {
            activityPartMock.when(() -> ActivityPart.of(activity)).thenReturn(activityPart);

            ActivityEvent activityEvent = ActivityEvent.ofInternal(null, routingKey, activity);

            assertEquals(activityPart, activityEvent.getActivity());
            assertEquals(routingKey, activityEvent.getRoutingKey());
        }
    }
}

