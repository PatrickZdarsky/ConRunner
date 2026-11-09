package at.control.shiftcontrol.shift.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.shiftcontrol.lib.common.UniqueCodeGenerator;
import at.shiftcontrol.lib.entity.Role;
import at.shiftcontrol.lib.entity.Shift;
import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.lib.entity.ShiftPlanInvite;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.lib.event.events.LeavePlanEvent;
import at.shiftcontrol.lib.event.events.ShiftPlanEvent;
import at.shiftcontrol.lib.event.events.ShiftPlanInviteEvent;
import at.shiftcontrol.lib.event.events.ShiftPlanVolunteerEvent;
import at.shiftcontrol.lib.exception.BadRequestException;
import at.shiftcontrol.lib.exception.ForbiddenException;
import at.shiftcontrol.lib.exception.IllegalStateException;
import at.shiftcontrol.lib.exception.UnauthorizedException;
import at.shiftcontrol.lib.type.LockStatus;
import at.shiftcontrol.lib.type.ShiftPlanInviteType;
import at.shiftcontrol.lib.util.ConvertUtil;
import at.shiftcontrol.shiftservice.annotation.AdminOnly;
import at.shiftcontrol.shiftservice.auth.ApplicationUserProvider;
import at.shiftcontrol.shiftservice.auth.UserAttributeProvider;
import at.shiftcontrol.shiftservice.auth.user.ShiftControlUser;
import at.shiftcontrol.shiftservice.dao.AssignmentDao;
import at.shiftcontrol.shiftservice.dao.EventDao;
import at.shiftcontrol.shiftservice.dao.ShiftPlanDao;
import at.shiftcontrol.shiftservice.dao.ShiftPlanInviteDao;
import at.shiftcontrol.shiftservice.dao.role.RoleDao;
import at.shiftcontrol.shiftservice.dao.userprofile.VolunteerDao;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanInviteCreateRequestDto;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanInviteCreateResponseDto;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanInviteDetailsDto;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanInviteDto;
import at.shiftcontrol.shiftservice.dto.invite.ShiftPlanJoinRequestDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanCreateDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanModificationDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanStatisticsDto;
import at.shiftcontrol.shiftservice.mapper.EventMapper;
import at.shiftcontrol.shiftservice.mapper.InviteMapper;
import at.shiftcontrol.shiftservice.mapper.ShiftPlanMapper;
import at.shiftcontrol.shiftservice.service.AssignmentService;
import at.shiftcontrol.shiftservice.service.ShiftPlanService;
import at.shiftcontrol.shiftservice.service.VolunteerService;
import at.shiftcontrol.shiftservice.userdirectory.UserLookupService;
import at.shiftcontrol.shiftservice.util.SecurityHelper;

import static at.shiftcontrol.lib.type.LockStatus.SELF_SIGNUP;

@Service
@RequiredArgsConstructor
public class ShiftPlanServiceImpl implements ShiftPlanService {
    private final AssignmentService assignmentService;

    private final EventDao eventDao;
    private final ShiftPlanDao shiftPlanDao;
    private final ShiftPlanInviteDao shiftPlanInviteDao;
    private final RoleDao roleDao;
    private final VolunteerDao volunteerDao;

    private final SecurityHelper securityHelper;
    private final ApplicationUserProvider userProvider;
    private final ApplicationEventPublisher publisher;
    private final UserAttributeProvider userAttributeProvider;
    private final UserLookupService userDirectoryService;

    private final UniqueCodeGenerator uniqueCodeGenerator;

    // URL-safe, human-friendly alphabet (no 0/O, 1/I to reduce confusion)
    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 8;
    private static final int MAX_INVITE_CODE_GENERATION_ATTEMPTS = 10;
    private final AssignmentDao assignmentDao;
    private final VolunteerService volunteerService;

    @Override
    public @NonNull Collection<ShiftPlanDto> getAllOfEvent(long eventId) {
        eventDao.getById(eventId);
        return ShiftPlanMapper.toShiftPlanDto(shiftPlanDao.findByEventId(eventId));
    }

    @Override
    @AdminOnly
    public @NonNull Collection<ShiftPlanDto> getAll() {
        return ShiftPlanMapper.toShiftPlanDto(shiftPlanDao.findAll());
    }

    @Override
    public @NonNull ShiftPlanDto get(long shiftPlanId) {
        return ShiftPlanMapper.toShiftPlanDto(shiftPlanDao.getById(shiftPlanId));
    }

    @Override
    @AdminOnly
    public @NonNull ShiftPlanCreateDto createShiftPlan(long eventId, @NonNull ShiftPlanModificationDto modificationDto) {
        var event = eventDao.getById(eventId);
        validateUniqueNameInEvent(eventId, modificationDto.getName(), null);

        var plan = ShiftPlanMapper.toShiftPlan(modificationDto);
        plan.setEvent(event);
        plan.setLockStatus(SELF_SIGNUP);
        plan = shiftPlanDao.save(plan);

        // create unspecific invites for volunteer and planner by default
        var volunteerInvite = ShiftPlanInvite.builder()
            .code(callGenerateUniqueCode())
            .type(ShiftPlanInviteType.VOLUNTEER_JOIN)
            .shiftPlan(plan)
            .active(true)
            .uses(0)
            .createdAt(Instant.now())
            .build();
        volunteerInvite = shiftPlanInviteDao.save(volunteerInvite);

        var plannerInvite = ShiftPlanInvite.builder()
            .code(callGenerateUniqueCode())
            .type(ShiftPlanInviteType.PLANNER_JOIN)
            .shiftPlan(plan)
            .active(true)
            .uses(0)
            .createdAt(Instant.now())
            .build();
        plannerInvite = shiftPlanInviteDao.save(plannerInvite);

        publisher.publishEvent(ShiftPlanEvent.planCreated(plan));
        var shiftPlanDto = ShiftPlanMapper.toShiftPlanDto(plan);

        return ShiftPlanCreateDto.builder()
            .shiftPlan(shiftPlanDto)
            .volunteerInvite(InviteMapper.toInviteDto(volunteerInvite, plan))
            .plannerInvite(InviteMapper.toInviteDto(plannerInvite, plan))
            .build();
    }

    @Override
    @AdminOnly
    public @NonNull ShiftPlanDto update(long shiftPlanId, @NonNull ShiftPlanModificationDto modificationDto) {
        var plan = shiftPlanDao.getById(shiftPlanId);
        validateUniqueNameInEvent(plan.getEvent().getId(), modificationDto.getName(), shiftPlanId);

        ShiftPlanMapper.updateShiftPlan(modificationDto, plan);
        shiftPlanDao.save(plan);
        publisher.publishEvent(ShiftPlanEvent.planUpdated(plan));
        return ShiftPlanMapper.toShiftPlanDto(plan);
    }

    void validateUniqueNameInEvent(long eventId, String name, Long excludeShiftPlanId) {
        var shiftPlansInEvent = shiftPlanDao.findByEventId(eventId);
        boolean nameExists = shiftPlansInEvent.stream()
            .anyMatch(shiftPlan -> shiftPlan.getName() != null
                && shiftPlan.getName().equalsIgnoreCase(name)
                && (excludeShiftPlanId == null || shiftPlan.getId() != excludeShiftPlanId));
        if (nameExists) {
            throw new BadRequestException("A shift plan with the given name already exists in the event");
        }
    }

    @Override
    @AdminOnly
    public void delete(long shiftPlanId) {
        var shiftPlan = shiftPlanDao.getById(shiftPlanId);

        var invites = shiftPlanInviteDao.findAllByShiftPlanId(shiftPlanId);
        for (var invite : invites) {
            var inviteEvent = ShiftPlanInviteEvent.inviteDeleted(invite);
            shiftPlanInviteDao.delete(invite);
            publisher.publishEvent(inviteEvent);
        }

        var shiftPlanEvent = ShiftPlanEvent.planDeleted(shiftPlan);
        shiftPlanDao.delete(shiftPlan);
        publisher.publishEvent(shiftPlanEvent);
    }

    @Override
    public @NonNull ShiftPlanInviteCreateResponseDto createShiftPlanInviteCode(long shiftPlanId, @NonNull ShiftPlanInviteCreateRequestDto requestDto) {
        var currentUser = userProvider.getCurrentUser();
        validatePermission(shiftPlanId, requestDto.getType(), currentUser);
        final var shiftPlan = shiftPlanDao.getById(shiftPlanId);
        if (requestDto.getExpiresAt() != null && requestDto.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("expiresAt must be in the future");
        }
        if (requestDto.getMaxUses() != null && requestDto.getMaxUses() <= 0) {
            throw new BadRequestException("maxUses must be positive");
        }
        // Generate a unique code (retry a few times)
        String code = callGenerateUniqueCode();
        Collection<Role> rolesToAssign = List.of();
        if (requestDto.getAutoAssignRoleIds() != null && !requestDto.getAutoAssignRoleIds().isEmpty()) {
            rolesToAssign = roleDao.findAllById(requestDto.getAutoAssignRoleIds().stream().map(ConvertUtil::idToLong).toList());
            if (rolesToAssign.size() != requestDto.getAutoAssignRoleIds().size()) {
                throw new BadRequestException("One or more roleIds are invalid");
            }
            if (!rolesToAssign.stream().allMatch(role -> role.getShiftPlan().getId() == shiftPlanId)) {
                throw new BadRequestException("One or more roles do not belong to the specified shift plan");
            }
        }
        var invite = ShiftPlanInvite.builder()
            .code(code)
            .type(requestDto.getType())
            .shiftPlan(shiftPlan)
            .active(true)
            .expiresAt(requestDto.getExpiresAt())
            .maxUses(requestDto.getMaxUses())
            .uses(0)
            .createdAt(Instant.now())
            .autoAssignRoles(rolesToAssign.isEmpty() ? null : rolesToAssign)
            .build();
        // Should not happen but just in case we retry once
        try {
            shiftPlanInviteDao.save(invite);
        } catch (DataIntegrityViolationException e) {
            // fallback once, because code uniqueness might collide under concurrency
            invite.setCode(callGenerateUniqueCode());
            shiftPlanInviteDao.save(invite);
        }
        publisher.publishEvent(ShiftPlanInviteEvent.inviteCreated(invite));
        return ShiftPlanInviteCreateResponseDto.builder()
            .code(invite.getCode())
            .type(invite.getType())
            .expiresAt(invite.getExpiresAt())
            .maxUses(invite.getMaxUses())
            .build();
    }

    private String callGenerateUniqueCode() {
        return uniqueCodeGenerator.generateUnique(
            INVITE_CODE_ALPHABET,
            INVITE_CODE_LENGTH,
            MAX_INVITE_CODE_GENERATION_ATTEMPTS,
            shiftPlanInviteDao::existsByCode
        );
    }

    @Override
    public void revokeShiftPlanInvite(long inviteId) {
        var currentUser = userProvider.getCurrentUser();
        var invite = shiftPlanInviteDao.getById(inviteId);
        validatePermission(invite.getShiftPlan().getId(), invite.getType(), currentUser);
        invite.setActive(false);
        invite.setRevokedAt(Instant.now());
        publisher.publishEvent(ShiftPlanInviteEvent.inviteRevoked(invite));
        shiftPlanInviteDao.save(invite);
    }

    @Override
    public void deleteShiftPlanInvite(long inviteId) {
        var currentUser = userProvider.getCurrentUser();
        var invite = shiftPlanInviteDao.getById(inviteId);
        validatePermission(invite.getShiftPlan().getId(), invite.getType(), currentUser);
        ShiftPlanInviteEvent inviteEvent = ShiftPlanInviteEvent.inviteDeleted(invite);
        shiftPlanInviteDao.delete(invite);
        publisher.publishEvent(inviteEvent);
    }

    private void validatePermission(long shiftPlanId, ShiftPlanInviteType type, ShiftControlUser currentUser) {
        if (type == ShiftPlanInviteType.VOLUNTEER_JOIN) {
            securityHelper.assertUserIsPlanner(shiftPlanId, currentUser);
        }
        boolean isNotAdmin = securityHelper.isNotUserAdmin(currentUser);
        // only allowed by admins
        if (type == ShiftPlanInviteType.PLANNER_JOIN && isNotAdmin) {
            throw new ForbiddenException("Only admins can create planner join invite codes.");
        }
    }

    @Override
    public @NonNull ShiftPlanInviteDetailsDto getShiftPlanInviteDetails(@NonNull String inviteCode) {
        String userId;
        try {
            userId = userProvider.getCurrentUser().getUserId();
        } catch (UnauthorizedException e) {
            // user not logged in, proceed without user context
            userId = "";
        }

        var invite = shiftPlanInviteDao.getByCode(inviteCode);
        var shiftPlan = shiftPlanDao.getById(invite.getShiftPlan().getId());

        // volunteer not necessary to get invite details
        var volunteer = volunteerDao.findById(userId);

        boolean alreadyJoined = volunteer.isPresent() && isUserAlreadyInShiftPlan(invite.getType(), shiftPlan, volunteer.get());
        boolean upgradeToPlannerPossible = volunteer.isPresent() && isUserAlreadyInShiftPlan(ShiftPlanInviteType.VOLUNTEER_JOIN, shiftPlan, volunteer.get())
            && !isUserAlreadyInShiftPlan(ShiftPlanInviteType.PLANNER_JOIN, shiftPlan, volunteer.get())
            && invite.getType() == ShiftPlanInviteType.PLANNER_JOIN;

        var rolesToAssign = invite.getAutoAssignRoles();
        boolean extensionOfRolesPossible = volunteer.isPresent()
            && rolesToAssign != null
            && rolesToAssign.stream().anyMatch(role -> !volunteer.get().getRoles().contains(role));

        var eventDto = EventMapper.toEventDto(shiftPlan.getEvent());
        var inviteDto = InviteMapper.toInviteDto(invite, shiftPlan);
        return ShiftPlanInviteDetailsDto.builder()
            .attendingVolunteerCount(shiftPlan.getPlanVolunteers().size())
            .joined(alreadyJoined)
            .upgradeToPlannerPossible(upgradeToPlannerPossible)
            .extensionOfRolesPossible(extensionOfRolesPossible)
            .inviteDto(inviteDto)
            .eventDto(eventDto)
            .build();
    }

    private boolean isUserAlreadyInShiftPlan(ShiftPlanInviteType type, ShiftPlan shiftPlan, Volunteer volunteer) {
        switch (type) {
            case VOLUNTEER_JOIN -> {
                return shiftPlan.getPlanVolunteers().contains(volunteer);
            }
            case PLANNER_JOIN -> {
                return shiftPlan.getPlanPlanners().contains(volunteer);
            }
            default -> throw new IllegalStateException("Unknown invite type: " + type);
        }
    }

    @Override
    public @NonNull Collection<ShiftPlanInviteDto> getAllShiftPlanInvites(long shiftPlanId) {
        // both planners and admins can list invites
        securityHelper.assertUserIsPlanner(shiftPlanId);
        var shiftPlan = shiftPlanDao.getById(shiftPlanId);
        var invites = shiftPlanInviteDao.findAllByShiftPlanId(shiftPlanId);
        return invites.stream()
            .map(invite -> InviteMapper.toInviteDto(invite, shiftPlan))
            .toList();
    }

    @Override
    @Transactional
    public void joinShiftPlan(@NonNull @Valid ShiftPlanJoinRequestDto requestDto) {
        String userId = userProvider.getCurrentUser().getUserId();
        String inviteCode = requestDto.getInviteCode().trim();
        ShiftPlanInvite invite = shiftPlanInviteDao.getByCode(inviteCode);
        validateInvite(invite);
        ShiftPlan shiftPlan = shiftPlanDao.getById(invite.getShiftPlan().getId());

        // volunteer data might not yet exist
        Volunteer volunteer = volunteerService.getOrCreate(userId);

        boolean joinedNow = addUserToShiftPlanIfAbsent(invite.getType(), shiftPlan, volunteer);

        var rolesToAssign = invite.getAutoAssignRoles();
        addRolesToUser(rolesToAssign, volunteer);

        // Increase uses and invalidate cache if joined now
        if (joinedNow) {
            userAttributeProvider.invalidateUserCache(userId);
            invite.setUses(invite.getUses() + 1);
        }
        // auto-deactivate if max uses reached
        if (invite.getMaxUses() != null && invite.getUses() >= invite.getMaxUses()) {
            invite.setActive(false);
            invite.setRevokedAt(Instant.now());
        }
        // save updates
        shiftPlanInviteDao.save(invite);
        shiftPlanDao.save(shiftPlan);

        //NOTIFY: publish events
        if (invite.getType() == ShiftPlanInviteType.VOLUNTEER_JOIN) {
            publisher.publishEvent(ShiftPlanVolunteerEvent.joinedAsVolunteer(shiftPlan, userId));
        } else if (invite.getType() == ShiftPlanInviteType.PLANNER_JOIN) {
            publisher.publishEvent(ShiftPlanVolunteerEvent.joinedAsPlanner(shiftPlan, userId));
        } else {
            throw new IllegalStateException("Unknown invite type: " + invite.getType());
        }
    }

    private void validateInvite(ShiftPlanInvite invite) {
        if (!invite.isActive()) {
            throw new BadRequestException("Invite code is revoked or inactive");
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invite code is expired");
        }
        if (invite.getMaxUses() != null && invite.getUses() >= invite.getMaxUses()) {
            throw new BadRequestException("Invite code has reached its max uses");
        }
    }

    // returns true if user was newly added, false if already a member
    private boolean addUserToShiftPlanIfAbsent(ShiftPlanInviteType type, ShiftPlan shiftPlan, Volunteer volunteer) {
        switch (type) {
            case VOLUNTEER_JOIN -> {
                if (shiftPlan.getPlanVolunteers().contains(volunteer)) {
                    return false;
                }
                shiftPlan.addPlanVolunteer(volunteer);
                return true;
            }
            case PLANNER_JOIN -> {
                if (shiftPlan.getPlanPlanners().contains(volunteer)) {
                    return false;
                }
                shiftPlan.addPlanPlanner(volunteer);
                if (!shiftPlan.getPlanVolunteers().contains(volunteer)) {
                    shiftPlan.addPlanVolunteer(volunteer); // planners are also volunteers (if not already)
                }
                return true;
            }
            default -> throw new IllegalStateException("Unknown invite type: " + type);
        }
    }

    private void addRolesToUser(Collection<Role> rolesToAssign, Volunteer volunteer) {
        if (rolesToAssign != null && !rolesToAssign.isEmpty()) {
            for (var role : rolesToAssign) {
                if (!volunteer.getRoles().contains(role)) {
                    volunteer.getRoles().add(role);
                }
            }
            volunteerDao.save(volunteer);
        }
    }

    @Override
    public void updateLockStatus(long shiftPlanId, @NonNull LockStatus lockStatus) {
        var shiftPlan = shiftPlanDao.getById(shiftPlanId);
        securityHelper.assertUserIsPlanner(shiftPlan);
        if (shiftPlan.getLockStatus().equals(lockStatus)) {
            throw new BadRequestException("Lock status already in requested state");
        }
        // automatically unassign all open auctions and decline all signup requests when switching back to SELF_SIGNUP
        if (lockStatus.equals(LockStatus.SELF_SIGNUP)) {
            assignmentService.unassignAllAuctions(shiftPlan);
            assignmentService.declineAllSignupRequests(shiftPlan);
        }
        shiftPlan.setLockStatus(lockStatus);
        shiftPlanDao.save(shiftPlan);
        publisher.publishEvent(ShiftPlanEvent.planLockStatusChanged(shiftPlan));
    }

    @Override
    @Transactional
    public void leavePlan(long shiftPlanId) {
        var plan = shiftPlanDao.getById(shiftPlanId);
        var user = userProvider.getCurrentUser();
        if (!user.isVolunteerInPlan(shiftPlanId)) {
            throw new BadRequestException("User is not volunteer of the provided shiftPlan.");
        }

        if (!plan.getLockStatus().equals(SELF_SIGNUP)) {
            throw new BadRequestException("Plan is managed by supervisor, leave is currently not possible.");
        }

        var existingAssignments = assignmentDao.findAssignmentsForShiftPlanAndUser(shiftPlanId, user.getUserId());
        if (!existingAssignments.isEmpty()) {
            assignmentDao.deleteAll(existingAssignments);
        }

        var volunteer = volunteerDao.getById(user.getUserId());
        volunteer.getVolunteeringPlans().remove(plan);

        userAttributeProvider.invalidateUserCache(user.getUserId());

        publisher.publishEvent(LeavePlanEvent.leavePlan(volunteer, String.valueOf(shiftPlanId), existingAssignments));
    }

    @Override
    @AdminOnly
    public @NonNull ShiftPlanStatisticsDto getShiftPlanStatistics(long shiftPlanId) {
        var plan = shiftPlanDao.getById(shiftPlanId);

        var shiftCount = 0;
        var totalPositions = 0;
        var currentPositionAssignedCount = 0;
        var totalShiftHours = 0.0;
        var totalPositionVolunteerHours = 0.0;
        var currentPositionVolunteerHours = 0.0;
        var volunteerStatsMap = new HashMap<String, ShiftPlanStatisticsDto.VolunteerShiftStatisticsDto>();
        var openPositions = new ArrayList<ShiftPlanStatisticsDto.PositionInfoDto>();
        for (Shift shift : plan.getShifts()) {
            shiftCount++;
            var shiftLengthHours = Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes() / 60.0;
            totalShiftHours += shiftLengthHours;

            var positionCount = shift.getSlots().size();
            totalPositions += positionCount;
            totalPositionVolunteerHours += positionCount * shiftLengthHours;

            for (var slot : shift.getSlots()) {
                for (var assignment : slot.getAssignments()) {
                    var stats = volunteerStatsMap.computeIfAbsent(assignment.getAssignedVolunteer().getId(),
                        id -> new ShiftPlanStatisticsDto.VolunteerShiftStatisticsDto(id, userDirectoryService.getUserById(id).username()));

                    stats.setPositionCount(stats.getPositionCount() + 1);
                    stats.setTotalHours(stats.getTotalHours() + shiftLengthHours);
                    stats.getAssignments().add(new ShiftPlanStatisticsDto.AssignmentInfoDto(
                        slot.getId(),
                        slot.getName(),
                        shift.getId(),
                        shift.getName(),
                        shiftLengthHours
                    ));
                    currentPositionVolunteerHours += shiftLengthHours;
                    currentPositionAssignedCount++;
                }

                if (slot.getDesiredVolunteerCount() > slot.getAssignments().size()) {
                    openPositions.add(new ShiftPlanStatisticsDto.PositionInfoDto(
                        slot.getId(),
                        slot.getName(),
                        shift.getId(),
                        shift.getName(),
                        slot.getDesiredVolunteerCount() - slot.getAssignments().size(),
                        shiftLengthHours
                    ));
                }
            }
        }
        return ShiftPlanStatisticsDto.builder()
            .shiftCount(shiftCount)
            .volunteerCount(volunteerStatsMap.size())
            .totalPositions(totalPositions)
            .currentPositionAssignedCount(currentPositionAssignedCount)
            .totalShiftHours(totalShiftHours)
            .totalPositionVolunteerHours(totalPositionVolunteerHours)
            .currentPositionVolunteerHours(currentPositionVolunteerHours)
            .volunteerStats(new ArrayList<>(volunteerStatsMap.values()))
            .openPositionStats(openPositions)
            .build();
    }
}
