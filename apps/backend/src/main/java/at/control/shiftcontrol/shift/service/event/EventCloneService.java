package at.control.shiftcontrol.shift.service.event;

import at.control.shiftcontrol.shift.dto.event.EventDto;

public interface EventCloneService {
    public EventDto cloneEvent(long eventId);
}
