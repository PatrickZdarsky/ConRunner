package at.control.shiftcontrol.shift.service.userdirectory;

import java.util.Collection;

import org.jspecify.annotations.NonNull;

import at.control.shiftcontrol.shift.dto.PaginationDto;
import at.control.shiftcontrol.shift.dto.user.ContactInfoDto;
import at.control.shiftcontrol.shift.dto.user.UserSearchDto;
import at.control.shiftcontrol.shift.dto.userprofile.AccountInfoDto;

public interface InternalUserDirectoryReadService {
    @NonNull AccountInfoDto getUser(@NonNull String userId);

    @NonNull Collection<AccountInfoDto> getUsers(@NonNull Collection<String> userIds);

    @NonNull Collection<ContactInfoDto> getContacts(@NonNull Collection<String> userIds);

    @NonNull PaginationDto<AccountInfoDto> searchUsers(int page, int size, @NonNull UserSearchDto searchDto);
}
