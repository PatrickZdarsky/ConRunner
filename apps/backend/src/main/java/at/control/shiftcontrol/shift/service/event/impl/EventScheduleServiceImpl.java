package at.control.shiftcontrol.shift.service.event.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import at.shiftcontrol.lib.entity.Activity;
import at.shiftcontrol.lib.entity.Event;
import at.shiftcontrol.lib.entity.Location;
import at.shiftcontrol.lib.entity.PositionSlot;
import at.shiftcontrol.lib.entity.Shift;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.lib.type.PositionSignupState;
import at.shiftcontrol.lib.type.ShiftRelevance;
import at.shiftcontrol.shiftservice.auth.ApplicationUserProvider;
import at.shiftcontrol.shiftservice.dao.ActivityDao;
import at.shiftcontrol.shiftservice.dao.EventDao;
import at.shiftcontrol.shiftservice.dao.ShiftDao;
import at.shiftcontrol.shiftservice.dao.userprofile.VolunteerDao;
import at.shiftcontrol.shiftservice.dto.event.schedule.ActivityScheduleDaySearchDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.ActivityScheduleDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.EventScheduleContentDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.EventScheduleDaySearchDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.EventScheduleFilterDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.EventScheduleFilterValuesDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.EventScheduleLayoutDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.ScheduleContentDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.ScheduleContentNoLocationDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.ScheduleLayoutDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.ScheduleLayoutNoLocationDto;
import at.shiftcontrol.shiftservice.dto.shift.ShiftColumnDto;
import at.shiftcontrol.shiftservice.mapper.ActivityMapper;
import at.shiftcontrol.shiftservice.mapper.EventMapper;
import at.shiftcontrol.shiftservice.mapper.LocationMapper;
import at.shiftcontrol.shiftservice.mapper.RoleMapper;
import at.shiftcontrol.shiftservice.mapper.ShiftAssemblingMapper;
import at.shiftcontrol.shiftservice.service.EligibilityService;
import at.shiftcontrol.shiftservice.service.StatisticService;
import at.shiftcontrol.shiftservice.service.event.EventScheduleService;
import at.shiftcontrol.shiftservice.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventScheduleServiceImpl implements EventScheduleService {
    private final EventDao eventDao;
    private final ShiftDao shiftDao;
    private final ActivityDao activityDao;
    private final VolunteerDao volunteerDao;
    private final ShiftAssemblingMapper shiftMapper;
    private final SecurityHelper securityHelper;
    private final StatisticService statisticService;
    private final EligibilityService eligibilityService;
    private final ApplicationUserProvider userProvider;

    @Override
    public EventScheduleLayoutDto getEventScheduleLayout(long eventId, EventScheduleFilterDto filterDto) {
        var event = eventDao.getById(eventId);
        var shiftsByLocation = getScheduleShiftsByLocation(event, filterDto);
        // Build location DTOs
        var scheduleLayoutDtos = shiftsByLocation.entrySet().stream()
            .map(entry -> buildScheduleLayoutDto(entry.getKey(), entry.getValue()))
            .toList();

        var userId = userProvider.getCurrentUser().getUserId();
        var shiftsWithoutLocation = shiftDao.searchShiftsInEvent(eventId,
                userId,
                filterDto).stream()
            .filter(shift -> shift.getLocation() == null)
            .filter(securityHelper::isUserInPlan)
            .toList();
        var scheduleLayoutNoLocationDto = buildScheduleLayoutNoLocationDto(shiftsWithoutLocation);

        var allShifts = new ArrayList<Shift>();
        shiftsByLocation.values().forEach(allShifts::addAll);
        allShifts.addAll(shiftsWithoutLocation);

        var volunteer = volunteerDao.getById(userId);

        var stats = statisticService.getShiftPlanScheduleStatistics(allShifts, volunteer);
        return EventScheduleLayoutDto.builder()
            .scheduleLayoutDtos(scheduleLayoutDtos)
            .scheduleLayoutNoLocationDto(scheduleLayoutNoLocationDto)
            .scheduleStatistics(stats)
            .build();
    }

    private ScheduleLayoutDto buildScheduleLayoutDto(Location location, List<Shift> shifts) {
        // sort for deterministic column placement
        int requiredShiftColumns = getRequiredShiftColumns(shifts);
        return ScheduleLayoutDto.builder()
            .location(LocationMapper.toLocationDto(location))
            .requiredShiftColumns(requiredShiftColumns)
            .build();
    }

    private ScheduleLayoutNoLocationDto buildScheduleLayoutNoLocationDto(List<Shift> shifts) {
        int requiredShiftColumns = getRequiredShiftColumns(shifts);
        return ScheduleLayoutNoLocationDto.builder()
            .requiredShiftColumns(requiredShiftColumns)
            .build();
    }

    private int getRequiredShiftColumns(List<Shift> shifts) {
        // sort for deterministic column placement
        var sorted = shifts.stream()
            .sorted(Comparator
                .comparing(Shift::getStartTime)
                .thenComparing(Shift::getEndTime)).toList();
        var shiftColumns = calculateShiftColumns(sorted);
        return shiftColumns.stream()
            .mapToInt(ShiftColumnDto::getColumnIndex)
            .max()
            .orElse(-1) + 1;
    }

    @Override
    public EventScheduleContentDto getEventScheduleContent(long eventId, EventScheduleDaySearchDto searchDto) {
        var event = eventDao.getById(eventId);
        var shiftsByLocation = getScheduleShiftsByLocation(event, searchDto);
        // Build location DTOs
        var scheduleContentDtos = shiftsByLocation.entrySet().stream()
            .map(entry -> buildScheduleContentDto(entry.getKey(), entry.getValue()))
            .toList();

        var scheduleContentNoLocationDto = buildScheduleContentNoLocationDto(event.getId(), searchDto);

        return EventScheduleContentDto.builder()
            .date(searchDto != null ? searchDto.getDate() : null)
            .scheduleContentDtos(scheduleContentDtos)
            .scheduleContentNoLocationDto(scheduleContentNoLocationDto)
            .build();
    }

    private Map<Location, List<Shift>> getScheduleShiftsByLocation(Event event, EventScheduleFilterDto filterDto) {
        long eventId = event.getId();
        var userId = validateEventAccessAndGetUserId(event);
        // if param is ShiftPlanScheduleFilterDto filtering is done without date; date filtering is only done if param is ShiftPlanScheduleDaySearchDto instance
        var filteredShiftsWithoutViewMode = shiftDao.searchShiftsInEvent(eventId, userId, filterDto);
        var queriedShifts = getShiftsBasedOnViewModes(eventId, userId, filterDto, filteredShiftsWithoutViewMode);
        Map<Location, List<Shift>> shiftsByLocation = new HashMap<>();
        for (var shift : queriedShifts) {
            if (shift.getLocation() == null || !securityHelper.isUserInPlan(shift)) {
                continue;
            }
            shiftsByLocation.computeIfAbsent(shift.getLocation(), k -> new ArrayList<>()).add(shift);
        }
        // add all other locations of the event without shifts
        var locations = event.getLocations();
        for (var location : locations) {
            shiftsByLocation.putIfAbsent(location, new ArrayList<>());
        }

        return shiftsByLocation;
    }

    private String validateEventAccessAndGetUserId(Event event) {
        securityHelper.assertUserIsAllowedToAccessEvent(event);
        return userProvider.getCurrentUser().getUserId();
    }

    private boolean isSignupPossible(PositionSlot slot, Volunteer volunteer) {
        var state = eligibilityService.getSignupStateForPositionSlot(slot, volunteer);
        // no further actions needed if not eligible
        if (state == PositionSignupState.NOT_ELIGIBLE) {
            return false;
        }
        boolean freeAndOpenTrade = state == PositionSignupState.SIGNUP_OR_TRADE;
        boolean freeAndEligible = state == PositionSignupState.SIGNUP_POSSIBLE;
        boolean hasOpenTrade = state == PositionSignupState.SIGNUP_VIA_TRADE;
        boolean hasAuction = state == PositionSignupState.SIGNUP_VIA_AUCTION;
        return freeAndEligible || freeAndOpenTrade || hasOpenTrade || hasAuction;
    }

    private ScheduleContentDto buildScheduleContentDto(Location location, List<Shift> shifts) {
        // sort for deterministic column placement
        shifts.sort(Comparator
            .comparing(Shift::getStartTime)
            .thenComparing(Shift::getEndTime));
        var shiftColumns = calculateShiftColumns(shifts);
        // get activities related to this location (shift unrelated, even when no shifts are present)
        var activitiesRelatedToLocation = activityDao.findAllByLocationId(location.getId()).stream()
            .distinct()
            .map(ActivityMapper::toActivityDto)
            .toList();
        return ScheduleContentDto.builder()
            .location(LocationMapper.toLocationDto(location))
            .activities(activitiesRelatedToLocation)
            .shiftColumns(shiftColumns)
            .build();
    }

    private ScheduleContentNoLocationDto buildScheduleContentNoLocationDto(
        long eventId,
        EventScheduleDaySearchDto searchDto) {

        // get activities without location
        var activitiesWithoutLocation = activityDao.findAllWithoutLocationByEventId(eventId).stream()
            .distinct()
            .map(ActivityMapper::toActivityDto)
            .toList();

        // get shifts without location
        var shiftsWithoutLocation = shiftDao.searchShiftsInEvent(eventId,
                userProvider.getCurrentUser().getUserId(),
                searchDto).stream()
            .filter(shift -> shift.getLocation() == null)
            .filter(securityHelper::isUserInPlan)
            .toList();

        var filteredShiftsWithoutLocation = getShiftsBasedOnViewModes(eventId,
            userProvider.getCurrentUser().getUserId(),
            searchDto,
            shiftsWithoutLocation);

        var shiftColumns = calculateShiftColumns(filteredShiftsWithoutLocation);

        return ScheduleContentNoLocationDto.builder()
            .activities(activitiesWithoutLocation)
            .shiftColumns(shiftColumns)
            .build();
    }

    private List<Shift> getShiftsBasedOnViewModes(
        long eventId,
        String userId,
        EventScheduleFilterDto filterDto,
        List<Shift> filteredShiftsWithoutViewMode) {
        if (filterDto != null
            && filterDto.getShiftRelevances() != null
            && !filterDto.getShiftRelevances().isEmpty()) {
            List<Shift> ownShifts = new ArrayList<>();
            List<Shift> signUpPossibleShifts = new ArrayList<>();
            if (filterDto.getShiftRelevances().contains(ShiftRelevance.MY_SHIFTS)) {
                ownShifts = shiftDao.searchUserRelatedShiftsInEvent(eventId, userId);
            }
            if (filterDto.getShiftRelevances().contains(ShiftRelevance.SIGNUP_POSSIBLE)) {
                signUpPossibleShifts = new ArrayList<>(filteredShiftsWithoutViewMode);
                var volunteer = volunteerDao.getById(userId);
                signUpPossibleShifts = signUpPossibleShifts.stream()
                    .filter(shift -> shift.getSlots().stream().anyMatch(slot ->
                        isSignupPossible(slot, volunteer)
                    ))
                    .toList();
            }
            // combine results
            var combinedShifts = new ArrayList<>(ownShifts);
            for (var shift : signUpPossibleShifts) {
                if (!combinedShifts.contains(shift)) {
                    combinedShifts.add(shift);
                }
            }
            // combine combined shifts with filteredShiftsWithoutViewMode to apply other filters
            return combinedShifts.stream()
                .filter(filteredShiftsWithoutViewMode::contains)
                .toList();
        }
        return filteredShiftsWithoutViewMode;
    }

    private List<ShiftColumnDto> calculateShiftColumns(List<Shift> sortedShifts) {
        // end time per column
        var columnEndTimes = new ArrayList<Instant>();
        var result = new ArrayList<ShiftColumnDto>(sortedShifts.size());
        for (var shift : sortedShifts) {
            int columnIndex = findFirstFreeColumnIndex(columnEndTimes, shift.getStartTime());
            if (columnIndex == -1) {
                columnIndex = columnEndTimes.size();
                columnEndTimes.add(shift.getEndTime());
            } else {
                columnEndTimes.set(columnIndex, shift.getEndTime());
            }
            result.add(ShiftColumnDto.builder()
                .columnIndex(columnIndex)
                .shiftDto(shiftMapper.assemble(shift))
                .build());
        }
        return result;
    }

    private int findFirstFreeColumnIndex(List<Instant> columnEndTimes, Instant startTime) {
        for (int i = 0; i < columnEndTimes.size(); i++) {
            // column is free if previous shift ended at or before this start
            if (!columnEndTimes.get(i).isAfter(startTime)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public EventScheduleFilterValuesDto getEventScheduleFilterValues(long eventId) {
        var event = eventDao.getById(eventId);
        var shiftPlans = event.getShiftPlans();
        if (shiftPlans == null || shiftPlans.isEmpty()) {
            return EventScheduleFilterValuesDto.builder()
                .locations(List.of())
                .roles(List.of())
                .firstDate(event.getStartTime())
                .lastDate(event.getEndTime())
                .build();
        }

        var allShifts = shiftPlans.stream()
            .flatMap(shiftPlan -> shiftPlan.getShifts().stream())
            .filter(Objects::nonNull)
            .toList();

        var incompleteDto = getDatesFromAllShifts(allShifts, event);

        var locations = event.getLocations().stream()
            .map(LocationMapper::toLocationDto)
            .toList();

        var roles = shiftPlans.stream()
            .flatMap(shiftPlan -> shiftPlan.getRoles().stream())
            .distinct()
            .map(RoleMapper::toRoleDto)
            .toList();

        return EventScheduleFilterValuesDto.builder()
            .locations(locations)
            .roles(roles)
            .firstDate(incompleteDto.getFirstDate())
            .lastDate(incompleteDto.getLastDate())
            .build();
    }

    private EventScheduleFilterValuesDto getDatesFromAllShifts(Collection<Shift> shifts, Event event) {
        if (shifts.isEmpty() || securityHelper.isUserAdmin() || securityHelper.isUserPlannerInAnyPlanOfEvent(event)) {
            return EventScheduleFilterValuesDto.builder()
                .locations(List.of())
                .roles(List.of())
                .firstDate(event.getStartTime())
                .lastDate(event.getEndTime())
                .build();
        }
        // Determine first and last date from shifts and related activities
        var firstDate = Stream.concat(
                shifts.stream().map(Shift::getStartTime),
                shifts.stream()
                    .map(Shift::getRelatedActivity)
                    .filter(Objects::nonNull)
                    .map(Activity::getStartTime)
            )
            .filter(Objects::nonNull)
            .min(Instant::compareTo)
            .orElse(event.getStartTime());
        var lastDate = Stream.concat(
                shifts.stream().map(Shift::getEndTime),
                shifts.stream()
                    .map(Shift::getRelatedActivity)
                    .filter(Objects::nonNull)
                    .map(Activity::getEndTime)
            )
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(event.getEndTime());

        return EventScheduleFilterValuesDto.builder()
            .locations(List.of())
            .roles(List.of())
            .firstDate(firstDate)
            .lastDate(lastDate)
            .build();
    }

    @Override
    public ActivityScheduleDto getActivityScheduleOfEvent(long eventId, ActivityScheduleDaySearchDto searchDto) {
        var event = eventDao.getById(eventId);
        securityHelper.assertUserIsAllowedToAccessEvent(event);

        var activitiesOfEvent = activityDao.searchActivitiesInEvent(eventId, searchDto).stream().toList();

        return EventMapper.toActivityScheduleDto(event, activitiesOfEvent);
    }
}
