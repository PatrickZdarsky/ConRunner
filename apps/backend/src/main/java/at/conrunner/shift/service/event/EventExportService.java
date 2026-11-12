package at.conrunner.shift.service.event;

import at.conrunner.shift.dto.event.EventExportDto;

public interface EventExportService {
    EventExportDto exportEvent(long eventId, String format);
}
