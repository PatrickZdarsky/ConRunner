package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;
import java.util.List;

import lombok.NoArgsConstructor;

import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanModificationDto;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ShiftPlanMapper {
    public static ShiftPlanDto toShiftPlanDto(ShiftPlan shiftPlan) {
        return ShiftPlanDto.builder()
            .id(String.valueOf(shiftPlan.getId()))
            .name(shiftPlan.getName())
            .shortDescription(shiftPlan.getShortDescription())
            .longDescription(shiftPlan.getLongDescription())
            .lockStatus(shiftPlan.getLockStatus())
            .defaultNoRolePointsPerMinute(shiftPlan.getDefaultNoRolePointsPerMinute())
            .build();
    }

    public static List<ShiftPlanDto> toShiftPlanDto(Collection<ShiftPlan> shiftPlans) {
        return shiftPlans.stream()
            .map(ShiftPlanMapper::toShiftPlanDto)
            .toList();
    }

    public static ShiftPlan toShiftPlan(ShiftPlanModificationDto shiftPlan) {
        var plan = new ShiftPlan();
        updateShiftPlan(shiftPlan, plan);
        return plan;
    }

    public static void updateShiftPlan(ShiftPlanModificationDto shiftPlanDto, ShiftPlan shiftPlan) {
        shiftPlan.setName(shiftPlanDto.getName());
        shiftPlan.setShortDescription(shiftPlanDto.getShortDescription());
        shiftPlan.setLongDescription(shiftPlanDto.getLongDescription());
        shiftPlan.setDefaultNoRolePointsPerMinute(shiftPlanDto.getDefaultNoRolePointsPerMinute());
    }
}
