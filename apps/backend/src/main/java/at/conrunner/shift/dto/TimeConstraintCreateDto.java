package at.conrunner.shift.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.conrunner.shift.lib.type.TimeConstraintType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeConstraintCreateDto {
    @NotNull
    private TimeConstraintType type;
    @NotNull
    private Instant from;
    @NotNull
    private Instant to;
}
