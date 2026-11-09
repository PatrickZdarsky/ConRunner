package at.control.shiftcontrol.shift.dto.trade;

import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.shiftcontrol.shiftservice.dto.userprofile.VolunteerDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeCreateDto {
    @NotNull
    private String offeredPositionSlotId;

    @NotNull
    private String requestedPositionSlotId;

    @NotNull
    @Valid
    private Collection<String> requestedVolunteerIds;
}
