package at.control.shiftcontrol.shift.dto.user;

import java.util.Collection;
import java.util.Comparator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.shiftcontrol.shiftservice.dto.role.RoleDto;
import at.shiftcontrol.shiftservice.dto.userprofile.VolunteerDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPlanDto {
    @NotNull
    @Valid
    private VolunteerDto volunteer;

    @NotNull
    private String email;

    @NotNull
    private Boolean isLocked;

    @NotNull
    private Collection<RoleDto> roles;

    public static Comparator<UserPlanDto> lastNameComparator() {
        return Comparator.comparing(
            (UserPlanDto udp ) -> udp.getVolunteer().getLastName(),
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        );
    }
}
