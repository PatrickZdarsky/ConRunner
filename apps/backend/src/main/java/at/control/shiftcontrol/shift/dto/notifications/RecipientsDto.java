package at.control.shiftcontrol.shift.dto.notifications;

import at.shiftcontrol.shiftservice.dto.userprofile.AccountInfoDto;

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
