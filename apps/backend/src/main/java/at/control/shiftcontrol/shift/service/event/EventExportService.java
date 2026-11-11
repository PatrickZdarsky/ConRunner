package at.control.shiftcontrol.shift.service.event;

import at.control.shiftcontrol.shift.dto.event.EventExportDto;

public interface EventExportService {
    EventExportDto exportEvent(long eventId, String format);
}
