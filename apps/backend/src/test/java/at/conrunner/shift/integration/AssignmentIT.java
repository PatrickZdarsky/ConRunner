package at.conrunner.shift.integration;

import static org.assertj.core.api.Assertions.assertThat;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.type.AssignmentStatus;
import at.conrunner.shift.lib.type.LockStatus;
import at.conrunner.shift.lib.type.TimeConstraintType;
import at.conrunner.shift.lib.util.ConvertUtil;
import at.conrunner.shift.auth.UserAttributeProvider;
import at.conrunner.shift.dto.TimeConstraintCreateDto;
import at.conrunner.shift.dto.TimeConstraintDto;
import at.conrunner.shift.dto.assignment.AssignmentDto;
import at.conrunner.shift.dto.positionslot.PositionSlotDto;
import at.conrunner.shift.dto.positionslot.PositionSlotRequestDto;
import at.conrunner.shift.integration.config.RestITBase;
import at.conrunner.shift.repo.AssignmentRepository;
import at.conrunner.shift.repo.EventRepository;
import at.conrunner.shift.repo.PositionSlotRepository;
import at.conrunner.shift.repo.ShiftPlanRepository;
import at.conrunner.shift.repo.ShiftRepository;
import at.conrunner.shift.repo.TimeConstraintRepository;
import at.conrunner.shift.repo.VolunteerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;

class AssignmentIT extends RestITBase {
    private static final String POSITIONSLOT_PATH = "/position-slots";
    private static final String JOIN_POSITIONSLOT_PATH = "/position-slots/%s/join";

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
    UserAttributeProvider userAttributeProvider;

    @Autowired
    private TimeConstraintRepository timeConstraintRepository;

    private Event eventA;
    private ShiftPlan shiftPlanA, shiftPlanB;
    private Shift shiftA, shiftB, shiftC;

    private Volunteer volunteerA, volunteerB;

    private PositionSlot positionSlotA, positionSlotB, positionSlotC;

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
        var shiftPlans = new ArrayList<ShiftPlan>();

        shiftPlanA = ShiftPlan.builder()
            .name("ShiftPlanA")
            .event(eventA)
            .lockStatus(LockStatus.SUPERVISED)
            .defaultNoRolePointsPerMinute(1)
            .build();
        shiftPlans.add(shiftPlanA);

        shiftPlanB = ShiftPlan.builder()
            .name("ShiftPlanB")
            .event(eventA)
            .lockStatus(LockStatus.SELF_SIGNUP)
            .defaultNoRolePointsPerMinute(1)
            .build();
        shiftPlans.add(shiftPlanB);

        shiftPlanRepository.saveAll(shiftPlans);

        assertAll(
            () -> assertThat(shiftPlanA.getId()).isGreaterThan(0),
            () -> assertThat(shiftPlanB.getId()).isGreaterThan(0),
            () -> assertThat(shiftPlanRepository.existsById(shiftPlanA.getId())).isTrue(),
            () -> assertThat(shiftPlanRepository.existsById(shiftPlanB.getId())).isTrue()
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


        var startTimeC = LocalDateTime.of(2024, 3, 1, 11, 0).toInstant(ZoneOffset.UTC);
        var endTimeC = LocalDateTime.of(2024, 3, 3, 18, 0).toInstant(ZoneOffset.UTC);

        shiftC = Shift.builder()
            .name("ShiftB")
            .shiftPlan(shiftPlanB)
            .shortDescription("ShortDescC")
            .longDescription("This is a description for ShiftC")
            .startTime(startTimeC)
            .endTime(endTimeC)
            .bonusRewardPoints(20)
            .build();
        shifts.add(shiftC);

        shiftRepository.saveAll(shifts);

        assertAll(
            () -> assertThat(shiftA.getId()).isGreaterThan(0),
            () -> assertThat(shiftB.getId()).isGreaterThan(0),
            () -> assertThat(shiftC.getId()).isGreaterThan(0),
            () -> assertThat(shiftRepository.existsById(shiftA.getId())).isTrue(),
            () -> assertThat(shiftRepository.existsById(shiftB.getId())).isTrue(),
            () -> assertThat(shiftRepository.existsById(shiftC.getId())).isTrue()
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

        positionSlotC = PositionSlot.builder()
            .name("positionSlotC")
            .skipAutoAssignment(true)
            .shift(shiftC)
            .description("DescriptionC")
            .desiredVolunteerCount(5)
            .overrideRewardPoints(70)
            .build();
        slots.add(positionSlotC);

        positionSlotRepository.saveAll(slots);

        assertAll(
            () -> assertThat(positionSlotA.getId()).isGreaterThan(0),
            () -> assertThat(positionSlotB.getId()).isGreaterThan(0),
            () -> assertThat(positionSlotC.getId()).isGreaterThan(0),
            () -> assertThat(positionSlotRepository.existsById(positionSlotA.getId())).isTrue(),
            () -> assertThat(positionSlotRepository.existsById(positionSlotB.getId())).isTrue(),
            () -> assertThat(positionSlotRepository.existsById(positionSlotC.getId())).isTrue()
        );
    }

    private void createVolunteers() {
        var volunteers = new ArrayList<Volunteer>();
        volunteerA = Volunteer.builder()
            .id(java.util.UUID.fromString("00000000-0000-0000-0000-000000011111"))
            .volunteeringPlans(Set.of(shiftPlanA, shiftPlanB))
            .build();
        volunteers.add(volunteerA);

        volunteerB = Volunteer.builder()
            .id(java.util.UUID.fromString("00000000-0000-0000-0000-000000022222"))
            .volunteeringPlans(Set.of(shiftPlanA, shiftPlanB))
            .build();
        volunteers.add(volunteerB);

        volunteerRepository.saveAll(volunteers);
        assertAll(
            () -> assertThat(volunteerA.getId()).isNotNull(),
            () -> assertThat(volunteerB.getId()).isNotNull(),
            () -> assertThat(volunteerA.getVolunteeringPlans()).contains(shiftPlanA),
            () -> assertThat(volunteerB.getVolunteeringPlans()).contains(shiftPlanA),
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
            () -> assertThat(assignmentA.getId()).isNotNull(),
            () -> assertThat(assignmentA.getPositionSlot()).isEqualTo(positionSlotA),
            () -> assertThat(assignmentA.getAssignedVolunteer()).isEqualTo(volunteerA),
            () -> assertThat(assignmentA.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED)
        );
    }

    @AfterEach
    void tearDown() {
        userAttributeProvider.invalidateUserCaches(Set.of(volunteerA.getId(), volunteerB.getId()));






}

    @Test
    void createEmergencyTimeConstraint_unassignsFromAssignments() {
        // We need to create a time constraint that unassigns the user from a shift.
        // This only works for SELF_SIGNUP shift plans.
        // shiftC is in shiftPlanB which is SELF_SIGNUP.
        // volunteerB is part of shiftPlanB.

        // 1. Assign volunteerB to a position slot in a SELF_SIGNUP shift plan (positionSlotC)
        var positionSlotDto = getRequestAsAssigned(
            POSITIONSLOT_PATH + "/" + positionSlotC.getId(),
            PositionSlotDto.class,
            volunteerB.getId()
        );
        var requestDto = new PositionSlotRequestDto(positionSlotDto.getRewardPointsDto().getRewardPointsConfigHash());

        var assignmentDto = postRequestAsAssigned(
            JOIN_POSITIONSLOT_PATH.formatted(positionSlotC.getId()),
            requestDto,
            AssignmentDto.class,
            volunteerB.getId()
        );

        Assertions.assertNotNull(assignmentDto);
        Assertions.assertTrue(assignmentRepository.findById(ConvertUtil.idToLong(assignmentDto.getId())).isPresent());

        // 2. Create an EMERGENCY time constraint that conflicts with the shift
        var shift = shiftRepository.findById(positionSlotC.getShift().getId()).orElseThrow();
        var eventId = shift.getShiftPlan().getEvent().getId();

        //Start and end time that fully covers the shift and is always from midnight to midnight
        var startTime = LocalDate.ofInstant(shift.getStartTime(), ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        var endTime = LocalDate.ofInstant(shift.getStartTime(), ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC);

        var timeConstraintCreateDto = new TimeConstraintCreateDto(
            TimeConstraintType.EMERGENCY,
            startTime,
            endTime
        );

        postRequestAsAssigned(
            "/events/" + eventId + "/time-constraints",
            timeConstraintCreateDto,
            TimeConstraintDto.class,
            volunteerB.getId()
        );

        // 3. Verify that the assignment has been removed
        Assertions.assertFalse(assignmentRepository.findById(ConvertUtil.idToLong(assignmentDto.getId())).isPresent());
    }

    @Test
    void createEmergencyTimeConstraint_requestsUnassignFromAssignments_whenShiftPlanSupervised() {
        // We need to create a time constraint that sets the assignment to AUCTION_REQUEST_FOR_UNASSIGN for the user from a shift.
        // This only works for SUPERVISED shift plans.
        // shiftC is in shiftPlanB which is SELF_SIGNUP.
        // volunteerB is part of shiftPlanB.

        // 1. Assign volunteerB to a position slot in a SELF_SIGNUP shift plan (positionSlotC)
        var positionSlotDto = getRequestAsAssigned(
            POSITIONSLOT_PATH + "/" + positionSlotC.getId(),
            PositionSlotDto.class,
            volunteerB.getId()
        );
        var requestDto = new PositionSlotRequestDto(positionSlotDto.getRewardPointsDto().getRewardPointsConfigHash());

        var assignmentDto = postRequestAsAssigned(
            JOIN_POSITIONSLOT_PATH.formatted(positionSlotC.getId()),
            requestDto,
            AssignmentDto.class,
            volunteerB.getId()
        );

        Assertions.assertNotNull(assignmentDto);
        Assertions.assertTrue(assignmentRepository.findById(ConvertUtil.idToLong(assignmentDto.getId())).isPresent());

        // 2. Set the shift plan to SUPERVISED
        var shift = shiftRepository.findById(positionSlotC.getShift().getId()).orElseThrow();
        var shiftPlan = shift.getShiftPlan();
        shiftPlan.setLockStatus(LockStatus.SUPERVISED);
        shiftPlanRepository.save(shiftPlan);

        // 3. Create an EMERGENCY time constraint that conflicts with the shift
        var eventId = shift.getShiftPlan().getEvent().getId();

        //Start and end time that fully covers the shift and is always from midnight to midnight
        var startTime = LocalDate.ofInstant(shift.getStartTime(), ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        var endTime = LocalDate.ofInstant(shift.getStartTime(), ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC);

        var timeConstraintCreateDto = new TimeConstraintCreateDto(
            TimeConstraintType.EMERGENCY,
            startTime,
            endTime
        );

        postRequestAsAssigned(
            "/events/" + eventId + "/time-constraints",
            timeConstraintCreateDto,
            TimeConstraintDto.class,
            volunteerB.getId()
        );

        // 4. Verify that the assignment has been updated to AUCTION_REQUEST_FOR_UNASSIGN
        var updatedAssignment = assignmentRepository.findById(ConvertUtil.idToLong(assignmentDto.getId())).orElseThrow();
        Assertions.assertEquals(AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN, updatedAssignment.getStatus());
    }
}
