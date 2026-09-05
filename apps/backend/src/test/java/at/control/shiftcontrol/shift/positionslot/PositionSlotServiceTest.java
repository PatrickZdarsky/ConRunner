package at.control.shiftcontrol.shift.positionslot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import at.control.shiftcontrol.shift.ShiftEntity;
import at.control.shiftcontrol.shift.ShiftService;
import at.control.shiftcontrol.shift.assignment.AssignmentRepository;
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
    @Mock private PositionSlotRepository positionSlotRepository;
    @Mock private ShiftService shiftService;
    @Mock private ShiftRoleRepository roleRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private PlanAccess planAccess;
    @Mock private Clock clock;
    @InjectMocks private PositionSlotService slots;

    @Test
    void createsSlotAttachedToTheShift() {
        UUID planId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        ShiftEntity shift = new ShiftEntity();
        shift.setId(shiftId);
        shift.setPlanId(planId);
        ShiftRoleEntity role = new ShiftRoleEntity();
        role.setId(roleId);
        role.setPlanId(planId);
        when(shiftService.requireShift(shiftId)).thenReturn(shift);
        when(positionSlotRepository.existsByShiftIdAndNameIgnoreCase(shiftId, "Door")).thenReturn(false);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(clock.instant()).thenReturn(Instant.parse("2026-05-01T10:00:00Z"));
        when(positionSlotRepository.save(any(PositionSlotEntity.class))).thenAnswer(invocation -> {
            PositionSlotEntity slot = invocation.getArgument(0);
            slot.setId(UUID.randomUUID());
            return slot;
        });

        PositionSlotDetails result = slots.create(shiftId, new PositionSlotData("Door", null, 2, roleId, false));

        assertEquals(shiftId, result.shiftId());
        assertEquals(roleId, result.requiredRoleId());
    }

    @Test
    void rejectsRoleFromAnotherPlanAndInvalidCapacity() {
        UUID planId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        ShiftEntity shift = new ShiftEntity();
        shift.setPlanId(planId);
        ShiftRoleEntity role = new ShiftRoleEntity();
        role.setPlanId(UUID.randomUUID());
        when(shiftService.requireShift(shiftId)).thenReturn(shift);
        when(positionSlotRepository.existsByShiftIdAndNameIgnoreCase(shiftId, "Door")).thenReturn(false);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThrows(ShiftValidationException.class,
            () -> slots.create(shiftId, new PositionSlotData("Door", null, 1, roleId, false)));
        assertThrows(ShiftValidationException.class, () -> new PositionSlotData("Door", null, 0, null, false));
    }

    @Test
    void cannotReduceCapacityBelowOccupiedAssignments() {
        UUID planId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ShiftEntity shift = new ShiftEntity();
        shift.setPlanId(planId);
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setId(slotId);
        slot.setShiftId(shiftId);
        slot.setName("Door");
        slot.setDesiredVolunteerCount(2);
        when(positionSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
        when(shiftService.requireShift(shiftId)).thenReturn(shift);
        when(assignmentRepository.countByPositionSlotIdAndStatusIn(org.mockito.ArgumentMatchers.eq(slotId), any())).thenReturn(2L);

        assertThrows(ShiftConflictException.class,
            () -> slots.update(slotId, new PositionSlotData("Door", null, 1, null, false)));
    }
}
