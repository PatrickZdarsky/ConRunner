package at.control.shiftcontrol.shift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.TestcontainersConfiguration;
import at.control.shiftcontrol.shift.assignment.AssignmentEntity;
import at.control.shiftcontrol.shift.assignment.AssignmentRepository;
import at.control.shiftcontrol.shift.assignment.AssignmentStatus;
import at.control.shiftcontrol.shift.plan.PlanInviteEntity;
import at.control.shiftcontrol.shift.plan.PlanInviteRepository;
import at.control.shiftcontrol.shift.plan.PlanInviteType;
import at.control.shiftcontrol.shift.plan.PlanMembershipEntity;
import at.control.shiftcontrol.shift.plan.PlanMembershipRepository;
import at.control.shiftcontrol.shift.plan.ShiftPlanEntity;
import at.control.shiftcontrol.shift.plan.ShiftPlanRepository;
import at.control.shiftcontrol.shift.plan.ShiftRoleEntity;
import at.control.shiftcontrol.shift.plan.ShiftRoleRepository;
import at.control.shiftcontrol.shift.plan.StaffingMode;
import at.control.shiftcontrol.shift.positionslot.PositionSlotEntity;
import at.control.shiftcontrol.shift.positionslot.PositionSlotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ShiftPersistenceTest {
    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

    @org.springframework.beans.factory.annotation.Autowired private ShiftPlanRepository planRepository;
    @org.springframework.beans.factory.annotation.Autowired private PlanMembershipRepository membershipRepository;
    @org.springframework.beans.factory.annotation.Autowired private ShiftRoleRepository roleRepository;
    @org.springframework.beans.factory.annotation.Autowired private PlanInviteRepository inviteRepository;
    @org.springframework.beans.factory.annotation.Autowired private ShiftRepository shiftRepository;
    @org.springframework.beans.factory.annotation.Autowired private PositionSlotRepository positionSlotRepository;
    @org.springframework.beans.factory.annotation.Autowired private TimeConstraintRepository timeConstraintRepository;
    @org.springframework.beans.factory.annotation.Autowired private AssignmentRepository assignmentRepository;

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
        ShiftEntity shift = saveShift(plan);
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setShiftId(shift.getId());
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
        ShiftEntity invalid = new ShiftEntity();
        invalid.setPlanId(savePlan("Operations").getId());
        invalid.setName("Information desk");
        invalid.setStartTime(NOW);
        invalid.setEndTime(NOW.plusSeconds(3600));
        invalid.setLocationId(UUID.randomUUID());
        invalid.setSessionId(UUID.randomUUID());
        invalid.setCreatedAt(NOW);

        assertThrows(DataIntegrityViolationException.class, () -> shiftRepository.saveAndFlush(invalid));
    }

    @Test
    void persistsLiveAssignmentsAndTypedTimeConstraintsWithoutTransitionTables() {
        ShiftPlanEntity plan = savePlan("Operations");
        ShiftEntity shift = saveShift(plan);
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setShiftId(shift.getId());
        slot.setName("Door");
        slot.setDesiredVolunteerCount(1);
        slot.setCreatedAt(NOW);
        slot = positionSlotRepository.saveAndFlush(slot);
        UUID userId = UUID.randomUUID();
        TimeConstraintEntity constraint = new TimeConstraintEntity();
        constraint.setEventId(plan.getEventId());
        constraint.setUserId(userId);
        constraint.setType(TimeConstraintType.UNAVAILABLE);
        constraint.setStartTime(NOW.plusSeconds(7200));
        constraint.setEndTime(NOW.plusSeconds(10800));
        constraint.setCreatedAt(NOW);
        constraint = timeConstraintRepository.saveAndFlush(constraint);
        AssignmentEntity assignment = new AssignmentEntity();
        assignment.setPositionSlotId(slot.getId());
        assignment.setUserId(userId);
        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment.setCreatedAt(NOW);
        assignment.setUpdatedAt(NOW);
        assignment = assignmentRepository.saveAndFlush(assignment);

        assertEquals(TimeConstraintType.UNAVAILABLE, timeConstraintRepository.findById(constraint.getId()).orElseThrow().getType());
        assertEquals(AssignmentStatus.ACCEPTED, assignmentRepository.findById(assignment.getId()).orElseThrow().getStatus());
        assertTrue(assignmentRepository.existsOccupiedAssignmentOverlappingShift(userId, NOW, NOW.plusSeconds(3600),
            Set.of(AssignmentStatus.ACCEPTED)));
    }

    private ShiftPlanEntity savePlan(String name) {
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setEventId(UUID.randomUUID());
        plan.setName(name);
        plan.setDefaultStaffingMode(StaffingMode.SELF_SIGNUP);
        plan.setCreatedAt(NOW);
        return planRepository.saveAndFlush(plan);
    }

    private ShiftEntity saveShift(ShiftPlanEntity plan) {
        ShiftEntity shift = new ShiftEntity();
        shift.setPlanId(plan.getId());
        shift.setName("Information desk");
        shift.setStartTime(NOW);
        shift.setEndTime(NOW.plusSeconds(3600));
        shift.setCreatedAt(NOW);
        return shiftRepository.saveAndFlush(shift);
    }
}
