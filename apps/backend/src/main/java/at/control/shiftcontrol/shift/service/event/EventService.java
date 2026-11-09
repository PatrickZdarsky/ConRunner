package at.control.shiftcontrol.shift.service.event;

import java.util.Collection;
import java.util.List;

import at.shiftcontrol.shiftservice.dto.event.EventDto;
import at.shiftcontrol.shiftservice.dto.event.EventModificationDto;
import at.shiftcontrol.shiftservice.dto.event.EventSearchDto;
import at.shiftcontrol.shiftservice.dto.event.EventShiftPlansOverviewDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanContactInfoDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanDto;

public interface EventService {
    EventDto getEvent(long eventId);

    List<EventDto> search(EventSearchDto searchDto);

    Collection<EventDto> getAllOpenEvents(String currentUser);

    List<ShiftPlanDto> getUserRelatedShiftPlansOfEvent(long eventId, String userId);

    Collection<ShiftPlanContactInfoDto> getPlannerContactInfo(long eventId, String userId);

    EventShiftPlansOverviewDto getEventShiftPlansOverview(long eventId, String userId);

    EventDto createEvent(EventModificationDto modificationDto);

    EventDto updateEvent(long eventId, EventModificationDto eventModificationDto);

    void deleteEvent(long eventId);
}
