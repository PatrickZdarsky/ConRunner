package at.conrunner.shift.dto.trade;

import java.time.Instant;

import at.conrunner.shift.dto.assignment.AssignmentContextDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.conrunner.shift.lib.type.TradeStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeInfoDto {
    @NotNull
    private String id;

    @NotNull
    private int offeredPositionSlotRewardPoints;

    @NotNull
    private int requestedPositionSlotRewardPoints;

    @NotNull
    @Valid
    private AssignmentContextDto offeringAssignment;

    @NotNull
    @Valid
    private AssignmentContextDto requestedAssignment;

    @NotNull
    private TradeStatus status;

    @NotNull
    private Instant createdAt;
}
