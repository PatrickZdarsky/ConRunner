package at.conrunner.shift.endpoint.user;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.conrunner.shift.dto.PaginationDto;
import at.conrunner.shift.dto.user.UserEventBulkDto;
import at.conrunner.shift.dto.user.UserEventDto;
import at.conrunner.shift.dto.user.UserSearchDto;
import at.conrunner.shift.service.UserAdministrationService;

@Tag(
    name = "user-event-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AdminUserAdministrationCollectionEndpoint {
    private final UserAdministrationService service;

    @GetMapping()
    @Operation(
        operationId = "getAllUsers",
        description = "Find app-relevant local users filtered by name. Pending invitees are managed separately via /api/v1/users/invites."
    )
    public PaginationDto<UserEventDto> getAllUsers(
        @RequestParam int page,
        @RequestParam int size,
        @RequestParam(required = false) String name
    ) {
        var searchDto = UserSearchDto.builder().name(name).build();
        return service.getAllUsers(page, size, searchDto);
    }

    @PatchMapping("/bulk/add")
    @Operation(
        operationId = "bulkAddVolunteeringPlans",
        description = "Add a list of volunteering plans to a list of users"
    )
    public Collection<UserEventDto> bulkAddVolunteeringPlans(@RequestBody @Valid UserEventBulkDto updateDto) {
        return service.bulkAddVolunteeringPlans(updateDto);
    }

    @PatchMapping("/bulk/remove")
    @Operation(
        operationId = "bulkRemoveVolunteeringPlans",
        description = "Remove a list of plans from a list of users"
    )
    public Collection<UserEventDto> bulkRemoveVolunteeringPlans(@RequestBody @Valid UserEventBulkDto updateDto) {
        return service.bulkRemoveVolunteeringPlans(updateDto);
    }
}
