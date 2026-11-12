package at.conrunner.shift.dto.trade;

import java.time.Instant;
import java.util.Collection;

import at.conrunner.shift.dto.positionslot.PositionSlotDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.conrunner.shift.dto.userprofile.VolunteerDto;

import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeCandidatesDto {
    @NotNull
    private PositionSlotDto ownPosition;

    @NonNull
    private String ownShiftName;

    @NotNull
    private Instant ownShiftStartTime;

    @NotNull
    @Valid
    private Collection<VolunteerDto> eligibleTradeRecipients;
}
