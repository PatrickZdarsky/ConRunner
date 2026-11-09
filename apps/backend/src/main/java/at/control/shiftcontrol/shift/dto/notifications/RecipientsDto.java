package at.control.shiftcontrol.shift.dto.notifications;

import at.control.shiftcontrol.shift.dto.userprofile.AccountInfoDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Data
@Builder
public class RecipientsDto {
    @NotNull
    private Set<AccountInfoDto> recipients;
}
