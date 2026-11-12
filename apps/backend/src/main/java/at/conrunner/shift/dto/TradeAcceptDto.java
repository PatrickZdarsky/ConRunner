package at.conrunner.shift.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeAcceptDto {
    @NotNull
    private String offeredSlot;

    @NotNull
    private String requestedSlot;

    @NotNull
    private String offeringVolunteer;
}
