package at.control.shiftcontrol.shift.service;

import java.util.Collection;
import jakarta.validation.Valid;
import lombok.NonNull;

import at.control.shiftcontrol.shift.dto.PaginationDto;
import at.control.shiftcontrol.shift.dto.user.UserEventBulkDto;
import at.control.shiftcontrol.shift.dto.user.UserEventDto;
import at.control.shiftcontrol.shift.dto.user.UserEventUpdateDto;
import at.control.shiftcontrol.shift.dto.user.UserPlanBulkDto;
import at.control.shiftcontrol.shift.dto.user.UserPlanDto;
import at.control.shiftcontrol.shift.dto.user.UserPlanUpdateDto;
import at.control.shiftcontrol.shift.dto.user.UserSearchDto;

public interface UserAdministrationService {
    @NonNull PaginationDto<UserEventDto> getAllUsers(int page, int size, @NonNull UserSearchDto searchDt);

    @NonNull PaginationDto<UserPlanDto> getAllPlanUsers(@NonNull Long shiftPlanId, int page, int size, @NonNull UserSearchDto searchDto);

    @NonNull UserEventDto getUser(@NonNull String userId);

    @NonNull UserPlanDto getPlanUser(@NonNull Long shiftPlanId, @NonNull String userId);

    @NonNull UserEventDto updateEventUser(@NonNull String userId, @NonNull UserEventUpdateDto updateDto);

    @NonNull UserPlanDto updatePlanUser(@NonNull Long shiftPlanId, @NonNull String userId, @NonNull UserPlanUpdateDto updateDto);

    @NonNull UserEventDto lockUser(@NonNull String userId, @NonNull Collection<Long> shiftPlanId);

    @NonNull UserEventDto unLockUser(@NonNull String userId, @NonNull Collection<Long> shiftPlanId);

    @NonNull UserEventDto resetUser(@NonNull String userId, @NonNull Collection<Long> shiftPlanId);

    @NonNull Collection<UserPlanDto> bulkAddRoles(long shiftPlanId, @Valid UserPlanBulkDto updateDto);

    @NonNull Collection<UserPlanDto> bulkRemoveRoles(long shiftPlanId, @Valid UserPlanBulkDto updateDto);

    @NonNull Collection<UserEventDto> bulkAddVolunteeringPlans(@Valid UserEventBulkDto updateDto);

    @NonNull Collection<UserEventDto> bulkRemoveVolunteeringPlans(@Valid UserEventBulkDto updateDto);
}
