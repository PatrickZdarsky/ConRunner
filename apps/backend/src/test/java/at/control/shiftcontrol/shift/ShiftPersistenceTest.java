package at.control.shiftcontrol.shift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import at.control.shiftcontrol.TestcontainersConfiguration;
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

    private ShiftPlanEntity savePlan(String name) {
        ShiftPlanEntity plan = new ShiftPlanEntity();
        plan.setEventId(UUID.randomUUID());
        plan.setName(name);
        plan.setDefaultStaffingMode(StaffingMode.SELF_SIGNUP);
        plan.setCreatedAt(NOW);
        return planRepository.saveAndFlush(plan);
    }
}
