package at.control.shiftcontrol.shift.dto.shift;

import java.time.Instant;
import java.util.Collection;

import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.shiftcontrol.lib.type.LockStatus;
import at.shiftcontrol.shiftservice.dto.activity.ActivityDto;
import at.shiftcontrol.shiftservice.dto.location.LocationDto;
import at.shiftcontrol.shiftservice.dto.positionslot.PositionSlotDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftDto {
    @NotNull
    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    @Size(max = 255)
    private String shortDescription;

    @Size(max = 1024)
    private String longDescription;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    @Valid
    private ActivityDto relatedActivity;

    @NotNull
    @Valid
    private ShiftPlanDto shiftPlan;

    @NotNull
    @Valid
    private Collection<PositionSlotDto> positionSlots;

    @Valid
    private LocationDto location;

    @NotNull
    private LockStatus lockStatus;

    @NotNull
    @Min(0)
    private int bonusRewardPoints;
}
