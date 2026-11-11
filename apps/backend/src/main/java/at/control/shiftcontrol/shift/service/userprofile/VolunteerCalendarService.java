package at.control.shiftcontrol.shift.service.userprofile;

import java.util.UUID;
import java.util.Optional;

import at.control.shiftcontrol.shift.dto.userprofile.CalendarShareIdDto;

public interface VolunteerCalendarService {
    Optional<String> getOrCreateCalendarShareId(UUID userId);

    CalendarShareIdDto regenerateCalendarShareId(UUID userId);

    String getCalendar(String shareId);
}
