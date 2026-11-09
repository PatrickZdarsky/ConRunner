package at.control.shiftcontrol.shift.service.event;

import at.shiftcontrol.shiftservice.dto.event.EventDto;

public interface EventCloneService {
    public EventDto cloneEvent(long eventId);
}
