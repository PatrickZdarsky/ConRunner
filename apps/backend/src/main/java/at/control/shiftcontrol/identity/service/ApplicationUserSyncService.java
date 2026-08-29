package at.control.shiftcontrol.identity.service;

import at.control.shiftcontrol.identity.ApplicationUser;
import at.control.shiftcontrol.identity.auth.JwtTokenDecoder;
import lombok.NonNull;

public interface ApplicationUserSyncService {

    @NonNull
    ApplicationUser constructUserForLogin(@NonNull JwtTokenDecoder.DecodedJwtToken jwtToken);
}
