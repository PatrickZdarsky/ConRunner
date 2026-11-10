package at.control.shiftcontrol.shift.service.impl;

import at.control.shiftcontrol.communications.api.NotificationTypes;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import at.control.shiftcontrol.communications.api.NotificationRequest;
import at.control.shiftcontrol.communications.api.NotificationRequests;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.Role;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.event.events.RoleEvent;
import at.control.shiftcontrol.shift.lib.event.events.RoleVolunteerEvent;
import at.control.shiftcontrol.platform.exception.BadRequestException;
import at.control.shiftcontrol.platform.exception.ForbiddenException;
import at.control.shiftcontrol.platform.exception.NotFoundException;
import at.control.shiftcontrol.shift.dao.ShiftPlanDao;
import at.control.shiftcontrol.shift.dao.role.RoleDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.role.RoleDto;
import at.control.shiftcontrol.shift.dto.role.RoleModificationDto;
import at.control.shiftcontrol.shift.dto.role.UserRoleAssignmentAssignDto;
import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;
import at.control.shiftcontrol.shift.mapper.RoleMapper;
import at.control.shiftcontrol.shift.mapper.VolunteerAssemblingMapper;
import at.control.shiftcontrol.shift.service.RoleService;
import at.control.shiftcontrol.shift.userdirectory.UserLookupService;
import at.control.shiftcontrol.shift.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleDao roleDao;
    private final ShiftPlanDao shiftPlanDao;
    private final VolunteerDao volunteerDao;
    private final SecurityHelper securityHelper;
    private final ApplicationEventPublisher publisher;
    private final NotificationRequests notificationRequests;
    private final UserLookupService userDirectoryService;

    @Override
    public @org.jspecify.annotations.NonNull Collection<RoleDto> getRoles(long shiftPlanId) {
        return RoleMapper.toRoleDto(roleDao.findAllByShiftPlanId(shiftPlanId));
    }

    @Override
    public @org.jspecify.annotations.NonNull RoleDto getRole(long roleId) {
        Role role = roleDao.getById(roleId);
        securityHelper.assertUserIsPlanner(role.getShiftPlan().getId());
        return RoleMapper.toRoleDto(role);
    }

    @Override
    public @org.jspecify.annotations.NonNull RoleDto createRole(long shiftPlanId, @NonNull RoleModificationDto roleDto) {
        securityHelper.assertUserIsPlanner(shiftPlanId);
        var shiftPlan = shiftPlanDao.getById(shiftPlanId);
        validateUniqueNameInShiftPlan(roleDto.getName(), shiftPlanId, null);

        var role = RoleMapper.toRole(roleDto);
        role.setShiftPlan(shiftPlan);

        publisher.publishEvent(RoleEvent.roleCreated(role));
        return RoleMapper.toRoleDto(roleDao.save(role));
    }

    @Override
    public @org.jspecify.annotations.NonNull RoleDto updateRole(long roleId, @NonNull RoleModificationDto roleDto) {
        var role = roleDao.getById(roleId);
        securityHelper.assertUserIsPlanner(role.getShiftPlan().getId());
        validateUniqueNameInShiftPlan(roleDto.getName(), role.getShiftPlan().getId(), roleId);

        updateRole(roleDto, role);
        role = roleDao.save(role);

        publisher.publishEvent(RoleEvent.roleUpdated(role));
        return RoleMapper.toRoleDto(role);
    }

    private void validateUniqueNameInShiftPlan(String name, long shiftPlanId, Long excludeRoleId) {
        var existingRole = roleDao.findByNameAndShiftPlanId(name, shiftPlanId);
        if (existingRole.isPresent() && (excludeRoleId == null || existingRole.get().getId() != excludeRoleId)) {
            throw new BadRequestException("A role with the given name already exists in this shift plan.");
        }
    }

    private void updateRole(@NonNull RoleModificationDto roleDto, Role role) {
        role.setName(roleDto.getName());
        role.setDescription(roleDto.getDescription());
        role.setSelfAssignable(roleDto.isSelfAssignable());
        role.setRewardPointsPerMinute(roleDto.getRewardPointsPerMinute());
    }

    @Override
    public void deleteRole(long roleId) {
        Role role = roleDao.getById(roleId);
        securityHelper.assertUserIsPlanner(role.getShiftPlan().getId());

        var roleEvent = RoleEvent.roleDeleted(role);
        publisher.publishEvent(roleEvent);
        roleDao.delete(role);
    }

    @Override
    @Transactional
    public @org.jspecify.annotations.NonNull VolunteerDto createUserRoleAssignment(String userId, @org.jspecify.annotations.NonNull UserRoleAssignmentAssignDto assignDto) {
        Role role = roleDao.getById(Long.valueOf(assignDto.getRoleId()));
        Volunteer volunteer = volunteerDao.getById(userId);
        if (!role.isSelfAssignable()) {
            securityHelper.assertUserIsPlanner(role.getShiftPlan().getId());
        }
        if (volunteer.getRoles() != null && volunteer.getRoles().stream().anyMatch(r -> r.getId() == role.getId())) {
            throw new ForbiddenException("Role already assigned to user.");
        }
        if (volunteer.getRoles() == null) {
            volunteer.setRoles(new HashSet<>());
        }
        volunteer.getRoles().add(role);
        volunteerDao.save(volunteer);

        notificationRequests.request(new NotificationRequest(java.util.UUID.randomUUID(), java.util.UUID.fromString(volunteer.getId()),
            NotificationTypes.ROLE_ASSIGNED, String.valueOf(role.getShiftPlan().getEvent().getId())));

        publisher.publishEvent(RoleVolunteerEvent.roleAssigned(role, volunteer.getId()));
        return VolunteerAssemblingMapper.toDtoFromUser(userDirectoryService.getUserById(volunteer.getId()));
    }

    @Override
    @Transactional
    public void deleteUserRoleAssignment(String userId, long roleId) {
        Role role = roleDao.getById(roleId);
        Volunteer volunteer = volunteerDao.getById(userId);
        if (!role.isSelfAssignable()) {
            securityHelper.assertUserIsPlanner(role.getShiftPlan().getId());
        }
        boolean removed = volunteer.getRoles().removeIf(r -> r.getId() == roleId);
        if (!removed) {
            throw new NotFoundException("Role is not assigned to this user.");
        }

        publisher.publishEvent(RoleVolunteerEvent.roleUnassigned(role, volunteer.getId()));
        volunteerDao.save(volunteer);
        notificationRequests.request(new NotificationRequest(java.util.UUID.randomUUID(), java.util.UUID.fromString(volunteer.getId()),
            NotificationTypes.ROLE_UNASSIGNED, String.valueOf(role.getShiftPlan().getEvent().getId())));
    }
}
