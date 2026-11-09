package at.control.shiftcontrol.shift.service.event;

import at.shiftcontrol.shiftservice.dto.event.EventExportDto;

public interface EventExportService {
    EventExportDto exportEvent(long eventId, String format);
}
