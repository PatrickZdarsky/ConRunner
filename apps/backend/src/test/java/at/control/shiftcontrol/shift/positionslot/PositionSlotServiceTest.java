package at.control.shiftcontrol.shift.positionslot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.shift.assignment.StaffingAssignmentRepository;
import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.plan.ShiftRoleEntity;
import at.control.shiftcontrol.shift.plan.ShiftRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PositionSlotServiceTest {
    @Mock
    private PositionSlotRepository positionSlotRepository;

    @Mock
    private StaffingShiftService staffingShiftService;

    @Mock
    private ShiftRoleRepository roleRepository;

    @Mock
    private StaffingAssignmentRepository assignmentRepository;

    @Mock
    private PlanAccess planAccess;

    @Mock
    private Clock clock;

    @InjectMocks
    private PositionSlotService slots;

    @Test
    void createsASlotWithARequiredRoleFromTheSamePlan() {
        UUID planId = UUID.randomUUID();
        UUID staffingShiftId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        when(staffingShiftService.requireShift(staffingShiftId)).thenReturn(shift(staffingShiftId, planId));
        when(positionSlotRepository.existsByStaffingShiftIdAndNameIgnoreCase(staffingShiftId, "Door")).thenReturn(false);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role(roleId, planId)));
        when(clock.instant()).thenReturn(Instant.parse("2026-05-01T12:00:00Z"));
        when(positionSlotRepository.save(any(PositionSlotEntity.class))).thenAnswer(invocation -> {
            PositionSlotEntity slot = invocation.getArgument(0);
            slot.setId(slotId);
            return slot;
        });

        PositionSlotDetails created = slots.create(staffingShiftId, new PositionSlotData(" Door ", "Entry", 2, roleId, true));

        assertEquals(slotId, created.id());
        assertEquals("Door", created.name());
        assertEquals(roleId, created.requiredRoleId());
        assertEquals(true, created.skipAutoAssignment());
        verify(planAccess).requirePlanner(planId);
    }

    @Test
    void rejectsARoleFromAnotherPlanAndAnInvalidCapacity() {
        UUID planId = UUID.randomUUID();
        UUID staffingShiftId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(staffingShiftService.requireShift(staffingShiftId)).thenReturn(shift(staffingShiftId, planId));
        when(positionSlotRepository.existsByStaffingShiftIdAndNameIgnoreCase(staffingShiftId, "Door")).thenReturn(false);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role(roleId, UUID.randomUUID())));

        assertThrows(ShiftValidationException.class,
            () -> slots.create(staffingShiftId, new PositionSlotData("Door", null, 1, roleId, false)));
        assertThrows(ShiftValidationException.class, () -> new PositionSlotData("Door", null, 0, null, false));
    }

    @Test
    void cannotReduceCapacityBelowOccupiedAssignments() {
        UUID planId = UUID.randomUUID();
        UUID staffingShiftId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setId(slotId);
        slot.setStaffingShiftId(staffingShiftId);
        slot.setName("Door");
        slot.setDesiredVolunteerCount(2);
        when(positionSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
        when(staffingShiftService.requireShift(staffingShiftId)).thenReturn(shift(staffingShiftId, planId));
        when(assignmentRepository.countByPositionSlotIdAndStateIn(eq(slotId), any())).thenReturn(2L);

        assertThrows(ShiftConflictException.class,
            () -> slots.update(slotId, new PositionSlotData("Door", null, 1, null, false)));
    }

    private static StaffingShiftEntity shift(UUID shiftId, UUID planId) {
        StaffingShiftEntity shift = new StaffingShiftEntity();
        shift.setId(shiftId);
        shift.setPlanId(planId);
        return shift;
    }

    private static ShiftRoleEntity role(UUID roleId, UUID planId) {
        ShiftRoleEntity role = new ShiftRoleEntity();
        role.setId(roleId);
        role.setPlanId(planId);
        return role;
    }
}
