package at.control.shiftcontrol.shift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.TestcontainersConfiguration;
import at.control.shiftcontrol.shift.assignment.AssignmentState;
import at.control.shiftcontrol.shift.assignment.AssignmentTransitionEntity;
import at.control.shiftcontrol.shift.assignment.AssignmentTransitionRepository;
import at.control.shiftcontrol.shift.assignment.StaffingAssignmentEntity;
import at.control.shiftcontrol.shift.assignment.StaffingAssignmentRepository;
import at.control.shiftcontrol.shift.availability.AvailabilityEntity;
import at.control.shiftcontrol.shift.availability.AvailabilityRepository;
import at.control.shiftcontrol.shift.plan.PlanInviteEntity;
import at.control.shiftcontrol.shift.plan.PlanInviteRepository;
import at.control.shiftcontrol.shift.plan.PlanInviteType;
import at.control.shiftcontrol.shift.plan.PlanMembershipEntity;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.PlanModeTransitionEntity;
import at.control.shiftcontrol.shift.plan.PlanModeTransitionRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanEntity;
import at.control.shiftcontrol.shift.plan.ShiftPlanRepository;
import at.control.shiftcontrol.shift.plan.ShiftRoleEntity;
import at.control.shiftcontrol.shift.plan.ShiftRoleRepository;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.PositionSlotRepository;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftEntity;
import at.control.shiftcontrol.shift.positionslot.StaffingShiftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ShiftPersistenceTest {
    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

    @org.springframework.beans.factory.annotation.Autowired
    private ShiftPlanRepository planRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private PlanMembershipRepository membershipRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private ShiftRoleRepository roleRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private PlanInviteRepository inviteRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private StaffingShiftRepository staffingShiftRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private PositionSlotRepository positionSlotRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private AvailabilityRepository availabilityRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private StaffingAssignmentRepository assignmentRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private AssignmentTransitionRepository assignmentTransitionRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private PlanModeTransitionRepository modeTransitionRepository;

    @Test
    void persistsThePlanMembershipRoleInviteShiftAndSlotAggregate() {
        ShiftPlanEntity plan = savePlan("Operations");
        PlanMembershipEntity membership = membershipRepository.saveAndFlush(new PlanMembershipEntity(plan.getId(), UUID.randomUUID(), true, NOW));
        ShiftRoleEntity role = new ShiftRoleEntity();
        role.setPlanId(plan.getId());
        role.setName("Safety");
        role.setSelfAssignable(true);
        role.setCreatedAt(NOW);
        role = roleRepository.saveAndFlush(role);
        membership.getRoleIds().add(role.getId());
        membershipRepository.saveAndFlush(membership);

        PlanInviteEntity invite = new PlanInviteEntity();
        invite.setPlanId(plan.getId());
        invite.setCode("invite-code");
        invite.setType(PlanInviteType.VOLUNTEER);
        invite.setActive(true);
        invite.setUses(0);
        invite.setCreatedAt(NOW);
        invite.setRoleIds(Set.of(role.getId()));
        invite = inviteRepository.saveAndFlush(invite);

        StaffingShiftEntity shift = new StaffingShiftEntity();
        shift.setPlanId(plan.getId());
        shift.setName("Information desk");
        shift.setStartTime(NOW);
        shift.setEndTime(NOW.plusSeconds(3600));
        shift.setLocationId(UUID.randomUUID());
        shift.setCreatedAt(NOW);
        shift = staffingShiftRepository.saveAndFlush(shift);

        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setStaffingShiftId(shift.getId());
        slot.setName("Door");
        slot.setDesiredVolunteerCount(2);
        slot.setRequiredRoleId(role.getId());
        slot.setCreatedAt(NOW);
        slot = positionSlotRepository.saveAndFlush(slot);

        assertEquals(Set.of(role.getId()), membershipRepository.findById(membership.getId()).orElseThrow().getRoleIds());
        assertEquals(Set.of(role.getId()), inviteRepository.findById(invite.getId()).orElseThrow().getRoleIds());
        assertEquals(role.getId(), positionSlotRepository.findById(slot.getId()).orElseThrow().getRequiredRoleId());
    }

    @Test
    void databasePreventsAmbiguousLocationSources() {
        ShiftPlanEntity plan = savePlan("Operations");
        StaffingShiftEntity invalid = new StaffingShiftEntity();
        invalid.setPlanId(plan.getId());
        invalid.setName("Information desk");
        invalid.setStartTime(NOW);
        invalid.setEndTime(NOW.plusSeconds(3600));
        invalid.setLocationId(UUID.randomUUID());
        invalid.setSessionId(UUID.randomUUID());
        invalid.setCreatedAt(NOW);

        assertThrows(DataIntegrityViolationException.class, () -> staffingShiftRepository.saveAndFlush(invalid));
    }

    @Test
    void persistsAvailabilityAssignmentAndTheirImmutableTransitionHistory() {
        ShiftPlanEntity plan = savePlan("Operations");
        StaffingShiftEntity shift = new StaffingShiftEntity();
        shift.setPlanId(plan.getId());
        shift.setName("Information desk");
        shift.setStartTime(NOW);
        shift.setEndTime(NOW.plusSeconds(3600));
        shift.setCreatedAt(NOW);
        shift = staffingShiftRepository.saveAndFlush(shift);
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setStaffingShiftId(shift.getId());
        slot.setName("Door");
        slot.setDesiredVolunteerCount(1);
        slot.setCreatedAt(NOW);
        slot = positionSlotRepository.saveAndFlush(slot);
        UUID userId = UUID.randomUUID();

        AvailabilityEntity availability = new AvailabilityEntity();
        availability.setEventId(plan.getEventId());
        availability.setUserId(userId);
        availability.setStartTime(NOW.plusSeconds(7200));
        availability.setEndTime(NOW.plusSeconds(10800));
        availability.setCreatedByUserId(userId);
        availability.setCreatedAt(NOW);
        availability = availabilityRepository.saveAndFlush(availability);

        StaffingAssignmentEntity assignment = new StaffingAssignmentEntity();
        assignment.setPositionSlotId(slot.getId());
        assignment.setUserId(userId);
        assignment.setState(AssignmentState.ACCEPTED);
        assignment.setCreatedAt(NOW);
        assignment.setUpdatedAt(NOW);
        assignment = assignmentRepository.saveAndFlush(assignment);
        AssignmentTransitionEntity assignmentTransition = new AssignmentTransitionEntity();
        assignmentTransition.setAssignmentId(assignment.getId());
        assignmentTransition.setToState(AssignmentState.ACCEPTED);
        assignmentTransition.setActorUserId(userId);
        assignmentTransition.setOccurredAt(NOW);
        assignmentTransitionRepository.saveAndFlush(assignmentTransition);

        PlanModeTransitionEntity modeTransition = new PlanModeTransitionEntity();
        modeTransition.setPlanId(plan.getId());
        modeTransition.setActorUserId(userId);
        modeTransition.setOldMode(StaffingMode.SELF_SIGNUP);
        modeTransition.setNewMode(StaffingMode.SUPERVISED);
        modeTransition.setApplicationPolicy("NO_REQUEST_RESOLUTION");
        modeTransition.setResolvedJoinRequestCount(0);
        modeTransition.setResolvedLeaveRequestCount(0);
        modeTransition.setOccurredAt(NOW);
        modeTransition = modeTransitionRepository.saveAndFlush(modeTransition);

        assertEquals(availability.getId(), availabilityRepository.findById(availability.getId()).orElseThrow().getId());
        assertEquals(AssignmentState.ACCEPTED, assignmentRepository.findById(assignment.getId()).orElseThrow().getState());
        assertTrue(assignmentRepository.existsOccupiedAssignmentOverlapping(plan.getEventId(), userId, NOW, NOW.plusSeconds(3600)));
        assertTrue(assignmentRepository.existsOccupiedAssignmentOverlappingShift(userId, NOW, NOW.plusSeconds(3600),
            Set.of(AssignmentState.ACCEPTED, AssignmentState.LEAVE_REQUESTED)));
        assertEquals(modeTransition.getNewMode(), modeTransitionRepository.findById(modeTransition.getId()).orElseThrow().getNewMode());
    }

    private ShiftPlanEntity savePlan(String name) {
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setEventId(UUID.randomUUID());
        plan.setName(name);
        plan.setDefaultStaffingMode(StaffingMode.SELF_SIGNUP);
        plan.setCreatedAt(NOW);
        return planRepository.saveAndFlush(plan);
    }
}
