package at.control.shiftcontrol.shift.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.shiftcontrol.lib.dto.PaginationDto;
import at.shiftcontrol.lib.entity.Role;
import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.lib.event.events.UserEvent;
import at.shiftcontrol.lib.event.events.UserEventBulkEvent;
import at.shiftcontrol.lib.event.events.UserPlanBulkEvent;
import at.shiftcontrol.lib.exception.BadRequestException;
import at.shiftcontrol.lib.util.ConvertUtil;
import at.shiftcontrol.shiftservice.annotation.AdminOnly;
import at.shiftcontrol.shiftservice.auth.UserAttributeProvider;
import at.shiftcontrol.shiftservice.dao.AssignmentSwitchRequestDao;
import at.shiftcontrol.shiftservice.dao.ShiftPlanDao;
import at.shiftcontrol.shiftservice.dao.role.RoleDao;
import at.shiftcontrol.shiftservice.dao.userprofile.VolunteerDao;
import at.shiftcontrol.shiftservice.dto.user.UserEventBulkDto;
import at.shiftcontrol.shiftservice.dto.user.UserEventDto;
import at.shiftcontrol.shiftservice.dto.user.UserEventUpdateDto;
import at.shiftcontrol.shiftservice.dto.user.UserPlanBulkDto;
import at.shiftcontrol.shiftservice.dto.user.UserPlanDto;
import at.shiftcontrol.shiftservice.dto.user.UserPlanUpdateDto;
import at.shiftcontrol.shiftservice.dto.user.UserSearchDto;
import at.shiftcontrol.shiftservice.mapper.PaginationMapper;
import at.shiftcontrol.shiftservice.mapper.UserAssemblingMapper;
import at.shiftcontrol.shiftservice.repo.AssignmentRepository;
import at.shiftcontrol.shiftservice.service.AssignmentService;
import at.shiftcontrol.shiftservice.service.UserAdministrationService;
import at.shiftcontrol.shiftservice.userdirectory.UserInfo;
import at.shiftcontrol.shiftservice.userdirectory.UserLookupService;
import at.shiftcontrol.shiftservice.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class UserAdministrationServiceImpl implements UserAdministrationService {
    private final VolunteerDao volunteerDao;
    private final ShiftPlanDao shiftPlanDao;
    private final UserLookupService userDirectoryService;
    private final UserAttributeProvider userAttributeProvider;
    private final SecurityHelper securityHelper;
    private final RoleDao roleDao;
    private final ApplicationEventPublisher publisher;
    private final AssignmentService assignmentService;
    private final AssignmentSwitchRequestDao assignmentSwitchRequestDao;
    private final AssignmentRepository assignmentRepository;

    @Override
    @AdminOnly
    public @NonNull PaginationDto<UserEventDto> getAllUsers(int page, int size, @NonNull UserSearchDto searchDto) {
        var users = userDirectoryService.searchUsers(page, size, searchDto);
        var volunteers = volunteerDao.findAllByVolunteerIds(
            users.getItems().stream().map(UserInfo::id).toList()
        );

        return PaginationMapper.toPaginationDto(size, page, users.getTotal(), UserAssemblingMapper.toUserEventDtoForUsers(volunteers, users.getItems()));
    }

    @Override
    public @NonNull PaginationDto<UserPlanDto> getAllPlanUsers(@NonNull Long shiftPlanId, int page, int size, @NonNull UserSearchDto searchDto) {
        securityHelper.assertUserIsPlanner(shiftPlanId);
        var volunteersInPlanId = volunteerDao.findAllIdsByShiftPlan(shiftPlanId);
        var usersForPlan = userDirectoryService.getUserByIds(volunteersInPlanId).stream()
            .sorted(Comparator.comparing(user -> user.lastName() == null ? "" : user.lastName()))
            .toList();
        var users = filterUsers(searchDto, usersForPlan);
        var paginatedUsers = paginateUsers(users, page, size);
        var volunteers = volunteerDao.findAllByVolunteerIds(
            paginatedUsers.stream().map(UserInfo::id).toList()
        );

        return PaginationMapper.toPaginationDto(size, page, users.size(), UserAssemblingMapper.toUserPlanDto(volunteers, paginatedUsers, shiftPlanId));
    }

    private List<UserInfo> paginateUsers(List<UserInfo> users, int page, int size) {
        int fromIndex = page * size;
        if (fromIndex >= users.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + size, users.size());
        return users.subList(fromIndex, toIndex);
    }

    private static List<UserInfo> filterUsers(UserSearchDto searchDto, List<UserInfo> users) {
        if (searchDto.getName() == null || searchDto.getName().isEmpty()) {
            return users;
        }
        var nameLower = searchDto.getName().toLowerCase().trim();
        return users.stream().filter(x ->
                safeLower(x.username()).contains(nameLower)
                    || safeLower(x.firstName()).contains(nameLower)
                    || safeLower(x.lastName()).contains(nameLower)
            )
            .toList();
    }

    @Override
    @AdminOnly
    public @NonNull UserEventDto getUser(@NonNull String userId) {
        var volunteer = volunteerDao.findById(userId).orElse(null);
        var user = userDirectoryService.getUserById(userId);
        return UserAssemblingMapper.toUserEventDto(volunteer, user);
    }

    @Override
    public @NonNull UserPlanDto getPlanUser(@NonNull Long shiftPlanId, @NonNull String userId) {
        var volunteer = volunteerDao.getById(userId);
        securityHelper.assertUserIsPlanner(shiftPlanId);
        securityHelper.assertVolunteerIsVolunteer(shiftPlanDao.getById(shiftPlanId), volunteer, false);
        return toUserPlanDto(volunteer, shiftPlanId);
    }

    @Override
    @Transactional
    @AdminOnly
    public @NonNull UserEventDto updateEventUser(@NonNull String userId, @NonNull UserEventUpdateDto updateDto) {
        var volunteer = volunteerDao.getById(userId);
        assertPlansChanged(volunteer, updateDto);

        var volunteerToAdd = toAddPlans(updateDto.getVolunteeringPlans(), volunteer.getVolunteeringPlans());
        var planningToAdd = toAddPlans(updateDto.getPlanningPlans(), volunteer.getPlanningPlans());
        addPlans(volunteer, volunteerToAdd, planningToAdd);

        var volunteerToRemove = toRemovePlans(updateDto.getVolunteeringPlans(), volunteer.getVolunteeringPlans());
        var planningToRemove = toRemovePlans(updateDto.getPlanningPlans(), volunteer.getPlanningPlans());
        removePlans(volunteer, volunteerToRemove, planningToRemove);

        var user = userDirectoryService.getUserById(volunteer.getId());
        userAttributeProvider.invalidateUserCache(userId);
        var allPlans = shiftPlanDao.getByIds(Stream.concat(
            updateDto.getPlanningPlans().stream(), updateDto.getVolunteeringPlans().stream())
            .map(ConvertUtil::idToLong)
            .collect(Collectors.toSet()));
        publisher.publishEvent(UserEvent.eventUpdate(volunteer, allPlans));
        return UserAssemblingMapper.toUserEventDto(volunteer, user);
    }

    @Override
    @Transactional
    public @NonNull UserPlanDto updatePlanUser(@NonNull Long shiftPlanId, @NonNull String userId, @NonNull UserPlanUpdateDto updateDto) {
        var volunteer = volunteerDao.getById(userId);
        securityHelper.assertUserIsPlanner(shiftPlanId);
        securityHelper.assertVolunteerIsVolunteer(shiftPlanDao.getById(shiftPlanId), volunteer, false);

        Collection<Role> requestedRoles = roleDao.getRolesByIdsAndShiftPlanId(updateDto.getRoles()
            .stream()
            .map(ConvertUtil::idToLong)
            .collect(Collectors.toSet()), shiftPlanId);

        Set<Long> currentRoles = volunteer.getRoles().stream()
            .filter(role -> role.getShiftPlan().getId() == shiftPlanId)
            .map(Role::getId)
            .collect(Collectors.toSet());

        assertRolesChanged(currentRoles, requestedRoles);

        var rolesToAdd = toAdd(updateDto.getRoles(), currentRoles);
        addRoles(volunteer, rolesToAdd, requestedRoles);

        var rolesToRemove = toRemove(updateDto.getRoles(), currentRoles);
        removeRoles(volunteer, rolesToRemove);

        var user = userDirectoryService.getUserById(volunteer.getId());
        userAttributeProvider.invalidateUserCache(userId);
        publisher.publishEvent(UserEvent.planUpdate(volunteer, shiftPlanDao.getById(shiftPlanId)));
        return UserAssemblingMapper.toUserPlanDto(volunteer, user, shiftPlanId);
    }

    @Override
    @Transactional
    public @NonNull UserEventDto lockUser(@NonNull String userId, @NonNull Collection<Long> shiftPlanIds) {
        shiftPlanIds.forEach(securityHelper::assertUserIsPlanner);
        var volunteer = volunteerDao.getById(userId);
        var plans = shiftPlanDao.getByIds(new HashSet<>(shiftPlanIds));
        for (ShiftPlan plan : plans) {
            securityHelper.assertUserIsPlanner(plan);
            securityHelper.assertVolunteerIsNotLockedInPlan(plan, volunteer);
            if (volunteer.getLockedPlans() == null) {
                volunteer.setLockedPlans(new HashSet<>());
            }
            volunteer.getLockedPlans().add(plan);
        }
        userAttributeProvider.invalidateUserCache(userId);
        publisher.publishEvent(UserEvent.lock(volunteer, plans));
        return toUserEventDto(volunteer);
    }

    @Override
    @Transactional
    public @NonNull UserEventDto unLockUser(@NonNull String userId, @NonNull Collection<Long> shiftPlanIds) {
        shiftPlanIds.forEach(securityHelper::assertUserIsPlanner);
        var volunteer = volunteerDao.getById(userId);
        var plans = shiftPlanDao.getByIds(new HashSet<>(shiftPlanIds));
        for (ShiftPlan plan : plans) {
            securityHelper.assertUserIsPlanner(plan);
            securityHelper.assertVolunteerIsLockedInPlan(plan, volunteer);
            volunteer.getLockedPlans().remove(plan);
        }
        userAttributeProvider.invalidateUserCache(userId);
        publisher.publishEvent(UserEvent.unlock(volunteer, plans));
        return toUserEventDto(volunteer);
    }

    @Override
    @Transactional
    public @NonNull UserEventDto resetUser(@NonNull String userId, @NonNull Collection<Long> shiftPlanId) {
        var volunteer = volunteerDao.getById(userId);
        var plans = shiftPlanDao.getByIds(new HashSet<>(shiftPlanId));
        for (ShiftPlan plan : plans) {
            securityHelper.assertUserIsPlanner(plan);
            securityHelper.assertVolunteerIsVolunteer(plan, volunteer, false);

            var assignments = assignmentService.getAllAssignmentsForUser(plan, volunteer);
            assignments.forEach(assignmentSwitchRequestDao::cancelTradesForAssignment);
            assignmentRepository.flush();
            assignments.forEach(assignmentService::unassignInternal);
        }
        userAttributeProvider.invalidateUserCache(userId);
        publisher.publishEvent(UserEvent.reset(volunteer, plans));
        return toUserEventDto(volunteer);
    }

    @Override
    public @NonNull Collection<UserPlanDto> bulkAddRoles(long shiftPlanId, UserPlanBulkDto updateDto) {
        securityHelper.assertUserIsPlanner(shiftPlanId);
        var volunteers = volunteerDao.findAllByVolunteerIds(updateDto.getVolunteers());
        var roles = roleDao.findAllById(updateDto.getRoles().stream().map(ConvertUtil::idToLong).toList());
        for (Volunteer v : volunteers) {
            for (Role role : roles) {
                if (!v.getRoles().contains(role)) {
                    v.getRoles().add(role);
                }
            }
        }
        var shiftPlan = shiftPlanDao.getById(shiftPlanId);
        publisher.publishEvent(UserPlanBulkEvent.add(volunteers, roles, shiftPlan));
        return getUserPlanDtos(shiftPlanId, volunteers);
    }

    @Override
    public @NonNull Collection<UserPlanDto> bulkRemoveRoles(long shiftPlanId, UserPlanBulkDto updateDto) {
        securityHelper.assertUserIsPlanner(shiftPlanId);
        var volunteers = volunteerDao.findAllByVolunteerIds(updateDto.getVolunteers());
        var roles = roleDao.findAllById(updateDto.getRoles().stream().map(ConvertUtil::idToLong).toList());
        for (Volunteer v : volunteers) {
            v.getRoles().removeAll(roles);
        }
        var shiftPlan = shiftPlanDao.getById(shiftPlanId);
        publisher.publishEvent(UserPlanBulkEvent.remove(volunteers, roles, shiftPlan));
        return getUserPlanDtos(shiftPlanId, volunteers);
    }

    @Override
    @AdminOnly
    @Transactional
    public @NonNull Collection<UserEventDto> bulkAddVolunteeringPlans(UserEventBulkDto updateDto) {
        var volunteers = volunteerDao.findAllByVolunteerIds(updateDto.getVolunteers());
        var plans = shiftPlanDao.getByIds(updateDto.getPlans().stream().map(ConvertUtil::idToLong).collect(Collectors.toSet()));
        for (Volunteer v : volunteers) {
            for (ShiftPlan plan : plans) {
                if (!v.getVolunteeringPlans().contains(plan)) {
                    v.getVolunteeringPlans().add(plan);
                }
            }
        }
        publisher.publishEvent(UserEventBulkEvent.add(volunteers, plans));
        return getUserEventDtos(volunteers);
    }

    @Override
    @AdminOnly
    @Transactional
    public @NonNull Collection<UserEventDto> bulkRemoveVolunteeringPlans(UserEventBulkDto updateDto) {
        var volunteers = volunteerDao.findAllByVolunteerIds(updateDto.getVolunteers());
        var plans = shiftPlanDao.getByIds(updateDto.getPlans().stream().map(ConvertUtil::idToLong).collect(Collectors.toSet()));
        for (Volunteer v : volunteers) {
            removePlans(v,
                plans.stream().map(ShiftPlan::getId).collect(Collectors.toSet()),
                Collections.emptySet()
            );
        }
        publisher.publishEvent(UserEventBulkEvent.remove(volunteers, plans));
        return getUserEventDtos(volunteers);
    }

    private @NonNull Collection<UserPlanDto> getUserPlanDtos(long shiftPlanId, Collection<Volunteer> volunteers) {
        var users = userDirectoryService.getUserByIds(volunteers.stream().map(Volunteer::getId).toList());
        userAttributeProvider.invalidateUserCaches(users.stream().map(UserInfo::id).toList());
        return UserAssemblingMapper.toUserPlanDto(volunteers, users, shiftPlanId);
    }

    private @NonNull Collection<UserEventDto> getUserEventDtos(Collection<Volunteer> volunteers) {
        var users = userDirectoryService.getUserByIds(volunteers.stream().map(Volunteer::getId).toList());
        return UserAssemblingMapper.toUserEventDtoForUsers(volunteers, users);
    }

    private @NonNull UserEventDto toUserEventDto(@NonNull Volunteer volunteer) {
        return UserAssemblingMapper.toUserEventDto(volunteer, getUserInfo(volunteer.getId()));
    }

    private @NonNull UserPlanDto toUserPlanDto(@NonNull Volunteer volunteer, long shiftPlanId) {
        return UserAssemblingMapper.toUserPlanDto(volunteer, getUserInfo(volunteer.getId()), shiftPlanId);
    }

    private UserInfo getUserInfo(String userId) {
        return userDirectoryService.getUserById(userId);
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private void addPlans(Volunteer volunteer, Set<Long> volunteerToAdd, Set<Long> planningToAdd) {
        if (volunteerToAdd != null && !volunteerToAdd.isEmpty()) {
            volunteer.setVolunteeringPlans(initIfNull(volunteer.getVolunteeringPlans()));
            volunteer.getVolunteeringPlans().addAll(shiftPlanDao.getByIds(volunteerToAdd));
        }
        if (planningToAdd != null && !planningToAdd.isEmpty()) {
            volunteer.setPlanningPlans(initIfNull(volunteer.getPlanningPlans()));
            volunteer.getPlanningPlans().addAll(shiftPlanDao.getByIds(planningToAdd));
        }
    }

    private void removePlans(Volunteer volunteer, Set<Long> volunteerToRemove, Set<Long> planningToRemove) {

        /* demote always possible */
        if (volunteer.getPlanningPlans() != null) {
            volunteer.getPlanningPlans().removeIf(x -> planningToRemove.contains(x.getId()));
        }

        /* only allow complete removal from plan if no assignments left */
        if (volunteer.getVolunteeringPlans() != null) {
            volunteer.getVolunteeringPlans().removeIf(plan -> {
                var removeMatch = volunteerToRemove.contains(plan.getId());
                if (removeMatch && !assignmentService.getAllAssignmentsForUser(plan, volunteer).isEmpty()) {
                    throw new BadRequestException("Cannot remove a plan in which the user still has assignments.");
                }

                /* if plan is to be removed, check additionally if user is still planner */
                if(
                    removeMatch && volunteer.getPlanningPlans() != null &&
                        volunteer.getPlanningPlans().stream().map(ShiftPlan::getId).anyMatch(planning -> planning.equals(plan.getId()))
                ) {
                    throw new BadRequestException("Cannot remove volunteer access from planner user.");
                }
                return removeMatch;
            });
        }

        /* if user is no longer registered in plan, remove from locked as well */
        if (volunteer.getLockedPlans() != null) {
            volunteer.getLockedPlans().removeIf(x -> volunteerToRemove.contains(x.getId()));
        }
    }

    private void addRoles(Volunteer vol, Set<Long> rolesToAdd, Collection<Role> roles) {
        if (rolesToAdd != null && !rolesToAdd.isEmpty()) {
            vol.setRoles(initIfNull(vol.getRoles()));
            vol.getRoles().addAll(roles.stream().filter(x -> rolesToAdd.contains(x.getId())).toList());
        }
    }

    private void removeRoles(Volunteer volunteer, Set<Long> rolesToRemove) {
        if (volunteer.getRoles() != null) {
            volunteer.getRoles().removeIf(x -> rolesToRemove.contains(x.getId()));
        }
    }

    private Set<Long> toAddPlans(Collection<String> request, Collection<ShiftPlan> existingPlans) {
        var existingIds = existingPlans.stream().map(ShiftPlan::getId).collect(Collectors.toSet());
        return toAdd(request, existingIds);
    }

    private Set<Long> toAdd(Collection<String> request, Set<Long> existingIds) {
        var requestSet = request.stream().map(ConvertUtil::idToLong).collect(Collectors.toSet());
        requestSet.removeAll(existingIds);
        return requestSet;
    }

    private Set<Long> toRemovePlans(Collection<String> request, Collection<ShiftPlan> existingPlans) {
        var existingIds = existingPlans.stream().map(ShiftPlan::getId).collect(Collectors.toSet());
        return toRemove(request, existingIds);
    }

    private Set<Long> toRemove(Collection<String> request, Set<Long> existingIds) {
        var requestSet = request.stream().map(ConvertUtil::idToLong).collect(Collectors.toSet());
        existingIds.removeAll(requestSet);
        return existingIds;
    }

    private void assertPlansChanged(Volunteer volunteer, UserEventUpdateDto updateDto) {
        var currentVolunteeringPlans = volunteer.getVolunteeringPlans().stream().map(x -> String.valueOf(x.getId())).toList();
        var currentPlaningPlans = volunteer.getPlanningPlans().stream().map(x -> String.valueOf(x.getId())).toList();
        if (new HashSet<>(currentVolunteeringPlans).equals(new HashSet<>(updateDto.getVolunteeringPlans()))
            && new HashSet<>(currentPlaningPlans).equals(new HashSet<>(updateDto.getPlanningPlans()))) {
            throw new BadRequestException("Update does not change anything");
        }

        if (!new HashSet<>(currentVolunteeringPlans).containsAll(currentPlaningPlans)) {
            throw new BadRequestException();
        }
    }

    private void assertRolesChanged(Collection<Long> currentRoles, Collection<Role> requestedRoles) {
        Set<Long> requested = new HashSet<>(currentRoles);

        Set<Long> current = requestedRoles.stream()
            .map(Role::getId)
            .collect(Collectors.toSet());

        if (requested.equals(current)) {
            throw new BadRequestException("Update does not change anything");
        }
    }

    private static <T> Collection<T> initIfNull(Collection<T> set) {
        return set != null ? set : new HashSet<>();
    }
}
