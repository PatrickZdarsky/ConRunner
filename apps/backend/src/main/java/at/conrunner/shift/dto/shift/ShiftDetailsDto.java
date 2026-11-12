package at.conrunner.shift.dto.shift;

import at.conrunner.shift.dto.event.EventDto;

import at.conrunner.shift.dto.shiftplan.ShiftPlanDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import at.conrunner.shift.dto.positionslot.PositionSlotPreferenceDto;

@Data
@Builder
public class ShiftDetailsDto {
    @NotNull
    @Valid
    private ShiftDto shift;

    @NotNull
    @Valid
    private EventDto event;

    @NotNull
    @Valid
    private ShiftPlanDto shiftPlan;

    // Optional preference for automatic shift planning.
    private PositionSlotPreferenceDto preference;
}
