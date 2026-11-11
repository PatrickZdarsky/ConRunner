package at.control.shiftcontrol.shift.service.event;

import at.control.shiftcontrol.shift.dto.event.EventExportDto;
import at.control.shiftcontrol.shift.dto.event.EventImportResultDto;
import org.springframework.web.multipart.MultipartFile;

public interface EventImportService {
    EventImportResultDto importEvent(MultipartFile file);

    EventExportDto getEventImportTemplate();
}
