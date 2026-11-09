package at.control.shiftcontrol.shift.service.userprofile;

import at.shiftcontrol.shiftservice.dto.userprofile.UserProfileDto;

public interface UserProfileService {
    UserProfileDto getUserProfile(String userId);
}
