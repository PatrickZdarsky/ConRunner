package at.control.shiftcontrol.shift.integration;

import static org.assertj.core.api.Assertions.assertThat;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.lib.type.LockStatus;
import at.control.shiftcontrol.shift.dto.activity.ActivityDto;
import at.control.shiftcontrol.shift.dto.activity.ActivityModificationDto;
import at.control.shiftcontrol.shift.integration.config.RestITBase;
import at.control.shiftcontrol.shift.repo.AssignmentRepository;
import at.control.shiftcontrol.shift.repo.EventRepository;
import at.control.shiftcontrol.shift.repo.PositionSlotRepository;
import at.control.shiftcontrol.shift.repo.ShiftPlanRepository;
import at.control.shiftcontrol.shift.repo.ShiftRepository;
import at.control.shiftcontrol.shift.repo.VolunteerRepository;
import io.restassured.http.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;

class ActivityIT extends RestITBase {
    private static final String EVENT_CLONE_PATH = "/events/%d/clone";
    private static final String ACTIVITY_COLLECTION_PATH = "/events/%d/activities";


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

    @AfterEach
    void tearDown() {




}

    @Test
    void createActivityAsAdminInsideEventBoundsSucceeds() {
        ActivityModificationDto modificationDto = ActivityModificationDto.builder()
            .name("ActivityA")
            .description("DescriptionA")
            .startTime(LocalDateTime.of(2024, 2, 1, 8, 0).toInstant(ZoneOffset.UTC))
            .endTime(LocalDateTime.of(2024, 2, 1, 12, 0).toInstant(ZoneOffset.UTC))
            .build();

        var response = postRequestAsAdmin(String.format(ACTIVITY_COLLECTION_PATH, eventA.getId()), modificationDto, ActivityDto.class);

        assertAll(
            () -> assertThat(response.getName()).isEqualTo(modificationDto.getName()),
            () -> assertThat(response.getDescription()).isEqualTo(modificationDto.getDescription()),
            () -> assertThat(response.getStartTime()).isEqualTo(modificationDto.getStartTime()),
            () -> assertThat(response.getEndTime()).isEqualTo(modificationDto.getEndTime())
        );
    }

    @Test
    void createActivityAsAdminOutsideEventBoundsReturnsBadRequest() {
        ActivityModificationDto modificationDto = ActivityModificationDto.builder()
            .name("ActivityB")
            .description("DescriptionB")
            .startTime(LocalDateTime.of(2024, 1, 30, 8, 0).toInstant(ZoneOffset.UTC))
            .endTime(LocalDateTime.of(2024, 1, 30, 12, 0).toInstant(ZoneOffset.UTC))
            .build();

        doRequestAsAdminAndAssertMessage(
            Method.POST,
            String.format(ACTIVITY_COLLECTION_PATH, eventA.getId()),
            modificationDto,
            HttpStatus.BAD_REQUEST.value(),
            "Activity time range must be within event time range",
            true
        );
    }
}
