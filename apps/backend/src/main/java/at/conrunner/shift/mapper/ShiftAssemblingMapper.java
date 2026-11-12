package at.conrunner.shift.mapper;

import java.util.Collection;

import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.dto.positionslot.PositionSlotDto;
import at.conrunner.shift.dto.shift.ShiftContextDto;
import at.conrunner.shift.dto.shift.ShiftDto;
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
        var location = shift.getEffectiveLocation();

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
        var location = shift.getEffectiveLocation();
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
