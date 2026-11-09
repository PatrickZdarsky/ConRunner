package at.control.shiftcontrol.shift.dto.userprofile;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarShareIdDto {
    @NotNull
    private String calendarShareId;
}
