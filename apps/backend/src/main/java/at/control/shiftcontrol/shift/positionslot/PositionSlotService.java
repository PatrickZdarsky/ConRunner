package at.control.shiftcontrol.shift.positionslot;

import java.time.Clock;
import java.util.Collection;
import java.util.UUID;

import at.control.shiftcontrol.shift.error.ShiftConflictException;
import at.control.shiftcontrol.shift.error.ShiftNotFoundException;
import at.control.shiftcontrol.shift.error.ShiftValidationException;
import at.control.shiftcontrol.shift.plan.PlanAccess;
import at.control.shiftcontrol.shift.plan.ShiftRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PositionSlotService {
    private final PositionSlotRepository positionSlotRepository;
    private final StaffingShiftService staffingShiftService;
    private final ShiftRoleRepository roleRepository;
    private final PlanAccess planAccess;
    private final Clock clock;

    @Transactional
    public PositionSlotDetails create(UUID staffingShiftId, PositionSlotData data) {
        StaffingShiftEntity shift = staffingShiftService.requireShift(staffingShiftId);
        planAccess.requirePlanner(shift.getPlanId());
        assertUniqueName(staffingShiftId, data.name(), null);
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setStaffingShiftId(staffingShiftId);
        slot.setCreatedAt(clock.instant());
        apply(data, slot, shift);
        return toDetails(positionSlotRepository.save(slot));
    }

    public PositionSlotDetails get(UUID positionSlotId) {
        PositionSlotEntity slot = requireSlot(positionSlotId);
        StaffingShiftEntity shift = staffingShiftService.requireShift(slot.getStaffingShiftId());
        planAccess.requireMember(shift.getPlanId());
        return toDetails(slot);
    }

    public Collection<PositionSlotDetails> listForShift(UUID staffingShiftId) {
        StaffingShiftEntity shift = staffingShiftService.requireShift(staffingShiftId);
        planAccess.requirePlanner(shift.getPlanId());
        return positionSlotRepository.findAllByStaffingShiftIdOrderByNameAsc(staffingShiftId).stream()
            .map(PositionSlotService::toDetails)
            .toList();
    }

    @Transactional
    public PositionSlotDetails update(UUID positionSlotId, PositionSlotData data) {
        PositionSlotEntity slot = requireSlot(positionSlotId);
        StaffingShiftEntity shift = staffingShiftService.requireShift(slot.getStaffingShiftId());
        planAccess.requirePlanner(shift.getPlanId());
        assertUniqueName(slot.getStaffingShiftId(), data.name(), slot);
        apply(data, slot, shift);
        return toDetails(positionSlotRepository.save(slot));
    }

    PositionSlotEntity requireSlot(UUID positionSlotId) {
        return positionSlotRepository.findById(positionSlotId)
            .orElseThrow(() -> new ShiftNotFoundException("Position slot not found: " + positionSlotId));
    }

    private void assertUniqueName(UUID staffingShiftId, String name, PositionSlotEntity current) {
        if (current == null || !current.getName().equalsIgnoreCase(name)) {
            if (positionSlotRepository.existsByStaffingShiftIdAndNameIgnoreCase(staffingShiftId, name)) {
                throw new ShiftConflictException("A position slot with this name already exists in the staffing shift");
            }
        }
    }

    private void apply(PositionSlotData data, PositionSlotEntity slot, StaffingShiftEntity shift) {
        if (data.requiredRoleId() != null) {
            var role = roleRepository.findById(data.requiredRoleId())
                .orElseThrow(() -> new ShiftValidationException("Required role not found"));
            if (!shift.getPlanId().equals(role.getPlanId())) {
                throw new ShiftValidationException("Required role must belong to the same shift plan");
            }
        }
        slot.setName(data.name());
        slot.setDescription(data.description());
        slot.setDesiredVolunteerCount(data.desiredVolunteerCount());
        slot.setRequiredRoleId(data.requiredRoleId());
        slot.setSkipAutoAssignment(data.skipAutoAssignment());
    }

    private static PositionSlotDetails toDetails(PositionSlotEntity slot) {
        return new PositionSlotDetails(slot.getId(), slot.getStaffingShiftId(), slot.getName(), slot.getDescription(),
            slot.getDesiredVolunteerCount(), slot.getRequiredRoleId(), slot.isSkipAutoAssignment());
    }
}
