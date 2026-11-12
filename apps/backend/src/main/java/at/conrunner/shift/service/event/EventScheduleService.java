package at.conrunner.shift.service.event;

import at.conrunner.shift.dto.event.schedule.ActivityScheduleDaySearchDto;
import at.conrunner.shift.dto.event.schedule.ActivityScheduleDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleContentDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleDaySearchDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleFilterDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleFilterValuesDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleLayoutDto;

public interface EventScheduleService {
    EventScheduleLayoutDto getEventScheduleLayout(long eventId, EventScheduleFilterDto filterDto);

    EventScheduleContentDto getEventScheduleContent(long eventId, EventScheduleDaySearchDto searchDto);

    EventScheduleFilterValuesDto getEventScheduleFilterValues(long eventId);

    ActivityScheduleDto getActivityScheduleOfEvent(long eventId, ActivityScheduleDaySearchDto searchDto);
}
