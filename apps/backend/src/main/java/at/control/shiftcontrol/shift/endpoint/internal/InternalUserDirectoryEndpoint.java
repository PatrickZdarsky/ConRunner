package at.control.shiftcontrol.shift.endpoint.internal;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.dto.PaginationDto;
import at.shiftcontrol.shiftservice.annotation.InternalUserReadOnly;
import at.shiftcontrol.shiftservice.dto.user.ContactInfoDto;
import at.shiftcontrol.shiftservice.dto.user.UserSearchDto;
import at.shiftcontrol.shiftservice.dto.userdirectory.InternalUserLookupRequestDto;
import at.shiftcontrol.shiftservice.dto.userprofile.AccountInfoDto;
import at.shiftcontrol.shiftservice.service.userdirectory.InternalUserDirectoryReadService;

@Tag(name = "internal-user-directory-endpoint")
@RestController
@RequestMapping(value = "api/internal/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@InternalUserReadOnly
public class InternalUserDirectoryEndpoint {
    private final InternalUserDirectoryReadService service;

    @GetMapping("/{userId}")
    @Operation(
        operationId = "getInternalUser",
        description = "Resolve a single known user from shiftservice's local user directory"
    )
    public AccountInfoDto getUser(@PathVariable String userId) {
        return service.getUser(userId);
    }

    @PostMapping("/batch")
    @Operation(
        operationId = "getInternalUsers",
        description = "Resolve multiple known users from shiftservice's local user directory"
    )
    public Collection<AccountInfoDto> getUsers(@RequestBody @Valid InternalUserLookupRequestDto request) {
        return service.getUsers(request.getUserIds());
    }

    @PostMapping("/contacts")
    @Operation(
        operationId = "getInternalUserContacts",
        description = "Resolve bulk contact data for known users"
    )
    public Collection<ContactInfoDto> getContacts(@RequestBody @Valid InternalUserLookupRequestDto request) {
        return service.getContacts(request.getUserIds());
    }

    @GetMapping
    @Operation(
        operationId = "searchInternalUsers",
        description = "Search known local users for internal service consumers"
    )
    public PaginationDto<AccountInfoDto> searchUsers(
        @RequestParam int page,
        @RequestParam int size,
        @RequestParam(required = false) String name
    ) {
        var searchDto = UserSearchDto.builder().name(name).build();
        return service.searchUsers(page, size, searchDto);
    }
}
