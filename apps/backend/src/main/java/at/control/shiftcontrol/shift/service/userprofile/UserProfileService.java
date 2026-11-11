package at.control.shiftcontrol.shift.service.userprofile;

import java.util.UUID;
import at.control.shiftcontrol.shift.dto.userprofile.UserProfileDto;

public interface UserProfileService {
    UserProfileDto getUserProfile(UUID userId);
}
