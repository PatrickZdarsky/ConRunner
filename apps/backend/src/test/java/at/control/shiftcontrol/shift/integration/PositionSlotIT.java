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
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.auth.UserAttributeProvider;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotRequestDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.TotalPointsDto;
import at.control.shiftcontrol.shift.integration.config.RestITBase;
import at.control.shiftcontrol.shift.repo.AssignmentRepository;
import at.control.shiftcontrol.shift.repo.EventRepository;
import at.control.shiftcontrol.shift.repo.PositionSlotRepository;
import at.control.shiftcontrol.shift.repo.ShiftPlanRepository;
import at.control.shiftcontrol.shift.repo.ShiftRepository;
import at.control.shiftcontrol.shift.repo.VolunteerRepository;
import io.restassured.http.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;

class PositionSlotIT extends RestITBase {
    private static final String POSITIONSLOT_PATH = "/position-slots";
    private static final String SHIFT_POSITIONSLOT_PATH = "shifts/%d/position-slots";
    private static final String REWARDPOINTS_PATH = "/reward-points";
    private static final String LEAVE_POSITIONSLOT_PATH = "/position-slots/%s/leave";
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

    private Event eventA;
    private ShiftPlan shiftPlanA, shiftPlanB;
    private Shift shiftA, shiftB, shiftC;

    private Volunteer volunteerA, volunteerB, adminWithoutMembership;

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

        adminWithoutMembership = Volunteer.builder()
            .id(java.util.UUID.fromString("831b9f95-de0c-5986-bfe3-50183cb97f2d"))
            .volunteeringPlans(Set.of())
            .build();
        volunteers.add(adminWithoutMembership);

        volunteerRepository.saveAll(volunteers);
        assertAll(
            () -> assertThat(volunteerA.getId()).isNotNull(),
            () -> assertThat(volunteerB.getId()).isNotNull(),
            () -> assertThat(adminWithoutMembership.getId()).isNotNull(),
            () -> assertThat(volunteerA.getVolunteeringPlans()).contains(shiftPlanA),
            () -> assertThat(volunteerB.getVolunteeringPlans()).contains(shiftPlanA),
            () -> assertThat(adminWithoutMembership.getVolunteeringPlans()).isEmpty(),
            () -> assertThat(volunteerRepository.existsById(volunteerA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.existsById(volunteerB.getId())).isTrue(),
            () -> assertThat(volunteerRepository.existsById(adminWithoutMembership.getId())).isTrue()
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
        userAttributeProvider.invalidateUserCaches(Set.of(volunteerA.getId(), volunteerB.getId(), adminWithoutMembership.getId()));





}

    @Test
    void findEventByNonExistingIdReturnsNotFound() {
        doRequestAsAssignedAndAssertMessage(Method.GET, POSITIONSLOT_PATH + "/999", "", HttpStatus.NOT_FOUND.value(), "PositionSlot not found.", true,
            volunteerA.getId());
    }

    @Test
    void findPositionSlotByIdReturnsPositionSlot() {
        var response = getRequestAsAssigned(POSITIONSLOT_PATH + "/" + positionSlotA.getId(), PositionSlotDto.class, volunteerA.getId());

        assertAll(
            () -> assertThat(response).isNotNull(),
            () -> assertThat(response.getId()).isEqualTo(String.valueOf(positionSlotA.getId())),
            () -> assertThat(response.getName()).isEqualTo(positionSlotA.getName()),
            () -> assertThat(response.getDescription()).isEqualTo(positionSlotA.getDescription()),
            () -> assertThat(response.getDesiredVolunteerCount()).isEqualTo(positionSlotA.getDesiredVolunteerCount()),
            () -> assertThat(response.isSkipAutoAssignment()).isEqualTo(positionSlotA.isSkipAutoAssignment()),
            () -> assertThat(response.getRewardPointsDto().getOverrideRewardPoints()).isNull(),
            () -> assertThat(response.getRewardPointsDto().getCurrentRewardPoints()).isEqualTo(10 * 60),
            () -> assertThat(response.getRewardPointsDto().getRewardPointsConfigHash()).isNotBlank(),
            () -> assertThat(response.getAssignments()).singleElement().satisfies(assignment -> {
                assertThat(assignment.getAssignedVolunteer().getId()).isEqualTo(volunteerA.getId());
                assertThat(assignment.getAssignedVolunteer().getUsername()).isEqualTo("assigned-" + volunteerA.getId());
                assertThat(assignment.getAssignedVolunteer().getProfile()).isEqualTo("https://cdn.example.test/profiles/" + volunteerA.getId() + ".png");
            })
        );
    }

    @Test
    void auctionAssignmentForPositionSlotInSupervisedShiftPlanWorks() {
        var response = patchRequestAsAssigned(
            POSITIONSLOT_PATH + "/" + positionSlotA.getId() + "/auction",
            "",
            AssignmentDto.class,
            volunteerA.getId()
        );

        assertAll(
            () -> assertThat(response).isNotNull(),
            () -> assertThat(response.getPositionSlotId()).isEqualTo(String.valueOf(positionSlotA.getId())),
            () -> assertThat(response.getAssignedVolunteer().getId()).isEqualTo(volunteerA.getId()),
            () -> assertThat(response.getStatus()).isEqualTo(AssignmentStatus.AUCTION),
            () -> assertThat(response.getAcceptedRewardPoints()).isEqualTo(40) // unchanged from existing assignment
        );
    }

    @Test
    void claimAuctionForPositionSlotInSupervisedShiftPlanWorks() {
        // get me endpoint reward points before auction/claim
        var rewardPointsBeforeUserA = getRequestAsAssigned(
            REWARDPOINTS_PATH,
            TotalPointsDto.class,
            volunteerA.getId()
        );

        var rewardPointsBeforeUserB = getRequestAsAssigned(
            REWARDPOINTS_PATH,
            TotalPointsDto.class,
            volunteerB.getId()
        );

        // First put assignment up for auction as volunteerA
        patchRequestAsAssigned(
            POSITIONSLOT_PATH + "/" + positionSlotA.getId() + "/auction",
            "",
            AssignmentDto.class,
            volunteerA.getId()
        );

        // user wants to see current position slot to know its details/reward points
        var positionSlotBeforeClaim = getRequestAsAssigned(
            POSITIONSLOT_PATH + "/" + positionSlotA.getId(),
            PositionSlotDto.class,
            volunteerB.getId()
        );

        // Then claim the auction as volunteerB
        var response = putRequestAsAssigned(
            POSITIONSLOT_PATH + "/" + positionSlotA.getId() + "/claim-auction/" + volunteerA.getId(),
            new PositionSlotRequestDto(positionSlotBeforeClaim.getRewardPointsDto().getRewardPointsConfigHash()),
            AssignmentDto.class,
            volunteerB.getId()
        );

        var rewardPointsAfterUserA = getRequestAsAssigned(
            REWARDPOINTS_PATH,
            TotalPointsDto.class,
            volunteerA.getId()
        );

        var rewardPointsAfterUserB = getRequestAsAssigned(
            REWARDPOINTS_PATH,
            TotalPointsDto.class,
            volunteerB.getId()
        );

        // TODO also check here if assigments list of position slot of assigment has correct entries (old removed, new added)
        assertAll(
            () -> assertThat(response).isNotNull(),
            () -> assertThat(response.getPositionSlotId()).isEqualTo(String.valueOf(positionSlotA.getId())),
            () -> assertThat(response.getAssignedVolunteer().getId()).isEqualTo(volunteerB.getId()),
            () -> assertThat(response.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED),
            () -> assertThat(response.getAcceptedRewardPoints()).isEqualTo(10 * 60), // recalculated for new volunteer
            () -> assertThat(rewardPointsAfterUserA).isNotEqualTo(rewardPointsBeforeUserA),
            () -> assertThat(rewardPointsAfterUserA.getTotalPoints()).isEqualTo(
                rewardPointsBeforeUserA.getTotalPoints() - assignmentA.getAcceptedRewardPoints()),
            () -> assertThat(rewardPointsAfterUserB).isNotEqualTo(rewardPointsBeforeUserB),
            () -> assertThat(rewardPointsAfterUserB.getTotalPoints()).isEqualTo(rewardPointsBeforeUserB.getTotalPoints() + (10 * 60))
        );
    }

    @Test
    void joinAndLeaveMultipleTimesWorks() {
        var positionSlotBeforeJoin = getRequestAsAssigned(
            POSITIONSLOT_PATH + "/" + positionSlotC.getId(),
            PositionSlotDto.class,
            volunteerB.getId()
        );
        var requestDto = new PositionSlotRequestDto(positionSlotBeforeJoin.getRewardPointsDto().getRewardPointsConfigHash());

        var resultJoin1 = postRequestAsAssigned(
            JOIN_POSITIONSLOT_PATH.formatted(positionSlotC.getId()),
            requestDto,
            AssignmentDto.class,
            volunteerB.getId()
        );

        Assertions.assertNotNull(resultJoin1);
        Assertions.assertEquals(AssignmentStatus.ACCEPTED, resultJoin1.getStatus());
        Assertions.assertEquals(String.valueOf(positionSlotC.getId()), resultJoin1.getPositionSlotId());
        Assertions.assertEquals(volunteerB.getId(), resultJoin1.getAssignedVolunteer().getId());

        deleteRequestAsAssigned(
            LEAVE_POSITIONSLOT_PATH.formatted(positionSlotC.getId()),
            "",
            null,
            volunteerB.getId()
        );

        Assertions.assertFalse(assignmentRepository.findById(ConvertUtil.idToLong(resultJoin1.getId())).isPresent());

        var resultJoin2 = postRequestAsAssigned(
            JOIN_POSITIONSLOT_PATH.formatted(positionSlotC.getId()),
            requestDto,
            AssignmentDto.class,
            volunteerB.getId()
        );

        Assertions.assertNotNull(resultJoin2);
        Assertions.assertEquals(AssignmentStatus.ACCEPTED, resultJoin2.getStatus());
        Assertions.assertEquals(String.valueOf(positionSlotC.getId()), resultJoin2.getPositionSlotId());
        Assertions.assertEquals(volunteerB.getId(), resultJoin2.getAssignedVolunteer().getId());

        deleteRequestAsAssigned(
            LEAVE_POSITIONSLOT_PATH.formatted(positionSlotC.getId()),
            "",
            null,
            volunteerB.getId()
        );

        Assertions.assertFalse(assignmentRepository.findById(ConvertUtil.idToLong(resultJoin2.getId())).isPresent());
    }

    @Test
    void adminWithoutMembershipCannotJoinPositionSlot() {
        var positionSlotBeforeJoin = getRequestAsAssigned(
            POSITIONSLOT_PATH + "/" + positionSlotC.getId(),
            PositionSlotDto.class,
            volunteerB.getId()
        );
        var requestDto = new PositionSlotRequestDto(positionSlotBeforeJoin.getRewardPointsDto().getRewardPointsConfigHash());

        doRequest(
            Method.POST,
            JOIN_POSITIONSLOT_PATH.formatted(positionSlotC.getId()),
            requestDto,
            Map.of(),
            Map.of(
                HDR_TEST_USER_TYPE, "ADMIN",
                HDR_TEST_USER_ID, adminWithoutMembership.getId().toString(),
                HDR_TEST_USERNAME, "admin-not-in-plan"
            ),
            HttpStatus.FORBIDDEN.value(),
            null
        );
    }

    @Test
    void adminWithExistingVolunteerMembershipCanJoinPositionSlot() {
        var positionSlotBeforeJoin = getRequestAsAssigned(
            POSITIONSLOT_PATH + "/" + positionSlotC.getId(),
            PositionSlotDto.class,
            volunteerB.getId()
        );
        var requestDto = new PositionSlotRequestDto(positionSlotBeforeJoin.getRewardPointsDto().getRewardPointsConfigHash());

        var result = doRequest(
            Method.POST,
            JOIN_POSITIONSLOT_PATH.formatted(positionSlotC.getId()),
            requestDto,
            Map.of(),
            Map.of(
                HDR_TEST_USER_TYPE, "ADMIN",
                HDR_TEST_USER_ID, volunteerB.getId().toString(),
                HDR_TEST_USERNAME, "admin-" + volunteerB.getId()
            ),
            HttpStatus.OK.value(),
            AssignmentDto.class
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AssignmentStatus.ACCEPTED, result.getStatus());
        Assertions.assertEquals(String.valueOf(positionSlotC.getId()), result.getPositionSlotId());
        Assertions.assertEquals(volunteerB.getId(), result.getAssignedVolunteer().getId());
    }
}
