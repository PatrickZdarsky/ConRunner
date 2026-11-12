package at.conrunner.shift.service.event.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.auth.ShiftActor;
import at.conrunner.shift.dao.ActivityDao;
import at.conrunner.shift.dao.EventDao;
import at.conrunner.shift.dao.ShiftDao;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.dto.event.schedule.EventScheduleContentDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleDaySearchDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleFilterDto;
import at.conrunner.shift.dto.event.schedule.ScheduleLayoutDto;
import at.conrunner.shift.dto.shift.ShiftColumnDto;
import at.conrunner.shift.dto.shift.ShiftDto;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.Location;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.mapper.ShiftAssemblingMapper;
import at.conrunner.shift.service.StatisticService;
import at.conrunner.shift.util.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventScheduleServiceImplTest {
    private static final long EVENT_ID = 1L;
    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);
    private static final Instant MIDNIGHT = DAY.atStartOfDay(ZoneOffset.UTC).toInstant();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private EventDao eventDao;
    @Mock private ShiftDao shiftDao;
    @Mock private ActivityDao activityDao;
    @Mock private VolunteerDao volunteerDao;
    @Mock private ShiftAssemblingMapper shiftMapper;
    @Mock private SecurityHelper securityHelper;
    @Mock private StatisticService statisticService;
    @Mock private CurrentShiftActor currentActor;
    @Mock private ShiftActor actor;
    @InjectMocks private EventScheduleServiceImpl service;

    private final Location hall = Location.builder().id(2L).name("Main Hall").build();

    @BeforeEach
    void setUp() {
        when(eventDao.getById(EVENT_ID)).thenReturn(Event.builder().id(EVENT_ID).locations(List.of(hall)).build());
        when(currentActor.get()).thenReturn(actor);
        when(actor.requireUserId()).thenReturn(USER_ID);
        when(securityHelper.isUserInPlan(any(Shift.class))).thenReturn(true);
        when(shiftMapper.assemble(any(Shift.class))).thenAnswer(invocation -> {
            Shift shift = invocation.getArgument(0);
            return ShiftDto.builder().name(shift.getName()).build();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void packsBothLanesIndependentlyAndReusesTheFirstFreeColumn(int simultaneous) {
        var shifts = new ArrayList<Shift>();
        var expected = new HashMap<String, Integer>();
        for (int column = simultaneous - 1; column >= 0; column--) {
            var name = "Shift " + column;
            // Identical intervals in the hall and staggered intervals in the unlocated lane.
            shifts.add(shift(column + 1, name, 60, 120, hall));
            shifts.add(shift(column + 10, name, 60 + column * 5, 120 + column * 5, null));
            expected.put(name, column);
        }
        shifts.addFirst(shift(20, "Reuses column", 120, 125, hall));
        shifts.addFirst(shift(21, "Reuses column", 120, 125, null));
        expected.put("Reuses column", 0);
        stubShifts(shifts);

        var layout = service.getEventScheduleLayout(EVENT_ID, new EventScheduleFilterDto());
        assertThat(layout.getScheduleLayoutDtos())
            .extracting(ScheduleLayoutDto::getRequiredShiftColumns).containsExactly(simultaneous);
        assertThat(layout.getScheduleLayoutNoLocationDto().getRequiredShiftColumns()).isEqualTo(simultaneous);

        var day = service.getEventScheduleContent(EVENT_ID, new EventScheduleDaySearchDto(DAY));
        assertThat(columns(day, true)).containsExactlyInAnyOrderEntriesOf(expected);
        assertThat(columns(day, false)).containsExactlyInAnyOrderEntriesOf(expected);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void keepsOvernightShiftInTheSameColumnOnBothDays(boolean hasLocation) {
        var location = hasLocation ? hall : null;
        stubShifts(List.of(
            shift(3, "Next day", 1440, 1500, location),
            shift(2, "Overnight", 1410, 1560, location),
            shift(1, "Ends at midnight", 1380, 1440, location)));

        var firstDay = service.getEventScheduleContent(EVENT_ID, new EventScheduleDaySearchDto(DAY));
        var secondDay = service.getEventScheduleContent(EVENT_ID, new EventScheduleDaySearchDto(DAY.plusDays(1)));

        assertThat(columns(firstDay, hasLocation)).containsExactlyInAnyOrderEntriesOf(
            Map.of("Ends at midnight", 0, "Overnight", 1));
        assertThat(columns(secondDay, hasLocation)).containsExactlyInAnyOrderEntriesOf(
            Map.of("Next day", 0, "Overnight", 1));
    }

    private void stubShifts(List<Shift> shifts) {
        when(shiftDao.searchShiftsInEvent(eq(EVENT_ID), eq(USER_ID), any(EventScheduleFilterDto.class))).thenReturn(shifts);
    }

    private static Shift shift(long id, String name, int startMinute, int endMinute, Location location) {
        return Shift.builder().id(id).name(name).location(location)
            .startTime(MIDNIGHT.plusSeconds(startMinute * 60L))
            .endTime(MIDNIGHT.plusSeconds(endMinute * 60L))
            .build();
    }

    private static Map<String, Integer> columns(EventScheduleContentDto day, boolean hasLocation) {
        Collection<ShiftColumnDto> shifts = hasLocation
            ? day.getScheduleContentDtos().stream().findFirst().orElseThrow().getShiftColumns()
            : day.getScheduleContentNoLocationDto().getShiftColumns();
        return shifts.stream().collect(Collectors.toMap(column -> column.getShiftDto().getName(), ShiftColumnDto::getColumnIndex));
    }
}
