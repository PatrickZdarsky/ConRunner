package at.conrunner.shift.service;

import java.util.UUID;
import java.util.Collection;
import jakarta.validation.Valid;
import lombok.NonNull;

import at.conrunner.shift.dto.PaginationDto;
import at.conrunner.shift.dto.user.UserEventBulkDto;
import at.conrunner.shift.dto.user.UserEventDto;
import at.conrunner.shift.dto.user.UserEventUpdateDto;
import at.conrunner.shift.dto.user.UserPlanBulkDto;
import at.conrunner.shift.dto.user.UserPlanDto;
import at.conrunner.shift.dto.user.UserPlanUpdateDto;
import at.conrunner.shift.dto.user.UserSearchDto;

public interface UserAdministrationService {
    @NonNull PaginationDto<UserEventDto> getAllUsers(int page, int size, @NonNull UserSearchDto searchDt);

    @NonNull PaginationDto<UserPlanDto> getAllPlanUsers(@NonNull Long shiftPlanId, int page, int size, @NonNull UserSearchDto searchDto);

    @NonNull UserEventDto getUser(@NonNull UUID userId);

    @NonNull UserPlanDto getPlanUser(@NonNull Long shiftPlanId, @NonNull UUID userId);

    @NonNull UserEventDto updateEventUser(@NonNull UUID userId, @NonNull UserEventUpdateDto updateDto);

    @NonNull UserPlanDto updatePlanUser(@NonNull Long shiftPlanId, @NonNull UUID userId, @NonNull UserPlanUpdateDto updateDto);

    @NonNull UserEventDto lockUser(@NonNull UUID userId, @NonNull Collection<Long> shiftPlanId);

    @NonNull UserEventDto unLockUser(@NonNull UUID userId, @NonNull Collection<Long> shiftPlanId);

    @NonNull UserEventDto resetUser(@NonNull UUID userId, @NonNull Collection<Long> shiftPlanId);

    @NonNull Collection<UserPlanDto> bulkAddRoles(long shiftPlanId, @Valid UserPlanBulkDto updateDto);

    @NonNull Collection<UserPlanDto> bulkRemoveRoles(long shiftPlanId, @Valid UserPlanBulkDto updateDto);

    @NonNull Collection<UserEventDto> bulkAddVolunteeringPlans(@Valid UserEventBulkDto updateDto);

    @NonNull Collection<UserEventDto> bulkRemoveVolunteeringPlans(@Valid UserEventBulkDto updateDto);
}
