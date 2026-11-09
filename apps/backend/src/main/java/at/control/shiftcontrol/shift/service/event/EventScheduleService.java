package at.control.shiftcontrol.shift.service.event;

import at.shiftcontrol.shiftservice.dto.event.schedule.ActivityScheduleDaySearchDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.ActivityScheduleDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.EventScheduleContentDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.EventScheduleDaySearchDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.EventScheduleFilterDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.EventScheduleFilterValuesDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.EventScheduleLayoutDto;

public interface EventScheduleService {
    EventScheduleLayoutDto getEventScheduleLayout(long eventId, EventScheduleFilterDto filterDto);

    EventScheduleContentDto getEventScheduleContent(long eventId, EventScheduleDaySearchDto searchDto);

    EventScheduleFilterValuesDto getEventScheduleFilterValues(long eventId);

    ActivityScheduleDto getActivityScheduleOfEvent(long eventId, ActivityScheduleDaySearchDto searchDto);
}
