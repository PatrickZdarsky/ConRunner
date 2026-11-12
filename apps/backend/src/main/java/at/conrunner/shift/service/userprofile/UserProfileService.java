package at.conrunner.shift.service.userprofile;

import java.util.UUID;
import at.conrunner.shift.dto.userprofile.UserProfileDto;

public interface UserProfileService {
    UserProfileDto getUserProfile(UUID userId);
}
