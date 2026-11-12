package at.conrunner.shift.integration;

import static org.assertj.core.api.Assertions.assertThat;

import at.conrunner.shift.dao.impl.specification.ShiftSpecifications;
import at.conrunner.shift.dto.event.EventDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleFilterDto;
import at.conrunner.shift.entity.Activity;
import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.Location;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.RewardPointsTransaction;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.TimeConstraint;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.type.AssignmentStatus;
import at.conrunner.shift.lib.type.LockStatus;
import at.conrunner.shift.lib.type.RewardPointTransactionType;
import at.conrunner.shift.lib.type.TimeConstraintType;
import at.conrunner.shift.integration.config.RestITBase;
import at.conrunner.shift.repo.ActivityRepository;
import at.conrunner.shift.repo.AssignmentRepository;
import at.conrunner.shift.repo.EventRepository;
import at.conrunner.shift.repo.LocationRepository;
import at.conrunner.shift.repo.PositionSlotRepository;
import at.conrunner.shift.repo.RewardPointTransactionRepository;
import at.conrunner.shift.repo.ShiftPlanRepository;
import at.conrunner.shift.repo.ShiftRepository;
import at.conrunner.shift.repo.TimeConstraintRepository;
import at.conrunner.shift.repo.VolunteerRepository;
import io.restassured.http.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;

class EventIT extends RestITBase {
    private static final String EVENT_CLONE_PATH = "/events/%d/clone";

    @Autowired
    PositionSlotRepository positionSlotRepository;

    @Autowired
    ShiftRepository shiftRepository;

    @Autowired
    ShiftPlanRepository shiftPlanRepository;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    VolunteerRepository volunteerRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Autowired
    TimeConstraintRepository timeConstraintRepository;

    @Autowired
    ActivityRepository activityRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    RewardPointTransactionRepository rewardPointTransactionRepository;

    @Autowired
    JdbcTemplate jdbc;

    private Event eventA;
    private ShiftPlan shiftPlanA;
    private Shift shiftA, shiftB;

    private Volunteer volunteerA, volunteerB;

    private PositionSlot positionSlotA, positionSlotB;

    private Assignment assignmentA;

    @BeforeEach
    void setUp() {





        createEvents();
        createShiftPlans();
        createShifts();
        createPositionSlots();
        createVolunteers();
        createAssignments();
    }

    private void createEvents() {
        var startTimeA = LocalDateTime.of(2024, 2, 1, 7, 0).toInstant(ZoneOffset.UTC);
        var endTimeA = LocalDateTime.of(2024, 2, 3, 22, 0).toInstant(ZoneOffset.UTC);

        eventA = Event.builder()
            .name("EventA")
            .startTime(startTimeA)
            .endTime(endTimeA)
            .build();

        eventRepository.save(eventA);

        assertAll(
            () -> assertThat(eventA.getId()).isGreaterThan(0),
            () -> assertThat(eventRepository.existsById(eventA.getId())).isTrue()
        );
    }

    private void createShiftPlans() {
        shiftPlanA = ShiftPlan.builder()
            .name("ShiftPlanA")
            .event(eventA)
            .lockStatus(LockStatus.SUPERVISED)
            .defaultNoRolePointsPerMinute(1)
            .build();

        shiftPlanRepository.save(shiftPlanA);

        assertAll(
            () -> assertThat(shiftPlanA.getId()).isGreaterThan(0),
            () -> assertThat(shiftPlanRepository.existsById(shiftPlanA.getId())).isTrue()
        );
    }

    private void createShifts() {
        var shifts = new ArrayList<Shift>();

        var startTimeA = LocalDateTime.of(2024, 1, 1, 10, 0).toInstant(ZoneOffset.UTC);
        var endTimeA = LocalDateTime.of(2024, 1, 1, 20, 0).toInstant(ZoneOffset.UTC);

        shiftA = Shift.builder()
            .name("ShiftA")
            .shiftPlan(shiftPlanA)
            .shortDescription("ShortDescA")
            .longDescription("This is a description for ShiftA")
            .startTime(startTimeA)
            .endTime(endTimeA)
            .bonusRewardPoints(0)
            .build();
        shifts.add(shiftA);

        var startTimeB = LocalDateTime.of(2024, 2, 1, 11, 0).toInstant(ZoneOffset.UTC);
        var endTimeB = LocalDateTime.of(2024, 2, 3, 18, 0).toInstant(ZoneOffset.UTC);

        shiftB = Shift.builder()
            .name("ShiftB")
            .shiftPlan(shiftPlanA)
            .shortDescription("ShortDescB")
            .longDescription("This is a description for ShiftB")
            .startTime(startTimeB)
            .endTime(endTimeB)
            .bonusRewardPoints(10)
            .build();
        shifts.add(shiftB);

        shiftRepository.saveAll(shifts);

        assertAll(
            () -> assertThat(shiftA.getId()).isGreaterThan(0),
            () -> assertThat(shiftB.getId()).isGreaterThan(0),
            () -> assertThat(shiftRepository.existsById(shiftA.getId())).isTrue(),
            () -> assertThat(shiftRepository.existsById(shiftB.getId())).isTrue()
        );
    }

    private void createPositionSlots() {
        var slots = new ArrayList<PositionSlot>();

        positionSlotA = PositionSlot.builder()
            .name("PositionSlotA")
            .skipAutoAssignment(false)
            .shift(shiftA)
            .description("DescriptionA")
            .desiredVolunteerCount(3)
            .build();
        slots.add(positionSlotA);

        positionSlotB = PositionSlot.builder()
            .name("PositionSlotB")
            .skipAutoAssignment(true)
            .shift(shiftB)
            .description("DescriptionB")
            .desiredVolunteerCount(5)
            .overrideRewardPoints(70)
            .build();
        slots.add(positionSlotB);

        positionSlotRepository.saveAll(slots);

        assertAll(
            () -> assertThat(positionSlotA.getId()).isGreaterThan(0),
            () -> assertThat(positionSlotB.getId()).isGreaterThan(0),
            () -> assertThat(positionSlotRepository.existsById(positionSlotA.getId())).isTrue(),
            () -> assertThat(positionSlotRepository.existsById(positionSlotB.getId())).isTrue()
        );
    }

    private void createVolunteers() {
        var volunteers = new ArrayList<Volunteer>();
        volunteerA = Volunteer.builder()
            .id(java.util.UUID.fromString("00000000-0000-0000-0000-000000011111"))
            .volunteeringPlans(Set.of(shiftPlanA))
            .build();
        volunteers.add(volunteerA);

        volunteerB = Volunteer.builder()
            .id(java.util.UUID.fromString("00000000-0000-0000-0000-000000022222"))
            .volunteeringPlans(Set.of(shiftPlanA))
            .build();
        volunteers.add(volunteerB);

        volunteerRepository.saveAll(volunteers);
        assertAll(
            () -> assertThat(volunteerA.getId()).isNotNull(),
            () -> assertThat(volunteerB.getId()).isNotNull(),
            () -> assertThat(volunteerA.getVolunteeringPlans()).contains(shiftPlanA),
            () -> assertThat(volunteerB.getVolunteeringPlans()).contains(shiftPlanA)
        );
        assertAll(
            () -> assertThat(volunteerRepository.existsById(volunteerA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.existsById(volunteerB.getId())).isTrue()
        );
    }

    private void createAssignments() {
        assignmentA = Assignment.builder()
            .positionSlot(positionSlotA)
            .assignedVolunteer(volunteerA)
            .status(AssignmentStatus.ACCEPTED)
            .acceptedRewardPoints(40)
            .build();

        assignmentRepository.save(assignmentA);

        assertAll(
            () -> assertThat(assignmentA.getId()).isNotZero(),
            () -> assertThat(assignmentA.getPositionSlot()).isEqualTo(positionSlotA),
            () -> assertThat(assignmentA.getAssignedVolunteer()).isEqualTo(volunteerA),
            () -> assertThat(assignmentA.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED)
        );
    }

    @Test
    void cloneEventAsNonAdminReturnsForbidden() {
        doRequestAsAssignedAndAssertMessage(
            Method.POST,
            EVENT_CLONE_PATH.formatted(eventA.getId()),
            "",
            HttpStatus.FORBIDDEN.value(),
            "Access Denied",
            true,
            volunteerA.getId()
        );
    }

    @Test
    void cloneEventByNonExistingIdReturnsNotFound() {
        doRequestAsAdminAndAssertMessage(
            Method.POST,
            EVENT_CLONE_PATH.formatted(9999),
            "",
            404,
            "Event not found.",
            true
        );
    }

    @Test
    void cloningAnEventPersistsItsNewPlansAndShifts() {
        EventDto clone = doRequest(Method.POST, EVENT_CLONE_PATH.formatted(eventA.getId()), "", Map.of(),
            asAdminHeaders(), HttpStatus.OK.value(), EventDto.class);
        long cloneId = Long.parseLong(clone.getId());

        assertThat(shiftPlanRepository.findByEventId(cloneId)).singleElement().satisfies(plan -> {
            assertThat(plan.getName()).isEqualTo(shiftPlanA.getName());
            assertThat(plan.getId()).isNotEqualTo(shiftPlanA.getId());
        });
        assertThat(shiftRepository.findAll(ShiftSpecifications.inEvent(cloneId)))
            .extracting(Shift::getName).containsExactlyInAnyOrder(shiftA.getName(), shiftB.getName());
    }

    @Test
    void deletingEventRemovesLiveStaffingAndPreservesHistory() {
        var constraint = timeConstraintRepository.save(TimeConstraint.builder()
            .volunteer(volunteerA)
            .event(eventA)
            .type(TimeConstraintType.UNAVAILABLE)
            .startTime(eventA.getStartTime())
            .endTime(eventA.getEndTime())
            .build());
        var reward = rewardPointTransactionRepository.save(RewardPointsTransaction.builder()
            .eventId(eventA.getId())
            .shiftPlanId(shiftPlanA.getId())
            .positionSlotId(positionSlotA.getId())
            .volunteerId(volunteerA.getId().toString())
            .points(assignmentA.getAcceptedRewardPoints())
            .type(RewardPointTransactionType.EARN)
            .sourceKey("event-delete-test")
            .createdAt(Instant.now())
            .build());

        doRequest(Method.DELETE, "/events/" + eventA.getId(), "", Map.of(), asAdminHeaders(), HttpStatus.OK.value(), null);

        assertThat(eventRepository.existsById(eventA.getId())).isFalse();
        assertThat(shiftPlanRepository.existsById(shiftPlanA.getId())).isFalse();
        assertThat(shiftRepository.existsById(shiftA.getId())).isFalse();
        assertThat(positionSlotRepository.existsById(positionSlotA.getId())).isFalse();
        assertThat(assignmentRepository.existsById(assignmentA.getId())).isFalse();
        assertThat(timeConstraintRepository.existsById(constraint.getId())).isFalse();
        assertThat(volunteerRepository.existsById(volunteerA.getId())).isTrue();
        assertThat(rewardPointTransactionRepository.existsById(reward.getId())).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_entry WHERE action = ? AND subject_ref = ?", Long.class,
            "assignment.removed", String.valueOf(assignmentA.getId()))).isEqualTo(1L);
    }

    @Test
    void scheduleLocationFilterFollowsTheActivityWhenItMoves() {
        var oldLocation = locationRepository.save(Location.builder().event(eventA).name("Hall A").build());
        var newLocation = locationRepository.save(Location.builder().event(eventA).name("Hall B").build());
        var activity = activityRepository.save(Activity.builder()
            .event(eventA)
            .name("Opening")
            .startTime(shiftB.getStartTime())
            .endTime(shiftB.getEndTime())
            .location(oldLocation)
            .build());
        shiftA.setLocation(oldLocation);
        shiftB.setRelatedActivity(activity);
        shiftRepository.saveAll(List.of(shiftA, shiftB));

        assertThat(shiftsAt(oldLocation)).containsExactlyInAnyOrder(shiftA.getId(), shiftB.getId());
        activity.setLocation(newLocation);
        activityRepository.save(activity);

        assertThat(shiftsAt(oldLocation)).containsExactly(shiftA.getId());
        assertThat(shiftsAt(newLocation)).containsExactly(shiftB.getId());
    }

    private List<Long> shiftsAt(Location location) {
        var filter = new EventScheduleFilterDto();
        filter.setLocationIds(List.of(String.valueOf(location.getId())));
        return shiftRepository.findAll(ShiftSpecifications.matchesSearchDto(filter)).stream().map(Shift::getId).toList();
    }
}
