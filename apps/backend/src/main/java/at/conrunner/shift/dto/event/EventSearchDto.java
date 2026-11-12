package at.conrunner.shift.dto.event;

import java.time.Instant;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSearchDto {
    @Size(max = 50)
    private String name;

    private Instant startTime;

    private Instant endTime;
}
