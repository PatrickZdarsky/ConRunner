package at.control.shiftcontrol.shift.service.impl;

import java.util.UUID;
import at.control.shiftcontrol.communications.api.NotificationTypes;
import at.control.shiftcontrol.communications.api.NotificationContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import at.control.shiftcontrol.communications.api.NotificationRequest;
import at.control.shiftcontrol.communications.api.NotificationRequests;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.control.shiftcontrol.shift.dto.PaginationDto;
import at.control.shiftcontrol.shift.entity.Role;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.event.events.UserEvent;
import at.control.shiftcontrol.shift.lib.event.events.UserEventBulkEvent;
import at.control.shiftcontrol.shift.lib.event.events.UserPlanBulkEvent;
import at.control.shiftcontrol.platform.exception.BadRequestException;
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.identity.AdminOnly;
import at.control.shiftcontrol.shift.auth.UserAttributeProvider;
import at.control.shiftcontrol.shift.dao.AssignmentSwitchRequestDao;
import at.control.shiftcontrol.shift.dao.ShiftPlanDao;
import at.control.shiftcontrol.shift.dao.role.RoleDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.user.UserEventBulkDto;
import at.control.shiftcontrol.shift.dto.user.UserEventDto;
import at.control.shiftcontrol.shift.dto.user.UserEventUpdateDto;
import at.control.shiftcontrol.shift.dto.user.UserPlanBulkDto;
import at.control.shiftcontrol.shift.dto.user.UserPlanDto;
import at.control.shiftcontrol.shift.dto.user.UserPlanUpdateDto;
import at.control.shiftcontrol.shift.dto.user.UserSearchDto;
import at.control.shiftcontrol.shift.mapper.PaginationMapper;
import at.control.shiftcontrol.shift.mapper.UserAssemblingMapper;
import at.control.shiftcontrol.shift.repo.AssignmentRepository;
import at.control.shiftcontrol.shift.service.AssignmentService;
import at.control.shiftcontrol.shift.service.UserAdministrationService;
import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserService;
import at.control.shiftcontrol.shift.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class UserAdministrationServiceImpl implements UserAdministrationService {
    private final VolunteerDao volunteerDao;
    private final ShiftPlanDao shiftPlanDao;
    private final UserService userService;
    private final UserAttributeProvider userAttributeProvider;
    private final SecurityHelper securityHelper;
    private final RoleDao roleDao;
    private final ApplicationEventPublisher publisher;
    private final NotificationRequests notificationRequests;
    private final AssignmentService assignmentService;
    private final AssignmentSwitchRequestDao assignmentSwitchRequestDao;
    private final AssignmentRepository assignmentRepository;

    @Override
    @AdminOnly
    public @NonNull PaginationDto<UserEventDto> getAllUsers(int page, int size, @NonNull UserSearchDto searchDto) {
        var users = userService.searchUsers(page, size, at.control.shiftcontrol.identity.UserSearchRequest.builder().name(searchDto.getName()).build());
        var volunteers = volunteerDao.findAllByVolunteerIds(
            users.getContent().stream().map(User::getId).toList()
        );

        return PaginationMapper.toPaginationDto(size, page, users.getTotalElements(), UserAssemblingMapper.toUserEventDtoForUsers(volunteers, users.getContent()));
    }

    @Override
    public @NonNull PaginationDto<UserPlanDto> getAllPlanUsers(@NonNull Long shiftPlanId, int page, int size, @NonNull UserSearchDto searchDto) {
        securityHelper.assertUserIsPlanner(shiftPlanId);
        var volunteersInPlanId = volunteerDao.findAllIdsByShiftPlan(shiftPlanId);
        var usersForPlan = userService.getUsers(volunteersInPlanId).stream()
            .sorted(Comparator.comparing(user -> user.getUsername() == null ? "" : user.getUsername()))
            .toList();
        var users = filterUsers(searchDto, usersForPlan);
        var paginatedUsers = paginateUsers(users, page, size);
        var volunteers = volunteerDao.findAllByVolunteerIds(
            paginatedUsers.stream().map(User::getId).toList()
        );

        return PaginationMapper.toPaginationDto(size, page, users.size(), UserAssemblingMapper.toUserPlanDto(volunteers, paginatedUsers, shiftPlanId));
    }

    private List<User> paginateUsers(List<User> users, int page, int size) {
        int fromIndex = page * size;
        if (fromIndex >= users.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + size, users.size());
        return users.subList(fromIndex, toIndex);
    }

    private static List<User> filterUsers(UserSearchDto searchDto, List<User> users) {
        if (searchDto.getName() == null || searchDto.getName().isEmpty()) {
            return users;
        }
        var nameLower = searchDto.getName().toLowerCase().trim();
        return users.stream().filter(x ->
                safeLower(x.getUsername()).contains(nameLower)
            )
            .toList();
    }

    @Override
    @AdminOnly
    public @NonNull UserEventDto getUser(@NonNull UUID userId) {
        var volunteer = volunteerDao.findById(userId).orElse(null);
        var user = userService.getUser(userId);
        return UserAssemblingMapper.toUserEventDto(volunteer, user);
    }

    @Override
    public @NonNull UserPlanDto getPlanUser(@NonNull Long shiftPlanId, @NonNull UUID userId) {
        var volunteer = volunteerDao.getById(userId);
        securityHelper.assertUserIsPlanner(shiftPlanId);
        securityHelper.assertVolunteerIsVolunteer(shiftPlanDao.getById(shiftPlanId), volunteer, false);
        return toUserPlanDto(volunteer, shiftPlanId);
    }

    @Override
    @Transactional
    @AdminOnly
    public @NonNull UserEventDto updateEventUser(@NonNull UUID userId, @NonNull UserEventUpdateDto updateDto) {
        var volunteer = volunteerDao.getById(userId);
        assertPlansChanged(volunteer, updateDto);
        var originalVolunteeringPlans = Optional.ofNullable(volunteer.getVolunteeringPlans()).orElse(Collections.emptyList());
        var originalPlanningPlans = Optional.ofNullable(volunteer.getPlanningPlans()).orElse(Collections.emptyList());

        var volunteerToAdd = toAddPlans(updateDto.getVolunteeringPlans(), originalVolunteeringPlans);
        var planningToAdd = toAddPlans(updateDto.getPlanningPlans(), originalPlanningPlans);
        var volunteerToRemove = toRemovePlans(updateDto.getVolunteeringPlans(), originalVolunteeringPlans);
        var planningToRemove = toRemovePlans(updateDto.getPlanningPlans(), originalPlanningPlans);

        removePlans(volunteer, volunteerToRemove, planningToRemove);
        addPlans(volunteer, volunteerToAdd, planningToAdd);

        var affectedPlanIds = Stream.of(
                volunteerToAdd,
                volunteerToRemove,
                planningToAdd,
                planningToRemove
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        var user = userService.getUser(volunteer.getId());
        userAttributeProvider.invalidateUserCache(userId);
        var allPlans = shiftPlanDao.getByIds(Stream.concat(
            updateDto.getPlanningPlans().stream(), updateDto.getVolunteeringPlans().stream())
            .map(ConvertUtil::idToLong)
            .collect(Collectors.toSet()));

        notifyUser(volunteer, shiftPlanDao.getByIds(affectedPlanIds), NotificationTypes.MEMBERSHIP_CHANGED);
        publisher.publishEvent(UserEvent.eventUpdate(volunteer, allPlans));
        return UserAssemblingMapper.toUserEventDto(volunteer, user);
    }

    @Override
    @Transactional
    public @NonNull UserPlanDto updatePlanUser(@NonNull Long shiftPlanId, @NonNull UUID userId, @NonNull UserPlanUpdateDto updateDto) {
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

        var user = userService.getUser(volunteer.getId());
        userAttributeProvider.invalidateUserCache(userId);
        var updatedPlan = shiftPlanDao.getById(shiftPlanId);
        notifyUser(volunteer, java.util.List.of(updatedPlan), NotificationTypes.ROLES_CHANGED);
        publisher.publishEvent(UserEvent.planUpdate(volunteer, updatedPlan));
        return UserAssemblingMapper.toUserPlanDto(volunteer, user, shiftPlanId);
    }

    @Override
    @Transactional
    public @NonNull UserEventDto lockUser(@NonNull UUID userId, @NonNull Collection<Long> shiftPlanIds) {
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
        notifyUser(volunteer, plans, NotificationTypes.VOLUNTEER_LOCKED);
        publisher.publishEvent(UserEvent.lock(volunteer, plans));
        return toUserEventDto(volunteer);
    }

    @Override
    @Transactional
    public @NonNull UserEventDto unLockUser(@NonNull UUID userId, @NonNull Collection<Long> shiftPlanIds) {
        shiftPlanIds.forEach(securityHelper::assertUserIsPlanner);
        var volunteer = volunteerDao.getById(userId);
        var plans = shiftPlanDao.getByIds(new HashSet<>(shiftPlanIds));
        for (ShiftPlan plan : plans) {
            securityHelper.assertUserIsPlanner(plan);
            securityHelper.assertVolunteerIsLockedInPlan(plan, volunteer);
            volunteer.getLockedPlans().remove(plan);
        }
        userAttributeProvider.invalidateUserCache(userId);
        notifyUser(volunteer, plans, NotificationTypes.VOLUNTEER_UNLOCKED);
        publisher.publishEvent(UserEvent.unlock(volunteer, plans));
        return toUserEventDto(volunteer);
    }

    @Override
    @Transactional
    public @NonNull UserEventDto resetUser(@NonNull UUID userId, @NonNull Collection<Long> shiftPlanId) {
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
        notifyUser(volunteer, plans, NotificationTypes.VOLUNTEER_RESET);
        publisher.publishEvent(UserEvent.reset(volunteer, plans));
        return toUserEventDto(volunteer);
    }

    @Override
    @Transactional
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
        volunteers.forEach(v -> notifyUser(v, java.util.List.of(shiftPlan), NotificationTypes.ROLES_CHANGED, "added"));
        publisher.publishEvent(UserPlanBulkEvent.add(volunteers, roles, shiftPlan));
        return getUserPlanDtos(shiftPlanId, volunteers);
    }

    @Override
    @Transactional
    public @NonNull Collection<UserPlanDto> bulkRemoveRoles(long shiftPlanId, UserPlanBulkDto updateDto) {
        securityHelper.assertUserIsPlanner(shiftPlanId);
        var volunteers = volunteerDao.findAllByVolunteerIds(updateDto.getVolunteers());
        var roles = roleDao.findAllById(updateDto.getRoles().stream().map(ConvertUtil::idToLong).toList());
        for (Volunteer v : volunteers) {
            v.getRoles().removeAll(roles);
        }
        var shiftPlan = shiftPlanDao.getById(shiftPlanId);
        volunteers.forEach(v -> notifyUser(v, java.util.List.of(shiftPlan), NotificationTypes.ROLES_CHANGED, "removed"));
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
        volunteers.forEach(v -> notifyUser(v, plans, NotificationTypes.MEMBERSHIP_CHANGED, "added"));
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
        volunteers.forEach(v -> notifyUser(v, plans, NotificationTypes.MEMBERSHIP_CHANGED, "removed"));
        publisher.publishEvent(UserEventBulkEvent.remove(volunteers, plans));
        return getUserEventDtos(volunteers);
    }

    private void notifyUser(Volunteer volunteer, Collection<ShiftPlan> plans, String type) {
        notifyUser(volunteer, plans, type, "");
    }

    private void notifyUser(Volunteer volunteer, Collection<ShiftPlan> plans, String type, String change) {
        plans.stream().map(plan -> plan.getEvent().getId()).distinct().forEach(eventId ->
            notificationRequests.request(new NotificationRequest(java.util.UUID.randomUUID(), volunteer.getId(),
                type, NotificationContext.encode(String.valueOf(eventId), "", change))));
    }

    private @NonNull Collection<UserPlanDto> getUserPlanDtos(long shiftPlanId, Collection<Volunteer> volunteers) {
        var users = userService.getUsers(volunteers.stream().map(Volunteer::getId).toList());
        userAttributeProvider.invalidateUserCaches(users.stream().map(User::getId).toList());
        return UserAssemblingMapper.toUserPlanDto(volunteers, users, shiftPlanId);
    }

    private @NonNull Collection<UserEventDto> getUserEventDtos(Collection<Volunteer> volunteers) {
        var users = userService.getUsers(volunteers.stream().map(Volunteer::getId).toList());
        return UserAssemblingMapper.toUserEventDtoForUsers(volunteers, users);
    }

    private @NonNull UserEventDto toUserEventDto(@NonNull Volunteer volunteer) {
        return UserAssemblingMapper.toUserEventDto(volunteer, lookupUser(volunteer.getId()));
    }

    private @NonNull UserPlanDto toUserPlanDto(@NonNull Volunteer volunteer, long shiftPlanId) {
        return UserAssemblingMapper.toUserPlanDto(volunteer, lookupUser(volunteer.getId()), shiftPlanId);
    }

    private User lookupUser(UUID userId) {
        return userService.getUser(userId);
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
