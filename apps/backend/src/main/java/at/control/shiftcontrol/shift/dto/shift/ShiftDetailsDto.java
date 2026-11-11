package at.control.shiftcontrol.shift.dto.shift;

import at.control.shiftcontrol.shift.dto.event.EventDto;

import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotPreferenceDto;

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
