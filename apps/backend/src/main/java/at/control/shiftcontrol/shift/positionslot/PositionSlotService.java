package at.control.shiftcontrol.shift.positionslot;

import java.time.Clock;
import java.util.Collection;
import java.util.UUID;

import at.control.shiftcontrol.shift.ShiftEntity;
import at.control.shiftcontrol.shift.ShiftService;
import at.control.shiftcontrol.shift.assignment.AssignmentEligibilityService;
import at.control.shiftcontrol.shift.assignment.AssignmentRepository;
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
    private final ShiftService shiftService;
    private final ShiftRoleRepository roleRepository;
    private final AssignmentRepository assignmentRepository;
    private final PlanAccess planAccess;
    private final Clock clock;

    @Transactional
    public PositionSlotDetails create(UUID shiftId, PositionSlotData data) {
        ShiftEntity shift = shiftService.requireShift(shiftId);
        planAccess.requirePlanner(shift.getPlanId());
        assertUniqueName(shiftId, data.name(), null);
        PositionSlotEntity slot = new PositionSlotEntity();
        slot.setShiftId(shiftId);
        slot.setCreatedAt(clock.instant());
        apply(data, slot, shift);
        return toDetails(positionSlotRepository.save(slot));
    }

    public PositionSlotDetails get(UUID positionSlotId) {
        PositionSlotEntity slot = requireSlot(positionSlotId);
        ShiftEntity shift = shiftService.requireShift(slot.getShiftId());
        planAccess.requireMember(shift.getPlanId());
        return toDetails(slot);
    }

    public Collection<PositionSlotDetails> listForShift(UUID shiftId) {
        ShiftEntity shift = shiftService.requireShift(shiftId);
        planAccess.requirePlanner(shift.getPlanId());
        return positionSlotRepository.findAllByShiftIdOrderByNameAsc(shiftId).stream().map(PositionSlotService::toDetails).toList();
    }

    @Transactional
    public PositionSlotDetails update(UUID positionSlotId, PositionSlotData data) {
        PositionSlotEntity slot = positionSlotRepository.findByIdForUpdate(positionSlotId)
            .orElseThrow(() -> new ShiftNotFoundException("Position slot not found: " + positionSlotId));
        ShiftEntity shift = shiftService.requireShift(slot.getShiftId());
        planAccess.requirePlanner(shift.getPlanId());
        assertUniqueName(slot.getShiftId(), data.name(), slot);
        long occupied = assignmentRepository.countByPositionSlotIdAndStatusIn(slot.getId(), AssignmentEligibilityService.OCCUPYING_STATUSES);
        if (data.desiredVolunteerCount() < occupied) {
            throw new ShiftConflictException("Position slot capacity cannot be reduced below occupied assignments");
        }
        apply(data, slot, shift);
        return toDetails(positionSlotRepository.save(slot));
    }

    public PositionSlotEntity requireSlot(UUID positionSlotId) {
        return positionSlotRepository.findById(positionSlotId)
            .orElseThrow(() -> new ShiftNotFoundException("Position slot not found: " + positionSlotId));
    }

    private void assertUniqueName(UUID shiftId, String name, PositionSlotEntity current) {
        if (current == null || !current.getName().equalsIgnoreCase(name)) {
            if (positionSlotRepository.existsByShiftIdAndNameIgnoreCase(shiftId, name)) {
                throw new ShiftConflictException("A position slot with this name already exists in the shift");
            }
        }
    }

    private void apply(PositionSlotData data, PositionSlotEntity slot, ShiftEntity shift) {
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
        return new PositionSlotDetails(slot.getId(), slot.getShiftId(), slot.getName(), slot.getDescription(),
            slot.getDesiredVolunteerCount(), slot.getRequiredRoleId(), slot.isSkipAutoAssignment());
    }
}
