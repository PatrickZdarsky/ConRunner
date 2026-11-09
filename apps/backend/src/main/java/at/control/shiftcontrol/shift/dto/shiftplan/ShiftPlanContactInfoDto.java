package at.control.shiftcontrol.shift.dto.shiftplan;

import java.util.Collection;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.shiftcontrol.shiftservice.dto.user.ContactInfoDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftPlanContactInfoDto {
    @NotNull
    private String planId;

    @NotNull
    private String planName;

    @NotNull
    private Collection<ContactInfoDto> contacts;
}
