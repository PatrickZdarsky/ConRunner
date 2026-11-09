package at.control.shiftcontrol.shift.service.userdirectory.impl;

import java.util.Collection;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.control.shiftcontrol.shift.dto.PaginationDto;
import at.control.shiftcontrol.shift.dto.user.ContactInfoDto;
import at.control.shiftcontrol.shift.dto.user.UserSearchDto;
import at.control.shiftcontrol.shift.dto.userprofile.AccountInfoDto;
import at.control.shiftcontrol.shift.mapper.AccountInfoMapper;
import at.control.shiftcontrol.shift.mapper.PaginationMapper;
import at.control.shiftcontrol.shift.mapper.UserAssemblingMapper;
import at.control.shiftcontrol.shift.service.userdirectory.InternalUserDirectoryReadService;
import at.control.shiftcontrol.shift.userdirectory.UserInfo;
import at.control.shiftcontrol.shift.userdirectory.UserLookupService;

@Service
@RequiredArgsConstructor
public class InternalUserDirectoryReadServiceImpl implements InternalUserDirectoryReadService {
    private final UserLookupService userDirectoryService;

    @Override
    public @NonNull AccountInfoDto getUser(@NonNull String userId) {
        return AccountInfoMapper.toDto(userDirectoryService.getUserById(userId));
    }

    @Override
    public @NonNull Collection<AccountInfoDto> getUsers(@NonNull Collection<String> userIds) {
        return userDirectoryService.getUserByIds(userIds).stream()
            .map(AccountInfoMapper::toDto)
            .toList();
    }

    @Override
    public @NonNull Collection<ContactInfoDto> getContacts(@NonNull Collection<String> userIds) {
        return userDirectoryService.getUserByIds(userIds).stream()
            .map(UserAssemblingMapper::toContactInfoDto)
            .toList();
    }

    @Override
    public @NonNull PaginationDto<AccountInfoDto> searchUsers(int page, int size, @NonNull UserSearchDto searchDto) {
        PaginationDto<UserInfo> users = userDirectoryService.searchUsers(page, size, searchDto);
        return PaginationMapper.toPaginationDto(
            size,
            page,
            users.getTotal(),
            users.getItems().stream().map(AccountInfoMapper::toDto).toList()
        );
    }
}
