package at.conrunner.shift.service.event.impl;

import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import at.conrunner.shift.entity.Activity;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.Location;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.type.PositionSignupState;
import at.conrunner.shift.lib.type.ShiftRelevance;
import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.dao.ActivityDao;
import at.conrunner.shift.dao.ShiftPlanDao;
import at.conrunner.shift.dao.EventDao;
import at.conrunner.shift.dao.ShiftDao;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.dto.event.schedule.ActivityScheduleDaySearchDto;
import at.conrunner.shift.dto.event.schedule.ActivityScheduleDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleContentDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleDaySearchDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleFilterDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleFilterValuesDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleLayoutDto;
import at.conrunner.shift.dto.event.schedule.ScheduleContentDto;
import at.conrunner.shift.dto.event.schedule.ScheduleContentNoLocationDto;
import at.conrunner.shift.dto.event.schedule.ScheduleLayoutDto;
import at.conrunner.shift.dto.event.schedule.ScheduleLayoutNoLocationDto;
import at.conrunner.shift.dto.shift.ShiftColumnDto;
import at.conrunner.shift.mapper.ActivityMapper;
import at.conrunner.shift.mapper.EventMapper;
import at.conrunner.shift.mapper.LocationMapper;
import at.conrunner.shift.mapper.RoleMapper;
import at.conrunner.shift.mapper.ShiftAssemblingMapper;
import at.conrunner.shift.service.EligibilityService;
import at.conrunner.shift.service.StatisticService;
import at.conrunner.shift.service.event.EventScheduleService;
import at.conrunner.shift.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventScheduleServiceImpl implements EventScheduleService {
    private record ShiftPlacement(Shift shift, int columnIndex) {}

    private final EventDao eventDao;
    private final ShiftPlanDao shiftPlanDao;
    private final ShiftDao shiftDao;
    private final ActivityDao activityDao;
    private final VolunteerDao volunteerDao;
    private final ShiftAssemblingMapper shiftMapper;
    private final SecurityHelper securityHelper;
    private final StatisticService statisticService;
    private final EligibilityService eligibilityService;
    private final CurrentShiftActor currentActor;

    @Override
    public EventScheduleLayoutDto getEventScheduleLayout(long eventId, EventScheduleFilterDto filterDto) {
        var event = eventDao.getById(eventId);
        var shiftsByLocation = getScheduleShiftsByLocation(event, filterDto);
        // Build location DTOs
        var scheduleLayoutDtos = shiftsByLocation.entrySet().stream()
            .filter(entry -> entry.getKey() != null)
            .map(entry -> buildScheduleLayoutDto(entry.getKey(), entry.getValue()))
            .toList();

        var userId = currentActor.get().requireUserId();
        var scheduleLayoutNoLocationDto = buildScheduleLayoutNoLocationDto(shiftsByLocation.get(null));

        var allShifts = new ArrayList<Shift>();
        shiftsByLocation.values().forEach(allShifts::addAll);

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
        return calculateShiftPlacements(shifts).stream()
            .mapToInt(ShiftPlacement::columnIndex)
            .max()
            .orElse(-1) + 1;
    }

    @Override
    public EventScheduleContentDto getEventScheduleContent(long eventId, EventScheduleDaySearchDto searchDto) {
        var event = eventDao.getById(eventId);
        var date = searchDto == null ? null : searchDto.getDate();
        // Assign columns across the filtered event so shifts spanning two days keep their column.
        var filters = searchDto == null ? null : new EventScheduleFilterDto(
            searchDto.getShiftPlanIds(), searchDto.getShiftName(), searchDto.getShiftRelevances(),
            searchDto.getRoleIds(), searchDto.getLocationIds());
        var shiftsByLocation = getScheduleShiftsByLocation(event, filters);
        var scheduleContentDtos = shiftsByLocation.entrySet().stream()
            .filter(entry -> entry.getKey() != null)
            .map(entry -> buildScheduleContentDto(entry.getKey(), entry.getValue(), date))
            .toList();

        var scheduleContentNoLocationDto = buildScheduleContentNoLocationDto(
            event.getId(), shiftsByLocation.get(null), date);

        return EventScheduleContentDto.builder()
            .date(date)
            .scheduleContentDtos(scheduleContentDtos)
            .scheduleContentNoLocationDto(scheduleContentNoLocationDto)
            .build();
    }

    private Map<Location, List<Shift>> getScheduleShiftsByLocation(Event event, EventScheduleFilterDto filterDto) {
        long eventId = event.getId();
        var userId = validateEventAccessAndGetUserId(event);
        var filteredShiftsWithoutViewMode = shiftDao.searchShiftsInEvent(eventId, userId, filterDto);
        var queriedShifts = getShiftsBasedOnViewModes(eventId, userId, filterDto, filteredShiftsWithoutViewMode);
        Map<Location, List<Shift>> shiftsByLocation = new HashMap<>();
        for (var shift : queriedShifts) {
            if (!securityHelper.isUserInPlan(shift)) {
                continue;
            }
            shiftsByLocation.computeIfAbsent(shift.getEffectiveLocation(), k -> new ArrayList<>()).add(shift);
        }
        // add all other locations of the event without shifts
        var locations = event.getLocations();
        for (var location : locations) {
            shiftsByLocation.putIfAbsent(location, new ArrayList<>());
        }
        shiftsByLocation.putIfAbsent(null, List.of());

        return shiftsByLocation;
    }

    private UUID validateEventAccessAndGetUserId(Event event) {
        securityHelper.assertUserIsAllowedToAccessEvent(event);
        return currentActor.get().requireUserId();
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

    private ScheduleContentDto buildScheduleContentDto(Location location, List<Shift> shifts, LocalDate date) {
        // get activities related to this location (shift unrelated, even when no shifts are present)
        var activitiesRelatedToLocation = activityDao.findAllByLocationId(location.getId()).stream()
            .distinct()
            .map(ActivityMapper::toActivityDto)
            .toList();
        return ScheduleContentDto.builder()
            .location(LocationMapper.toLocationDto(location))
            .activities(activitiesRelatedToLocation)
            .shiftColumns(toShiftColumns(shifts, date))
            .build();
    }

    private ScheduleContentNoLocationDto buildScheduleContentNoLocationDto(
        long eventId,
        List<Shift> shifts,
        LocalDate date) {

        // get activities without location
        var activitiesWithoutLocation = activityDao.findAllWithoutLocationByEventId(eventId).stream()
            .distinct()
            .map(ActivityMapper::toActivityDto)
            .toList();

        return ScheduleContentNoLocationDto.builder()
            .activities(activitiesWithoutLocation)
            .shiftColumns(toShiftColumns(shifts, date))
            .build();
    }

    private List<Shift> getShiftsBasedOnViewModes(
        long eventId,
        UUID userId,
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

    private List<ShiftColumnDto> toShiftColumns(List<Shift> shifts, LocalDate date) {
        var placements = calculateShiftPlacements(shifts).stream();
        if (date != null) {
            var start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            var end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            placements = placements.filter(placement -> placement.shift().getStartTime().isBefore(end)
                && placement.shift().getEndTime().isAfter(start));
        }
        return placements.map(placement -> ShiftColumnDto.builder()
            .columnIndex(placement.columnIndex())
            .shiftDto(shiftMapper.assemble(placement.shift()))
            .build()).toList();
    }

    private List<ShiftPlacement> calculateShiftPlacements(List<Shift> shifts) {
        var sortedShifts = shifts.stream().sorted(Comparator.comparing(Shift::getStartTime)
            .thenComparing(Shift::getEndTime)
            .thenComparingLong(Shift::getId)).toList();
        // end time per column
        var columnEndTimes = new ArrayList<Instant>();
        var result = new ArrayList<ShiftPlacement>(sortedShifts.size());
        for (var shift : sortedShifts) {
            int columnIndex = findFirstFreeColumnIndex(columnEndTimes, shift.getStartTime());
            if (columnIndex == -1) {
                columnIndex = columnEndTimes.size();
                columnEndTimes.add(shift.getEndTime());
            } else {
                columnEndTimes.set(columnIndex, shift.getEndTime());
            }
            result.add(new ShiftPlacement(shift, columnIndex));
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
        var shiftPlans = shiftPlanDao.findByEventId(eventId);
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
