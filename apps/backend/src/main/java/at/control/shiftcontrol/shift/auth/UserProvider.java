package at.control.shiftcontrol.shift.auth;

import java.util.Optional;

import lombok.NonNull;

@Deprecated
public interface UserProvider {
    @NonNull
    ApplicationUser getApplicationUser();

    Optional<ApplicationUser> getNullableApplicationUser();
}
