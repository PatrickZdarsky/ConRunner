package at.control.shiftcontrol.shift.dto.shift;

import at.shiftcontrol.shiftservice.dto.event.EventDto;

import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import at.shiftcontrol.shiftservice.dto.positionslot.PositionSlotPreferenceDto;

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

    /* TODO NOT IN USE */
    /*@NotNull
    @Valid*/
    private PositionSlotPreferenceDto preference;
    // TODO additional info needed for trade/auction?
}
