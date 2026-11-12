package at.conrunner.identity.service;

import at.conrunner.identity.ApplicationUser;
import at.conrunner.identity.auth.JwtTokenDecoder;
import lombok.NonNull;

public interface ApplicationUserSyncService {

    @NonNull
    ApplicationUser constructUserForLogin(@NonNull JwtTokenDecoder.DecodedJwtToken jwtToken);
}
