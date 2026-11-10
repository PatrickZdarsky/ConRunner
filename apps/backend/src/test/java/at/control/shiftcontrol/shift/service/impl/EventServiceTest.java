package at.control.shiftcontrol.shift.service.impl;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.platform.exception.NotFoundException;
import at.control.shiftcontrol.shift.auth.CurrentShiftActor;
import at.control.shiftcontrol.shift.auth.ShiftActor;
import at.control.shiftcontrol.shift.dao.EventDao;
import at.control.shiftcontrol.shift.dao.RewardPointsTransactionDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.OverallStatisticsDto;
import at.control.shiftcontrol.shift.dto.OwnStatisticsDto;
import at.control.shiftcontrol.shift.dto.event.EventSearchDto;
import at.control.shiftcontrol.shift.dto.rows.PlanVolunteerIdRow;
import at.control.shiftcontrol.shift.mapper.EventMapper;
import at.control.shiftcontrol.shift.mapper.ShiftPlanMapper;
import at.control.shiftcontrol.shift.service.StatisticService;
import at.control.shiftcontrol.shift.service.event.impl.EventServiceImpl;
import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserService;
import at.control.shiftcontrol.shift.util.SecurityHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    private static final java.util.UUID USER_ID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Mock
    private EventDao eventDao;

    @Mock
    private VolunteerDao volunteerDao;

    @Mock
    private StatisticService statisticService;

    @Mock
    private CurrentShiftActor userProvider;

    @Mock
    private RewardPointsTransactionDao rewardPointsTransactionDao;

    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private UserService userDirectoryService;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void search_filtersEventsByVolunteerShiftPlans() {
        // Arrange: create shift plans
        ShiftPlan spRelevant = new ShiftPlan();
        spRelevant.setId(1L);
        ShiftPlan spIrrelevant = new ShiftPlan();
        spIrrelevant.setId(2L);

        // Arrange: event that matches (contains spRelevant)
        Event relevantEvent = new Event();
        relevantEvent.setShiftPlans(List.of(spRelevant));
        relevantEvent.setStartTime(Instant.MIN);
        relevantEvent.setEndTime(Instant.MAX);

        // Arrange: event that does NOT match (contains spIrrelevant)
        Event nonRelevantEvent = new Event();
        nonRelevantEvent.setShiftPlans(List.of(spIrrelevant));

        var assignedUser = mock(ShiftActor.class);
        when(assignedUser.requireUserId()).thenReturn(USER_ID);
        when(userProvider.get()).thenReturn(assignedUser);

        // DAO returns both events
        when(eventDao.search(any(EventSearchDto.class)))
            .thenReturn(List.of(relevantEvent, nonRelevantEvent));

        // Volunteer participates in spRelevant only
        Volunteer volunteer = mock(Volunteer.class);
        when(volunteer.getVolunteeringPlans()).thenReturn(List.of(spRelevant));
        when(volunteerDao.getById(USER_ID)).thenReturn(volunteer);

        // Expected result is only the relevant event mapped
        var expected = EventMapper.toEventDto(List.of(relevantEvent));

        // Act
        var result = eventService.search(new EventSearchDto());

        // Assert
        verify(eventDao).search(any(EventSearchDto.class));
        verify(volunteerDao).getById(USER_ID);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getUserRelatedShiftPlansOfEvent_throwsWhenEventNotFound() {
        long eventId = 999L;
        java.util.UUID userId = USER_ID;

        when(volunteerDao.getById(userId)).thenReturn(new Volunteer());
        when(eventDao.getById(eventId)).thenThrow(NotFoundException.class);

        assertThatThrownBy(() -> eventService.getUserRelatedShiftPlansOfEvent(eventId, userId))
            .isInstanceOf(NotFoundException.class);

        verify(eventDao).getById(eventId);
        verifyNoInteractions(statisticService);
    }

    @Test
    void getUserRelatedShiftPlansOfEvent_throwsWhenVolunteerNotFound() {
        long eventId = 1L;
        java.util.UUID userId = java.util.UUID.randomUUID();

        Event event = new Event();
        event.setShiftPlans(List.of());

        when(volunteerDao.getById(userId)).thenThrow(NotFoundException.class);

        assertThatThrownBy(() -> eventService.getUserRelatedShiftPlansOfEvent(eventId, userId))
            .isInstanceOf(NotFoundException.class);

        verify(volunteerDao).getById(userId);
        verifyNoInteractions(statisticService, eventDao);
    }

    @Test
    void getEventShiftPlansOverview_buildsOverviewFromEventShiftPlansAndStatistics() {
        long eventId = 1L;
        java.util.UUID userId = USER_ID;

        // ShiftPlans (again: different IDs)
        ShiftPlan spRelevant = new ShiftPlan();
        spRelevant.setId(1L);
        ShiftPlan spIrrelevant = new ShiftPlan();
        spIrrelevant.setId(2L);

        Event event = new Event();
        event.setShiftPlans(List.of(spRelevant, spIrrelevant));
        event.setStartTime(Instant.MIN);
        event.setEndTime(Instant.MIN);

        Volunteer volunteer = mock(Volunteer.class);
        when(volunteer.getVolunteeringPlans()).thenReturn(List.of(spRelevant));

        when(eventDao.getById(eventId)).thenReturn(event);
        when(volunteerDao.getById(userId)).thenReturn(volunteer);

        // Stats (use your real DTO types here)
        OwnStatisticsDto ownStats = mock(OwnStatisticsDto.class);
        OverallStatisticsDto overallStats = mock(OverallStatisticsDto.class);

        when(statisticService.getOwnStatisticsOfShiftPlans(List.of(spRelevant), userId)).thenReturn(ownStats);
        when(statisticService.getOverallEventStatistics(event)).thenReturn(overallStats);
        when(rewardPointsTransactionDao.sumPointsByVolunteerAndEvent(userId.toString(), eventId)).thenReturn(10L);

        var result = eventService.getEventShiftPlansOverview(eventId, userId);

        // expected pieces
        var expectedEventOverview = EventMapper.toEventDto(event);
        var expectedShiftPlans = ShiftPlanMapper.toShiftPlanDto(List.of(spRelevant));

        assertThat(result.getEventOverview()).isEqualTo(expectedEventOverview);
        assertThat(result.getShiftPlans()).isEqualTo(expectedShiftPlans);
        assertThat(result.getRewardPoints()).isEqualTo(10L);
        assertThat(result.getOwnEventStatistics()).isEqualTo(ownStats);
        assertThat(result.getOverallEventStatistics()).isEqualTo(overallStats);

        verify(eventDao, times(2)).getById(eventId); // called twice since shiftPlans is fetched via call to getUserRelatedShiftPlansOfEvent
        verify(volunteerDao).getById(userId);
        verify(statisticService).getOwnStatisticsOfShiftPlans(List.of(spRelevant), userId);
        verify(statisticService).getOverallEventStatistics(event);
    }

    @Test
    void getEventShiftPlansOverview_throwsWhenEventNotFound() {
        long eventId = 999L;
        java.util.UUID userId = USER_ID;

        when(eventDao.getById(eventId)).thenThrow(NotFoundException.class);

        assertThatThrownBy(() -> eventService.getEventShiftPlansOverview(eventId, userId))
            .isInstanceOf(NotFoundException.class);

        verify(eventDao).getById(eventId);
        verifyNoInteractions(volunteerDao, statisticService);
    }

    @Test
    void getPlannerContactInfo_enrichesPlannerRowsFromUserDirectory() {
        long eventId = 7L;
        java.util.UUID userId = USER_ID;
        var planner1 = java.util.UUID.randomUUID();
        var planner2 = java.util.UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);
        event.setShiftPlans(List.of(new ShiftPlan()));

        when(eventDao.getById(eventId)).thenReturn(event);

        var rows = List.of(
            new PlanVolunteerIdRow(1L, "Main Desk", planner1),
            new PlanVolunteerIdRow(1L, "Main Desk", planner2),
            new PlanVolunteerIdRow(2L, "Backstage", planner2)
        );
        when(eventDao.getPlannersForEventAndUser(eventId, userId)).thenReturn(rows);
        when(userDirectoryService.getUsers(Set.of(planner1, planner2))).thenReturn(List.of(
            User.builder().id(planner1).username("planner1").email("alice@example.com").build(),
            User.builder().id(planner2).username("planner2").email("bob@example.com").build()
        ));

        var result = List.copyOf(eventService.getPlannerContactInfo(eventId, userId));

        assertThat(result).hasSize(2);
        assertThat(result).extracting("planId").containsExactly("1", "2");
        assertThat(result.get(0).getContacts()).hasSize(2);
        assertThat(result.get(0).getContacts())
            .extracting("userId", "email")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(planner1, "alice@example.com"),
                org.assertj.core.groups.Tuple.tuple(planner2, "bob@example.com")
            );
        assertThat(result.get(1).getContacts())
            .extracting("userId")
            .containsExactly(planner2);
    }
}
