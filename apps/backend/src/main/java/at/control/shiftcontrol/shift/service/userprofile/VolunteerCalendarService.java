package at.control.shiftcontrol.shift.service.userprofile;

import java.util.Optional;

import at.shiftcontrol.shiftservice.dto.userprofile.CalendarShareIdDto;

public interface VolunteerCalendarService {
    Optional<String> getOrCreateCalendarShareId(String userId);

    CalendarShareIdDto regenerateCalendarShareId(String userId);

    String getCalendar(String shareId);
}
