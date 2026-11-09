package at.control.shiftcontrol.shift.userdirectory;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.NonNull;

import at.shiftcontrol.lib.dto.PaginationDto;
import at.shiftcontrol.shiftservice.dto.user.UserSearchDto;

public interface UserLookupService {
    UserInfo getUserById(String userId);

    Collection<UserInfo> getUserByIds(Collection<String> userIds);

    List<UserInfo> getAllUsers();

    List<UserInfo> getAllAdmins();

    @NonNull PaginationDto<UserInfo> searchUsers(int page, int size, @NonNull UserSearchDto searchDto);
}
