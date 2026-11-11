package at.control.shiftcontrol.shift.dto.positionslot;

import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.lib.type.LockStatus;
import at.control.shiftcontrol.shift.lib.type.PositionSignupState;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.RewardPointsDto;
import at.control.shiftcontrol.shift.dto.role.RoleDto;
import at.control.shiftcontrol.shift.dto.trade.TradeInfoDto;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionSlotDto {
    @NotNull
    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull
    private boolean skipAutoAssignment;

    @NotNull
    private String associatedShiftId;

    @Valid
    private RoleDto role;

    @NotNull
    @Valid
    private Collection<AssignmentDto> assignments;

    @NotNull
    @Min(0)
    private int desiredVolunteerCount;

    /**
     * Specific for the current user's signup state for this position slot.
     */
    @NotNull
    private PositionSignupState positionSignupState;

    /**
     * Specific for the current user's offered trades for this position slot.
     */
    @NotNull
    @Valid
    private Collection<TradeInfoDto> offeredTrades;

    /**
     * Specific for the current user's requested trades for this position slot.
     */
    @NotNull
    @Valid
    private Collection<TradeInfoDto> requestedTrades;

    /**
     * Specific for the current user's available auctions of this position slot.
     */
    @NotNull
    @Valid
    private Collection<AssignmentDto> auctions;

    /**
     * Specific for the current user's preference value for this position slot.
     */
    @NotNull
    @Min(-10)
    @Max(10)
    private int preferenceValue;

    @NotNull
    private LockStatus lockStatus;

    @NotNull
    @Valid
    private RewardPointsDto rewardPointsDto;
}
