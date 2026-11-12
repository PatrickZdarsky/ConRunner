package at.conrunner.shift.dto.trade;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.conrunner.shift.lib.type.TradeStatus;
import at.conrunner.shift.dto.assignment.AssignmentDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeDto {
    @NotNull
    private String id;

    @NotNull
    @Valid
    private AssignmentDto offeringAssignment;

    @NotNull
    @Valid
    private AssignmentDto requestedAssignment;

    @NotNull
    private TradeStatus status;

    @NotNull
    private Instant createdAt;
}
