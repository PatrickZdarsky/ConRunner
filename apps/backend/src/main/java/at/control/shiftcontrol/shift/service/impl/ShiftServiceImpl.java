package at.control.shiftcontrol.shift.service.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import io.micrometer.common.util.StringUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.dto.PaginationDto;
import at.shiftcontrol.lib.entity.Shift;
import at.shiftcontrol.lib.event.events.ShiftEvent;
import at.shiftcontrol.lib.exception.BadRequestException;
import at.shiftcontrol.lib.type.AssignmentStatus;
import at.shiftcontrol.lib.util.ConvertUtil;
import at.shiftcontrol.shiftservice.annotation.AdminOnly;
import at.shiftcontrol.shiftservice.dao.ActivityDao;
import at.shiftcontrol.shiftservice.dao.LocationDao;
import at.shiftcontrol.shiftservice.dao.ShiftDao;
import at.shiftcontrol.shiftservice.dao.ShiftPlanDao;
import at.shiftcontrol.shiftservice.dto.shift.ShiftDetailsDto;
import at.shiftcontrol.shiftservice.dto.shift.ShiftDto;
import at.shiftcontrol.shiftservice.dto.shift.ShiftModificationDto;
import at.shiftcontrol.shiftservice.mapper.EventMapper;
import at.shiftcontrol.shiftservice.mapper.PaginationMapper;
import at.shiftcontrol.shiftservice.mapper.ShiftAssemblingMapper;
import at.shiftcontrol.shiftservice.mapper.ShiftPlanMapper;
import at.shiftcontrol.shiftservice.service.ShiftService;
import at.shiftcontrol.shiftservice.service.UserPreferenceService;
import at.shiftcontrol.shiftservice.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {
    private final ShiftDao shiftDao;
    private final ShiftPlanDao shiftPlanDao;
    private final ActivityDao activityDao;
    private final LocationDao locationDao;
    private final UserPreferenceService userPreferenceService;
    private final ShiftAssemblingMapper shiftAssemblingMapper;
    private final SecurityHelper securityHelper;
    private final ApplicationEventPublisher publisher;

    @Override
    public @org.jspecify.annotations.NonNull ShiftDetailsDto getShiftDetails(long shiftId, @org.jspecify.annotations.NonNull String userId) {
        var shift = shiftDao.getById(shiftId);
        var plan = shift.getShiftPlan();
        var event = plan.getEvent();

        var userPref = userPreferenceService.getUserPreference(userId, shiftId);

        return ShiftDetailsDto.builder()
            .shift(shiftAssemblingMapper.assemble(shift))
            .event(EventMapper.toEventDto(event))
            .shiftPlan(ShiftPlanMapper.toShiftPlanDto(plan))
            .preference(userPref)
            .build();
    }

    @Override
    public @org.jspecify.annotations.NonNull PaginationDto<ShiftDto> getAllOpenShiftsOfPlanPaginated(long shiftPlanId, int page, int size) {
        securityHelper.assertUserIsPlanner(shiftPlanId);

        var shifts = shiftDao.findAllInShiftPlan(shiftPlanId)
            .stream()
            .filter(shift -> shift.getSlots() != null && shift.getSlots()
                .stream()
                .anyMatch(slot ->slot.getAssignments() != null && slot.getAssignments()
                    .stream()
                    .filter(ass -> ass.getStatus() != AssignmentStatus.REQUEST_FOR_ASSIGNMENT)
                    .count() != slot.getDesiredVolunteerCount()
                )
            )
            .sorted(Comparator.comparingLong(Shift::getId))
            .toList();

        var shiftsPage = shifts
            .stream()
            .skip((long) page * size)
            .limit(size)
            .map(shiftAssemblingMapper::assemble)
            .toList();


        var sizeTotal = shifts.size();

        return PaginationMapper.toPaginationDto(size, page, sizeTotal,shiftsPage);
    }

    @Override
    public @org.jspecify.annotations.NonNull ShiftDto createShift(long shiftPlanId, @NonNull ShiftModificationDto modificationDto) {
        securityHelper.assertUserIsPlanner(shiftPlanId);

        var shiftPlan = shiftPlanDao.getById(shiftPlanId);
        var newShift = Shift.builder()
            .shiftPlan(shiftPlan)
            .build();

        validateModificationDtoAndSetShiftFields(modificationDto, newShift);
        newShift = shiftDao.save(newShift);

        publisher.publishEvent(ShiftEvent.shiftCreated(newShift));
        return shiftAssemblingMapper.assemble(newShift);
    }

    @Override
    public @org.jspecify.annotations.NonNull ShiftDto updateShift(long shiftId, @NonNull ShiftModificationDto modificationDto) {
        var shift = shiftDao.getById(shiftId);
        securityHelper.assertUserIsPlanner(shift);

        validateModificationDtoAndSetShiftFields(modificationDto, shift);

        shift = shiftDao.save(shift);

        publisher.publishEvent(ShiftEvent.shiftUpdated(shift));
        return shiftAssemblingMapper.assemble(shift);
    }

    private void validateModificationDtoAndSetShiftFields(ShiftModificationDto modificationDto, Shift shift) {
        if (StringUtils.isNotBlank(modificationDto.getActivityId()) && StringUtils.isNotBlank(modificationDto.getLocationId())) {
            throw new BadRequestException("Cannot set both related activity and location");
        }

        var event = shift.getShiftPlan().getEvent();

        if (modificationDto.getStartTime().isAfter(modificationDto.getEndTime())) {
            throw new BadRequestException("Shift start time must be before end time");
        }

        if (modificationDto.getStartTime().isBefore(event.getStartTime())
            || modificationDto.getEndTime().isAfter(event.getEndTime())
            || modificationDto.getEndTime().isBefore(modificationDto.getStartTime())) {
            throw new BadRequestException("Shift time range must be within event time range");
        }

        shift.setName(modificationDto.getName());
        shift.setShortDescription(modificationDto.getShortDescription());
        shift.setLongDescription(modificationDto.getLongDescription());
        shift.setStartTime(modificationDto.getStartTime());
        shift.setEndTime(modificationDto.getEndTime());
        shift.setBonusRewardPoints(modificationDto.getBonusRewardPoints());
        if (StringUtils.isNotBlank(modificationDto.getActivityId())) {
            var activity = activityDao.getById(ConvertUtil.idToLong(modificationDto.getActivityId()));
            shift.setRelatedActivity(activity);

            if (activity.getLocation() != null) {
                var eventLocations = shift.getShiftPlan().getEvent().getLocations();
                if (!eventLocations.contains(activity.getLocation())) {
                    throw new BadRequestException("Activity location does not belong to the same event as the shift");
                }
                shift.setLocation(activity.getLocation());
            }
        } else if (StringUtils.isNotBlank(modificationDto.getLocationId())) {
            var location = locationDao.getById(ConvertUtil.idToLong(modificationDto.getLocationId()));

            var eventLocations = shift.getShiftPlan().getEvent().getLocations();
            if (!eventLocations.contains(location)) {
                throw new BadRequestException("Location does not belong to the same event as the shift");
            }
            shift.setLocation(location);
            shift.setRelatedActivity(null);
        }
    }

    @Override
    public void deleteShift(long shiftId) {
        var shift = shiftDao.getById(shiftId);
        securityHelper.assertUserIsPlanner(shift);

        var shiftEvent = ShiftEvent.shiftDeleted(shift);
        shiftDao.delete(shift);
        publisher.publishEvent(shiftEvent);
    }

    @AdminOnly
    @Override
    public List<Long> getShiftIds(long shiftPlanId) {
        return shiftDao.findAllShiftIdsInShiftPlan(shiftPlanId);
    }
}
