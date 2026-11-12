package at.conrunner.shift.service.event;

import at.conrunner.shift.dto.event.EventDto;

public interface EventCloneService {
    public EventDto cloneEvent(long eventId);
}
