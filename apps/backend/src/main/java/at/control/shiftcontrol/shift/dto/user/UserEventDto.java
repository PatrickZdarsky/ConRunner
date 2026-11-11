package at.control.shiftcontrol.shift.dto.user;

import java.util.Collection;
import java.util.Comparator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEventDto {
    @NotNull
    @Valid
    private VolunteerDto volunteer;

    @NotNull
    private String email;

    @NotNull
    private Collection<String> planningPlans;

    @NotNull
    private Collection<String> lockedPlans;

    @NotNull
    private Collection<String> volunteeringPlans;

    public static Comparator<UserEventDto> nameComparator() {
        return Comparator.comparing(
            (UserEventDto udp ) -> udp.getVolunteer().getName(),
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        );
    }
}
