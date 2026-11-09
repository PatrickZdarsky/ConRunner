package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;

import at.shiftcontrol.lib.entity.Shift;
import at.shiftcontrol.shiftservice.dto.positionslot.PositionSlotDto;
import at.shiftcontrol.shiftservice.dto.shift.ShiftContextDto;
import at.shiftcontrol.shiftservice.dto.shift.ShiftDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ShiftAssemblingMapper {
    private final PositionSlotAssemblingMapper positionSlotAssemblingMapper;

    public ShiftDto assemble(@NonNull Shift shift) {
        var positionSlots = shift.getSlots() == null ? null : positionSlotAssemblingMapper.assemble(shift.getSlots());
        return toDto(shift, positionSlots);
    }

    public Collection<ShiftDto> assemble(@NonNull Collection<Shift> shifts) {
        return shifts.stream().map(this::assemble).toList();
    }

    public static ShiftDto toDto(@NonNull Shift shift, Collection<PositionSlotDto> positionSlots) {
        var relatedActivity = shift.getRelatedActivity();
        var location = shift.getLocation();

        return new ShiftDto(
            String.valueOf(shift.getId()),
            shift.getName(),
            shift.getShortDescription(),
            shift.getLongDescription(),
            shift.getStartTime(),
            shift.getEndTime(),
            relatedActivity == null ? null : ActivityMapper.toActivityDto(relatedActivity),
            ShiftPlanMapper.toShiftPlanDto(shift.getShiftPlan()),
            positionSlots,
            location == null ? null : LocationMapper.toLocationDto(location),
            shift.getShiftPlan().getLockStatus(),
            shift.getBonusRewardPoints()
        );
    }

    public static ShiftContextDto toContextDto(Shift shift) {
        var location = shift.getLocation();
        return new ShiftContextDto(
            String.valueOf(shift.getId()),
            shift.getName(),
            shift.getShortDescription(),
            shift.getLongDescription(),
            shift.getStartTime(),
            shift.getEndTime(),
            location == null ? null : LocationMapper.toLocationDto(location),
            shift.getBonusRewardPoints()
        );
    }
}
