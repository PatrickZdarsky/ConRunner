package at.conrunner.shift.endpoint.internal;

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

import at.conrunner.shift.dto.PaginationDto;
import at.conrunner.identity.InternalUserReadOnly;
import at.conrunner.shift.dto.user.ContactInfoDto;
import at.conrunner.shift.dto.userdirectory.InternalUserLookupRequestDto;
import at.conrunner.shift.dto.userprofile.AccountInfoDto;
import at.conrunner.identity.UserService;
import at.conrunner.identity.UserSearchRequest;
import java.util.UUID;
import at.conrunner.shift.mapper.AccountInfoMapper;
import at.conrunner.shift.mapper.UserAssemblingMapper;
import at.conrunner.shift.mapper.PaginationMapper;

@Tag(name = "internal-user-directory-endpoint")
@RestController
@RequestMapping(value = "api/internal/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@InternalUserReadOnly
public class InternalUserDirectoryEndpoint {
    private final UserService service;

    @GetMapping("/{userId}")
    @Operation(
        operationId = "getInternalUser",
        description = "Resolve a single known user from the identity service"
    )
    public AccountInfoDto getUser(@PathVariable UUID userId) {
        return AccountInfoMapper.toDto(service.getUser(userId));
    }

    @PostMapping("/batch")
    @Operation(
        operationId = "getInternalUsers",
        description = "Resolve multiple known users from the identity service"
    )
    public Collection<AccountInfoDto> getUsers(@RequestBody @Valid InternalUserLookupRequestDto request) {
        return service.getUsers(request.getUserIds()).stream().map(AccountInfoMapper::toDto).toList();
    }

    @PostMapping("/contacts")
    @Operation(
        operationId = "getInternalUserContacts",
        description = "Resolve bulk contact data for known users"
    )
    public Collection<ContactInfoDto> getContacts(@RequestBody @Valid InternalUserLookupRequestDto request) {
        return service.getUsers(request.getUserIds()).stream().map(UserAssemblingMapper::toContactInfoDto).toList();
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
        var searchDto = UserSearchRequest.builder().name(name).build();
        var result = service.searchUsers(page, size, searchDto);
        return PaginationMapper.toPaginationDto(size, page, result.getTotalElements(), result.getContent().stream().map(AccountInfoMapper::toDto).toList());
    }
}
