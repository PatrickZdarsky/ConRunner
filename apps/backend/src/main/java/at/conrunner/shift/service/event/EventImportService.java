package at.conrunner.shift.service.event;

import at.conrunner.shift.dto.event.EventExportDto;
import at.conrunner.shift.dto.event.EventImportResultDto;
import org.springframework.web.multipart.MultipartFile;

public interface EventImportService {
    EventImportResultDto importEvent(MultipartFile file);

    EventExportDto getEventImportTemplate();
}
