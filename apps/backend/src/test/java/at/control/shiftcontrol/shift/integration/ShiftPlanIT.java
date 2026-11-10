package at.control.shiftcontrol.shift.integration;

import static org.assertj.core.api.Assertions.assertThat;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Role;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.event.BaseEvent;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.lib.type.LockStatus;
import at.control.shiftcontrol.shift.lib.type.ShiftPlanInviteType;
import at.control.shiftcontrol.shift.auth.UserAttributeProvider;
import at.control.shiftcontrol.shift.config.RabbitMqConfig;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentAssignDto;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentDto;
import at.control.shiftcontrol.shift.dto.invite.ShiftPlanInviteCreateRequestDto;
import at.control.shiftcontrol.shift.dto.invite.ShiftPlanInviteCreateResponseDto;
import at.control.shiftcontrol.shift.dto.invite.ShiftPlanInviteDetailsDto;
import at.control.shiftcontrol.shift.dto.invite.ShiftPlanJoinRequestDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanCreateDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanModificationDto;
import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;
import at.control.shiftcontrol.shift.integration.config.RestITBase;
import at.control.shiftcontrol.shift.repo.AssignmentRepository;
import at.control.shiftcontrol.shift.repo.EventRepository;
import at.control.shiftcontrol.shift.repo.PositionSlotRepository;
import at.control.shiftcontrol.shift.repo.ShiftPlanInviteRepository;
import at.control.shiftcontrol.shift.repo.ShiftPlanRepository;
import at.control.shiftcontrol.shift.repo.ShiftRepository;
import at.control.shiftcontrol.shift.repo.VolunteerRepository;
import at.control.shiftcontrol.shift.repo.role.RoleRepository;
import io.restassured.http.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;

class ShiftPlanIT extends RestITBase {
    private static final String SHIFTPLAN_COLLECTION_PATH = "events/%s/shift-plans";
    private static final String INVITE_PATH = "shift-plans/%d/invites";
    private static final String INVITE_ITEM_PATH = "/invites/%s";
    private static final String JOIN_PATH = "/join";
    private static final String ASSIGNABLE_USERS_PATH = "shift-plans/%d/assignable-users/%d";
    private static final String ASSIGN_PATH = "shift-plans/%d/assign";

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
    ShiftPlanInviteRepository shiftPlanInviteRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserAttributeProvider userAttributeProvider;

    private Event eventA;
    private ShiftPlan shiftPlanA;
    private Shift shiftA, shiftB;

    private Volunteer volunteerNotJoined, volunteerJoinedAsVolunteerOnly, volunteerJoinedAsPlannerOnly;

    private PositionSlot positionSlotA, positionSlotB;

    private Role roleA, roleB, roleC;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {







        createEvents();
        createShiftPlans();
        createRoles();
        createShifts();
        createPositionSlots();
        createVolunteers();
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

    private void createRoles() {
        var roles = new ArrayList<Role>();

        roleA = Role.builder()
            .name("RoleA")
            .shiftPlan(shiftPlanA)
            .description("DescriptionA")
            .selfAssignable(true)
            .rewardPointsPerMinute(2)
            .build();
        roles.add(roleA);

        roleB = Role.builder()
            .name("RoleB")
            .shiftPlan(shiftPlanA)
            .description("DescriptionB")
            .selfAssignable(true)
            .rewardPointsPerMinute(3)
            .build();
        roles.add(roleB);

        roleC = Role.builder()
            .name("RoleC")
            .shiftPlan(shiftPlanA)
            .description("DescriptionC")
            .selfAssignable(true)
            .rewardPointsPerMinute(5)
            .build();
        roles.add(roleC);

        roleRepository.saveAll(roles);

        assertAll(
            () -> assertThat(roleA.getId()).isGreaterThan(0),
            () -> assertThat(roleB.getId()).isGreaterThan(0),
            () -> assertThat(roleC.getId()).isGreaterThan(0),
            () -> assertThat(roleRepository.existsById(roleA.getId())).isTrue(),
            () -> assertThat(roleRepository.existsById(roleB.getId())).isTrue(),
            () -> assertThat(roleRepository.existsById(roleC.getId())).isTrue()
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
        volunteerNotJoined = Volunteer.builder()
            .id(java.util.UUID.fromString("00000000-0000-0000-0000-000000011111"))
            .build();
        volunteers.add(volunteerNotJoined);

        volunteerJoinedAsVolunteerOnly = Volunteer.builder()
            .id(java.util.UUID.fromString("00000000-0000-0000-0000-000000022222"))
            .build();
        shiftPlanA.addPlanVolunteer(volunteerJoinedAsVolunteerOnly);
        volunteers.add(volunteerJoinedAsVolunteerOnly);

        volunteerJoinedAsPlannerOnly = Volunteer.builder()
            .id(java.util.UUID.fromString("00000000-0000-0000-0000-000000033333"))
            .build();
        shiftPlanA.addPlanPlanner(volunteerJoinedAsPlannerOnly);
        volunteers.add(volunteerJoinedAsPlannerOnly);

        volunteerRepository.saveAll(volunteers);
        assertAll(
            () -> assertThat(volunteerNotJoined.getId()).isNotNull(),
            () -> assertThat(volunteerJoinedAsVolunteerOnly.getId()).isNotNull(),
            () -> assertThat(volunteerJoinedAsPlannerOnly.getId()).isNotNull(),
            () -> assertThat(volunteerRepository.existsById(volunteerNotJoined.getId())).isTrue(),
            () -> assertThat(volunteerRepository.existsById(volunteerJoinedAsVolunteerOnly.getId())).isTrue(),
            () -> assertThat(volunteerRepository.existsById(volunteerJoinedAsPlannerOnly.getId())).isTrue(),
            () -> assertThat(volunteerRepository.isVolunteerInShiftPlan(java.util.UUID.fromString("00000000-0000-0000-0000-000000011111"), shiftPlanA.getId())).isFalse(),
            () -> assertThat(volunteerRepository.isPlannerInShiftPlan(java.util.UUID.fromString("00000000-0000-0000-0000-000000011111"), shiftPlanA.getId())).isFalse(),
            () -> assertThat(volunteerRepository.isVolunteerInShiftPlan(java.util.UUID.fromString("00000000-0000-0000-0000-000000022222"), shiftPlanA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.isPlannerInShiftPlan(java.util.UUID.fromString("00000000-0000-0000-0000-000000022222"), shiftPlanA.getId())).isFalse(),
            () -> assertThat(volunteerRepository.isPlannerInShiftPlan(java.util.UUID.fromString("00000000-0000-0000-0000-000000033333"), shiftPlanA.getId())).isTrue()
        );
    }

    @AfterEach
    void tearDown() {





        // invalidate caches after each test
        userAttributeProvider.invalidateUserCache(volunteerNotJoined.getId());
        userAttributeProvider.invalidateUserCache(volunteerJoinedAsVolunteerOnly.getId());
        userAttributeProvider.invalidateUserCache(volunteerJoinedAsPlannerOnly.getId());
    }

    @Test
    void createShiftPlanAsNotJoinedReturnsForbidden() {
        var modificationDto = ShiftPlanModificationDto.builder()
            .name("NewShiftPlan")
            .defaultNoRolePointsPerMinute(1)
            .build();

        doRequestAsAssignedAndAssertMessage(
            Method.POST,
            SHIFTPLAN_COLLECTION_PATH.formatted(eventA.getId()),
            modificationDto,
            HttpStatus.FORBIDDEN.value(),
            "Access Denied",
            volunteerNotJoined.getId()
        );
    }

    @Test
    void createShiftPlanAsVolunteerReturnsForbidden() {
        var modificationDto = ShiftPlanModificationDto.builder()
            .name("NewShiftPlan")
            .defaultNoRolePointsPerMinute(1)
            .build();

        doRequestAsAssignedAndAssertMessage(
            Method.POST,
            SHIFTPLAN_COLLECTION_PATH.formatted(eventA.getId()),
            modificationDto,
            HttpStatus.FORBIDDEN.value(),
            "Access Denied",
            volunteerJoinedAsVolunteerOnly.getId()
        );
    }

    @Test
    void createShiftPlanAsPlannerReturnsForbidden() {
        var modificationDto = ShiftPlanModificationDto.builder()
            .name("NewShiftPlan")
            .defaultNoRolePointsPerMinute(1)
            .build();

        doRequestAsAssignedAndAssertMessage(
            Method.POST,
            SHIFTPLAN_COLLECTION_PATH.formatted(eventA.getId()),
            modificationDto,
            HttpStatus.FORBIDDEN.value(),
            "Access Denied",
            volunteerJoinedAsPlannerOnly.getId()
        );
    }

    @Test
    void createShiftPlanAsAdminSucceeds() {
        var modificationDto = ShiftPlanModificationDto.builder()
            .name("NewShiftPlan")
            .defaultNoRolePointsPerMinute(1)
            .build();

        var result = postRequestAsAdmin(
            SHIFTPLAN_COLLECTION_PATH.formatted(eventA.getId()),
            modificationDto,
            ShiftPlanCreateDto.class
        );

        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getShiftPlan()).isNotNull(),
            () -> assertThat(result.getShiftPlan().getId()).isNotNull(),
            () -> assertThat(result.getShiftPlan().getName()).isEqualTo("NewShiftPlan"),
            () -> assertThat(result.getVolunteerInvite()).isNotNull(),
            () -> assertThat(result.getVolunteerInvite().getCode()).isNotNull(),
            () -> assertThat(result.getVolunteerInvite().getCode()).isNotBlank(),
            () -> assertThat(result.getVolunteerInvite().getType()).isEqualTo(ShiftPlanInviteType.VOLUNTEER_JOIN),
            () -> assertThat(result.getVolunteerInvite().getShiftPlanDto().getId()).isEqualTo(result.getShiftPlan().getId()),
            () -> assertThat(result.getPlannerInvite()).isNotNull(),
            () -> assertThat(result.getPlannerInvite().getCode()).isNotNull(),
            () -> assertThat(result.getPlannerInvite().getCode()).isNotBlank(),
            () -> assertThat(result.getPlannerInvite().getType()).isEqualTo(ShiftPlanInviteType.PLANNER_JOIN),
            () -> assertThat(result.getPlannerInvite().getShiftPlanDto().getId()).isEqualTo(result.getShiftPlan().getId())
        );
    }

    @Test
    void createVolunteerInviteCodeAsNotJoinedReturnsForbidden() {
        var requestDto = ShiftPlanInviteCreateRequestDto.builder()
            .type(ShiftPlanInviteType.VOLUNTEER_JOIN)
            .build();
        doRequestAsAssignedAndAssertMessage(
            Method.POST,
            INVITE_PATH.formatted(shiftPlanA.getId()),
            requestDto,
            HttpStatus.FORBIDDEN.value(),
            "User is not a planner in shift plan.",
            volunteerNotJoined.getId()
        );
    }

    @Test
    void createPlannerInviteCodeAsNotJoinedReturnsForbidden() {
        var requestDto = ShiftPlanInviteCreateRequestDto.builder()
            .type(ShiftPlanInviteType.PLANNER_JOIN)
            .build();
        doRequestAsAssignedAndAssertMessage(
            Method.POST,
            INVITE_PATH.formatted(shiftPlanA.getId()),
            requestDto,
            HttpStatus.FORBIDDEN.value(),
            "Only admins can create planner join invite codes.",
            volunteerNotJoined.getId()
        );
    }

    @Test
    void createVolunteerInviteCodeAsVolunteerOnlyReturnsForbidden() {
        var requestDto = ShiftPlanInviteCreateRequestDto.builder()
            .type(ShiftPlanInviteType.VOLUNTEER_JOIN)
            .build();
        doRequestAsAssignedAndAssertMessage(
            Method.POST,
            INVITE_PATH.formatted(shiftPlanA.getId()),
            requestDto,
            HttpStatus.FORBIDDEN.value(),
            "User is not a planner in shift plan.",
            volunteerJoinedAsVolunteerOnly.getId()
        );
    }

    @Test
    void createPlannerInviteCodeAsVolunteerOnlyReturnsForbidden() {
        var requestDto = ShiftPlanInviteCreateRequestDto.builder()
            .type(ShiftPlanInviteType.PLANNER_JOIN)
            .build();
        doRequestAsAssignedAndAssertMessage(
            Method.POST,
            INVITE_PATH.formatted(shiftPlanA.getId()),
            requestDto,
            HttpStatus.FORBIDDEN.value(),
            "Only admins can create planner join invite codes.",
            volunteerJoinedAsVolunteerOnly.getId()
        );
    }

    @Test
    void createVolunteerInviteCodeAsPlannerOnlySucceeds() {
        var requestDto = ShiftPlanInviteCreateRequestDto.builder()
            .type(ShiftPlanInviteType.VOLUNTEER_JOIN)
            .build();

        var result = postRequestAsAssigned(
            INVITE_PATH.formatted(shiftPlanA.getId()),
            requestDto,
            ShiftPlanInviteCreateResponseDto.class,
            volunteerJoinedAsPlannerOnly.getId()
        );

        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getCode()).isNotNull(),
            () -> assertThat(result.getCode()).isNotBlank(),
            () -> assertThat(result.getType()).isEqualTo(ShiftPlanInviteType.VOLUNTEER_JOIN)
        );
    }

    @Test
    void createPlannerInviteCodeAsPlannerOnlyReturnsForbidden() {
        var requestDto = ShiftPlanInviteCreateRequestDto.builder()
            .type(ShiftPlanInviteType.PLANNER_JOIN)
            .build();
        doRequestAsAssignedAndAssertMessage(
            Method.POST,
            INVITE_PATH.formatted(shiftPlanA.getId()),
            requestDto,
            HttpStatus.FORBIDDEN.value(),
            "Only admins can create planner join invite codes.",
            volunteerJoinedAsPlannerOnly.getId()
        );
    }

    @Test
    void createPlannerInviteCodeAsAdminSucceeds() {
        var requestDto = ShiftPlanInviteCreateRequestDto.builder()
            .type(ShiftPlanInviteType.PLANNER_JOIN)
            .build();

        var result = postRequestAsAdmin(
            INVITE_PATH.formatted(shiftPlanA.getId()),
            requestDto,
            ShiftPlanInviteCreateResponseDto.class
        );

        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getCode()).isNotNull(),
            () -> assertThat(result.getCode()).isNotBlank(),
            () -> assertThat(result.getType()).isEqualTo(ShiftPlanInviteType.PLANNER_JOIN)
        );
    }

    @Test
    void createVolunteerInviteCodeAsPlannerAndJoinAsNotJoinedSucceeds() {
        var requestDto = ShiftPlanInviteCreateRequestDto.builder()
            .type(ShiftPlanInviteType.VOLUNTEER_JOIN)
            .autoAssignRoleIds(List.of(String.valueOf(roleA.getId()), String.valueOf(roleB.getId())))
            .build();

        var resultInvite = postRequestAsAssigned(
            INVITE_PATH.formatted(shiftPlanA.getId()),
            requestDto,
            ShiftPlanInviteCreateResponseDto.class,
            volunteerJoinedAsPlannerOnly.getId()
        );


        assertAll(
            () -> assertThat(resultInvite).isNotNull(),
            () -> assertThat(resultInvite.getCode()).isNotNull(),
            () -> assertThat(resultInvite.getCode()).isNotBlank(),
            () -> assertThat(resultInvite.getType()).isEqualTo(ShiftPlanInviteType.VOLUNTEER_JOIN),
            () -> assertThat(volunteerRepository.isVolunteerInShiftPlan(volunteerNotJoined.getId(), shiftPlanA.getId())).isFalse(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleA.getId())).isFalse(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleB.getId())).isFalse()
        );

        // Now join as volunteer using the invite code
        var joinRequestDto = ShiftPlanJoinRequestDto.builder()
            .inviteCode(resultInvite.getCode())
            .build();

        postRequestAsAssigned(
            JOIN_PATH,
            joinRequestDto,
            null,
            volunteerNotJoined.getId()
        );

        var inviteDetails = getRequestAsAssigned(
            INVITE_ITEM_PATH.formatted(resultInvite.getCode()),
            ShiftPlanInviteDetailsDto.class,
            volunteerJoinedAsVolunteerOnly.getId()
        );

        assertAll(
            () -> assertThat(inviteDetails).isNotNull(),
            () -> assertThat(inviteDetails.isJoined()).isTrue(),
            () -> assertThat(inviteDetails.getAttendingVolunteerCount()).isEqualTo(2),
            () -> assertThat(inviteDetails.getEventDto()).isNotNull(),
            () -> assertThat(inviteDetails.getInviteDto()).isNotNull(),
            () -> assertThat(volunteerRepository.isVolunteerInShiftPlan(volunteerNotJoined.getId(), shiftPlanA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleB.getId())).isTrue()
        );
    }

    @Test
    void createTwoVolunteerInviteCodesAsPlannerAndJoinBothAsNotJoinedExtendsRolesSucceeds() {
        // Create first invite
        var requestDto1 = ShiftPlanInviteCreateRequestDto.builder()
            .type(ShiftPlanInviteType.VOLUNTEER_JOIN)
            .autoAssignRoleIds(List.of(String.valueOf(roleA.getId()), String.valueOf(roleB.getId())))
            .build();

        var resultInvite1 = postRequestAsAssigned(
            INVITE_PATH.formatted(shiftPlanA.getId()),
            requestDto1,
            ShiftPlanInviteCreateResponseDto.class,
            volunteerJoinedAsPlannerOnly.getId()
        );

        assertAll(
            () -> assertThat(resultInvite1).isNotNull(),
            () -> assertThat(resultInvite1.getCode()).isNotNull(),
            () -> assertThat(resultInvite1.getCode()).isNotBlank(),
            () -> assertThat(resultInvite1.getType()).isEqualTo(ShiftPlanInviteType.VOLUNTEER_JOIN),
            () -> assertThat(volunteerRepository.isVolunteerInShiftPlan(volunteerNotJoined.getId(), shiftPlanA.getId())).isFalse(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleA.getId())).isFalse(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleB.getId())).isFalse()
        );

        // Join first time
        var joinRequestDto1 = ShiftPlanJoinRequestDto.builder()
            .inviteCode(resultInvite1.getCode())
            .build();

        postRequestAsAssigned(
            JOIN_PATH,
            joinRequestDto1,
            null,
            volunteerNotJoined.getId()
        );

        var inviteDetails1 = getRequestAsAssigned(
            INVITE_ITEM_PATH.formatted(resultInvite1.getCode()),
            ShiftPlanInviteDetailsDto.class,
            volunteerJoinedAsVolunteerOnly.getId()
        );

        assertAll(
            () -> assertThat(inviteDetails1).isNotNull(),
            () -> assertThat(inviteDetails1.isJoined()).isTrue(),
            () -> assertThat(inviteDetails1.getAttendingVolunteerCount()).isEqualTo(2),
            () -> assertThat(inviteDetails1.getEventDto()).isNotNull(),
            () -> assertThat(inviteDetails1.getInviteDto()).isNotNull(),
            () -> assertThat(volunteerRepository.isVolunteerInShiftPlan(volunteerNotJoined.getId(), shiftPlanA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleB.getId())).isTrue()
        );

        // Create second invite
        var requestDto2 = ShiftPlanInviteCreateRequestDto.builder()
            .type(ShiftPlanInviteType.VOLUNTEER_JOIN)
            .autoAssignRoleIds(List.of(String.valueOf(roleC.getId())))
            .build();

        var resultInvite2 = postRequestAsAssigned(
            INVITE_PATH.formatted(shiftPlanA.getId()),
            requestDto2,
            ShiftPlanInviteCreateResponseDto.class,
            volunteerJoinedAsPlannerOnly.getId()
        );

        assertAll(
            () -> assertThat(resultInvite2).isNotNull(),
            () -> assertThat(resultInvite2.getCode()).isNotNull(),
            () -> assertThat(resultInvite2.getCode()).isNotBlank(),
            () -> assertThat(resultInvite2.getType()).isEqualTo(ShiftPlanInviteType.VOLUNTEER_JOIN),
            () -> assertThat(volunteerRepository.isVolunteerInShiftPlan(volunteerNotJoined.getId(), shiftPlanA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleB.getId())).isTrue(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleC.getId())).isFalse()
        );

        // Join second time
        var joinRequestDto2 = ShiftPlanJoinRequestDto.builder()
            .inviteCode(resultInvite2.getCode())
            .build();

        postRequestAsAssigned(
            JOIN_PATH,
            joinRequestDto2,
            null,
            volunteerNotJoined.getId()
        );

        var inviteDetails2 = getRequestAsAssigned(
            INVITE_ITEM_PATH.formatted(resultInvite2.getCode()),
            ShiftPlanInviteDetailsDto.class,
            volunteerJoinedAsVolunteerOnly.getId()
        );

        assertAll(
            () -> assertThat(inviteDetails2).isNotNull(),
            () -> assertThat(inviteDetails2.isJoined()).isTrue(),
            () -> assertThat(inviteDetails2.getAttendingVolunteerCount()).isEqualTo(2),
            () -> assertThat(inviteDetails2.getEventDto()).isNotNull(),
            () -> assertThat(inviteDetails2.getInviteDto()).isNotNull(),
            () -> assertThat(volunteerRepository.isVolunteerInShiftPlan(volunteerNotJoined.getId(), shiftPlanA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleB.getId())).isTrue(),
            () -> assertThat(volunteerRepository.hasUserRole(volunteerNotJoined.getId(), roleC.getId())).isTrue()
        );
    }

    @Test
    void createPlannerInviteCodesAsAdminAndJoinAsVolunteerOnlyUpgradesToPlannerSucceeds() {
        var requestDto = ShiftPlanInviteCreateRequestDto.builder()
            .type(ShiftPlanInviteType.PLANNER_JOIN)
            .build();

        var resultInvite = postRequestAsAdmin(
            INVITE_PATH.formatted(shiftPlanA.getId()),
            requestDto,
            ShiftPlanInviteCreateResponseDto.class
        );

        assertAll(
            () -> assertThat(resultInvite).isNotNull(),
            () -> assertThat(resultInvite.getCode()).isNotNull(),
            () -> assertThat(resultInvite.getCode()).isNotBlank(),
            () -> assertThat(resultInvite.getType()).isEqualTo(ShiftPlanInviteType.PLANNER_JOIN),
            () -> assertThat(volunteerRepository.isVolunteerInShiftPlan(volunteerJoinedAsVolunteerOnly.getId(), shiftPlanA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.isPlannerInShiftPlan(volunteerJoinedAsVolunteerOnly.getId(), shiftPlanA.getId())).isFalse()
        );

        // Now join as planner using the invite code
        var joinRequestDto = ShiftPlanJoinRequestDto.builder()
            .inviteCode(resultInvite.getCode())
            .build();

        postRequestAsAssigned(
            JOIN_PATH,
            joinRequestDto,
            null,
            volunteerJoinedAsVolunteerOnly.getId()
        );

        var inviteDetails = getRequestAsAssigned(
            INVITE_ITEM_PATH.formatted(resultInvite.getCode()),
            ShiftPlanInviteDetailsDto.class,
            volunteerJoinedAsVolunteerOnly.getId()
        );

        assertAll(
            () -> assertThat(inviteDetails).isNotNull(),
            () -> assertThat(inviteDetails.isJoined()).isTrue(),
            () -> assertThat(inviteDetails.getAttendingVolunteerCount()).isEqualTo(1), // nothing changes
            () -> assertThat(inviteDetails.getEventDto()).isNotNull(),
            () -> assertThat(inviteDetails.getInviteDto()).isNotNull(),
            () -> assertThat(volunteerRepository.isVolunteerInShiftPlan(volunteerJoinedAsVolunteerOnly.getId(), shiftPlanA.getId())).isTrue(),
            () -> assertThat(volunteerRepository.isPlannerInShiftPlan(volunteerJoinedAsVolunteerOnly.getId(), shiftPlanA.getId())).isTrue()
        );
    }

    @Test
    void getAssignableUsersAsPlannerSucceeds() {
        var result = getRequestAsAssigned(
            ASSIGNABLE_USERS_PATH.formatted(shiftPlanA.getId(), positionSlotA.getId()),
            Collection.class,
            volunteerJoinedAsPlannerOnly.getId()
        );

        Assertions.assertFalse(result.isEmpty());
        VolunteerDto volunteerDto = objectMapper.convertValue(result.stream().findFirst().get(), VolunteerDto.class);
        Assertions.assertEquals(volunteerDto.getId(), volunteerJoinedAsVolunteerOnly.getId());
    }

    @Test
    void assignUsersToSlotAsPlannerSucceeds() {
        var assignmentAssignDto = AssignmentAssignDto.builder()
            .positionSlotId(String.valueOf(positionSlotA.getId()))
            .volunteerIds(List.of(volunteerJoinedAsVolunteerOnly.getId()))
            .build();

        var result = postRequestAsAssigned(
            ASSIGN_PATH.formatted(shiftPlanA.getId()),
            assignmentAssignDto,
            Collection.class,
            volunteerJoinedAsPlannerOnly.getId()
        );

        Assertions.assertEquals(1, result.size());
        AssignmentDto assignmentDto = objectMapper.convertValue(result.stream().findFirst().get(), AssignmentDto.class);
        Assertions.assertAll(
            () -> Assertions.assertEquals(AssignmentStatus.ACCEPTED, assignmentDto.getStatus()),
            () -> Assertions.assertEquals(String.valueOf(positionSlotA.getId()), assignmentDto.getPositionSlotId()),
            () -> Assertions.assertEquals(volunteerJoinedAsVolunteerOnly.getId(), assignmentDto.getAssignedVolunteer().getId())
        );
    }

    @Test
    void createShiftPlan_withDuplicateName_shouldFail() {
        var modificationDto = new ShiftPlanModificationDto("duplicate-plan-name", "desc", "long desc", 10);

        // Create the first shift plan
        postRequestAsAdmin(
            SHIFTPLAN_COLLECTION_PATH.formatted(eventA.getId()),
            modificationDto,
            ShiftPlanCreateDto.class
        );

        // Try to create a second shift plan with the same name and expect failure
        postRequestAsAdmin(
            SHIFTPLAN_COLLECTION_PATH.formatted(eventA.getId()),
            modificationDto,
            ShiftPlanCreateDto.class,
            HttpStatus.BAD_REQUEST.value()
        );
    }
}
