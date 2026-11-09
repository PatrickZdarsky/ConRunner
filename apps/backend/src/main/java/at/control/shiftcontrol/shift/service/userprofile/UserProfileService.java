package at.control.shiftcontrol.shift.service.userprofile;

import at.control.shiftcontrol.shift.dto.userprofile.UserProfileDto;

public interface UserProfileService {
    UserProfileDto getUserProfile(String userId);
}
