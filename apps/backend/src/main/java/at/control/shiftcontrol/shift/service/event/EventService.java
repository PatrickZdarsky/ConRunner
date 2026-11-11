package at.control.shiftcontrol.shift.service.event;

import java.util.UUID;
import java.util.Collection;
import java.util.List;

import at.control.shiftcontrol.shift.dto.event.EventDto;
import at.control.shiftcontrol.shift.dto.event.EventModificationDto;
import at.control.shiftcontrol.shift.dto.event.EventSearchDto;
import at.control.shiftcontrol.shift.dto.event.EventShiftPlansOverviewDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanContactInfoDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanDto;

public interface EventService {
    EventDto getEvent(long eventId);

    List<EventDto> search(EventSearchDto searchDto);

    Collection<EventDto> getAllOpenEvents(UUID currentUser);

    List<ShiftPlanDto> getUserRelatedShiftPlansOfEvent(long eventId, UUID userId);

    Collection<ShiftPlanContactInfoDto> getPlannerContactInfo(long eventId, UUID userId);

    EventShiftPlansOverviewDto getEventShiftPlansOverview(long eventId, UUID userId);

    EventDto createEvent(EventModificationDto modificationDto);

    EventDto updateEvent(long eventId, EventModificationDto eventModificationDto);

    void deleteEvent(long eventId);
}
