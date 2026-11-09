package at.control.shiftcontrol.shift.dto.location;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationSearchDto {
    @NotNull
    @Size(max = 50)
    private String name;

    @NotNull
    private boolean readOnly;
}
