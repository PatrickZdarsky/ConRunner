package at.control.shiftcontrol.shift.dto.activity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivitySuggestionDto {
    @Valid
    private ActivityTimeFilterDto timeFilter;

    @Size(max = 50)
    private String name;
}
