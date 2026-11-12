package at.conrunner.shift.service.event.impl;

import at.conrunner.shift.audit.EventManagementAudit;
import org.springframework.transaction.annotation.Transactional;
import at.conrunner.shift.audit.AssignmentAudit;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import at.conrunner.identity.User;
import at.conrunner.identity.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.SocialMediaLink;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.event.events.EventEvent;
import at.conrunner.platform.exception.BadRequestException;
import at.conrunner.platform.exception.ValidationException;
import at.conrunner.identity.AdminOnly;
import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.dao.EventDao;
import at.conrunner.shift.dao.RewardPointsTransactionDao;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.dto.event.EventDto;
import at.conrunner.shift.dto.event.EventModificationDto;
import at.conrunner.shift.dto.event.EventSearchDto;
import at.conrunner.shift.dto.event.EventShiftPlansOverviewDto;
import at.conrunner.shift.dto.event.SocialMediaLinkDto;
import at.conrunner.shift.dto.role.RoleDto;
import at.conrunner.shift.dto.rows.PlanVolunteerIdRow;
import at.conrunner.shift.dto.shiftplan.ShiftPlanContactInfoDto;
import at.conrunner.shift.dto.shiftplan.ShiftPlanDto;
import at.conrunner.shift.dto.user.ContactInfoDto;
import at.conrunner.shift.mapper.EventMapper;
import at.conrunner.shift.mapper.RoleMapper;
import at.conrunner.shift.mapper.ShiftPlanMapper;
import at.conrunner.shift.mapper.UserAssemblingMapper;
import at.conrunner.shift.service.StatisticService;
import at.conrunner.shift.service.event.EventService;
import at.conrunner.shift.util.SecurityHelper;
import at.conrunner.shift.util.SocialLinksParser;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventManagementAudit eventManagementAudit;
    private final AssignmentAudit audit;
    private final EventDao eventDao;
    private final VolunteerDao volunteerDao;
    private final RewardPointsTransactionDao rewardPointsTransactionDao;
    private final StatisticService statisticService;
    private final CurrentShiftActor currentActor;
    private final SecurityHelper securityHelper;
    private final ApplicationEventPublisher publisher;
    private final UserService userService;

    @Override
    public EventDto getEvent(long eventId) {
        var event = eventDao.getById(eventId);
        securityHelper.assertUserIsAllowedToAccessEvent(event);

        return EventMapper.toEventDto(event);
    }

    @Override
    public List<EventDto> search(EventSearchDto searchDto) {
        var filteredEvents = eventDao.search(searchDto);
        var actor = currentActor.get();

        // skip filtering for admin users
        if (securityHelper.isUserAdmin(actor)) {
            return EventMapper.toEventDto(filteredEvents);
        }
        UUID userId = actor.requireUserId();

        var volunteer = volunteerDao.getById(userId);
        var volunteerShiftPlans = volunteer.getVolunteeringPlans();
        var planningShiftPlans = volunteer.getPlanningPlans();

        // filter events that the volunteer is part of
        var relevantEvents = filteredEvents.stream()
            .filter(event -> event.getShiftPlans().stream()
                .anyMatch(shiftPlan -> volunteerShiftPlans.contains(shiftPlan) || planningShiftPlans.contains(shiftPlan))
            )
            .toList();

        return EventMapper.toEventDto(relevantEvents);
    }

    @Override
    public Collection<EventDto> getAllOpenEvents(UUID currentUser) {
        if (securityHelper.isUserAdmin()) {
            return EventMapper.toEventDto(eventDao.getAllOpenEvents());
        }
        return EventMapper.toEventDto(eventDao.getAllOpenEventsForUser(currentUser));
    }

    @Override
    public List<ShiftPlanDto> getUserRelatedShiftPlansOfEvent(long eventId, UUID userId) {
        var volunteer = volunteerDao.getById(userId);
        var event = eventDao.getById(eventId);
        var shiftPlans = securityHelper.isUserAdmin()
            ? List.copyOf(event.getShiftPlans())
            : getVolunteerShiftPlans(event, volunteer);
        return ShiftPlanMapper.toShiftPlanDto(shiftPlans);
    }

    @Override
    public Collection<ShiftPlanContactInfoDto> getPlannerContactInfo(long eventId, UUID userId) {
        // check access
        var event = eventDao.getById(eventId);
        securityHelper.assertUserIsAllowedToAccessEvent(event);

        // get planner IDs of users plans
        Collection<PlanVolunteerIdRow> rows = eventDao.getPlannersForEventAndUser(eventId, userId);

        // Resolve all planner contacts from Shiftservice's local user directory.
        Set<UUID> allPlannerIds =
            rows.stream()
                .map(PlanVolunteerIdRow::getVolunteerId)
                .collect(Collectors.toSet());
        var planners = userService.getUsers(allPlannerIds);

        // create maps to lookup userIds per plan, planNames and planner-userRepresentations
        Map<Long, List<UUID>> volunteerIdsByPlan =
            rows.stream()
                .collect(Collectors.groupingBy(
                    PlanVolunteerIdRow::getPlanId,
                    LinkedHashMap::new,
                    Collectors.mapping(
                        PlanVolunteerIdRow::getVolunteerId,
                        Collectors.toList()
                    )
                ));
        Map<Long, String> planNames =
            rows.stream()
                .collect(Collectors.toMap(
                    PlanVolunteerIdRow::getPlanId,
                    PlanVolunteerIdRow::getPlanName,
                    (a, b) -> a
                ));
        Map<UUID, ContactInfoDto> contactInfoById =
            planners.stream()
                .collect(Collectors.toMap(
                    User::getId,
                    UserAssemblingMapper::toContactInfoDto
                ));

        // create result
        return volunteerIdsByPlan.entrySet().stream()
            .map(entry -> {
                Long planId = entry.getKey();
                String planName = planNames.get(planId);
                List<ContactInfoDto> contacts =
                    entry.getValue().stream()
                        .map(contactInfoById::get)
                        .toList();

                return new ShiftPlanContactInfoDto(String.valueOf(planId), planName, contacts);
            })
            .toList();
    }

    @Override
    public EventShiftPlansOverviewDto getEventShiftPlansOverview(long eventId, UUID userId) {
        var event = eventDao.getById(eventId);
        boolean isAdmin = securityHelper.isUserAdmin();
        var volunteer = isAdmin ? volunteerDao.findById(userId) : Optional.of(volunteerDao.getById(userId));

        var eventOverviewDto = EventMapper.toEventDto(event);
        var userRelevantShiftPlans = isAdmin
            ? List.copyOf(event.getShiftPlans())
            : getVolunteerShiftPlans(event, volunteer.orElseThrow());

        // get roles of the user inside this event's shiftplans; Remove equally named roles (from different ShiftPlans)
        var roles = volunteer.stream()
            .flatMap(profile -> profile.getRoles().stream())
            .map(RoleMapper::toRoleDto)
            .filter(roleDto -> userRelevantShiftPlans.stream()
                .anyMatch(shiftPlan -> shiftPlan.getRoles().stream()
                    .anyMatch(role -> role.getName().equals(roleDto.getName()))
                )
            )
            .collect(Collectors.toMap(
                RoleDto::getName,
                r -> r,
                (r1, r2) -> r1,
                LinkedHashMap::new
            ))
            .values();

        return EventShiftPlansOverviewDto.builder()
            .eventOverview(eventOverviewDto)
            .shiftPlans(ShiftPlanMapper.toShiftPlanDto(userRelevantShiftPlans))
            .rewardPoints((int) rewardPointsTransactionDao.sumPointsByVolunteerAndEvent(userId.toString(), eventId))
            .ownEventStatistics(statisticService.getOwnStatisticsOfShiftPlans(userRelevantShiftPlans, userId))
            .overallEventStatistics(statisticService.getOverallEventStatistics(event))
            .roles(roles)
            .build();
    }

    @Override
    @AdminOnly
    @Transactional
    public EventDto createEvent(@NonNull EventModificationDto modificationDto) {
        validateEventModificationDto(modificationDto);
        validateNameUniqueness(modificationDto.getName(), null);

        Event event = EventMapper.toEvent(modificationDto);
        event = eventDao.save(event);

        syncSocialMediaLinks(event, modificationDto);

        eventManagementAudit.record("event", "created", event.getId(), null, eventManagementAudit.snapshot(event));
        publisher.publishEvent(EventEvent.eventCreated(event));
        return EventMapper.toEventDto(event);
    }

    @Override
    @AdminOnly
    @Transactional
    public EventDto updateEvent(long eventId, @NonNull EventModificationDto modificationDto) {
        validateEventModificationDto(modificationDto);
        validateNameUniqueness(modificationDto.getName(), eventId);

        Event event = eventDao.getById(eventId);
        var before = eventManagementAudit.snapshot(event);
        EventMapper.updateEvent(event, modificationDto);
        syncSocialMediaLinks(event, modificationDto);
        eventDao.save(event);

        eventManagementAudit.record("event", "updated", event.getId(), before, eventManagementAudit.snapshot(event));
        publisher.publishEvent(EventEvent.eventUpdated(event));
        return EventMapper.toEventDto(event);
    }

    private void validateNameUniqueness(String name, Long excludeEventId) {
        var eventOpt = eventDao.findByName(name);
        if (eventOpt.isPresent() && (excludeEventId == null || eventOpt.get().getId() != excludeEventId)) {
            throw new BadRequestException("An event with the given name already exists");
        }
    }

    private void validateEventModificationDto(EventModificationDto modificationDto) {
        if (modificationDto.getStartTime().isAfter(modificationDto.getEndTime())) {
            throw new BadRequestException("Event end time must be after start time");
        }

        // validate that rewardPointsRedeemUrl is a valid URL if provided
        String url = modificationDto.getRewardPointsRedeemUrl();
        if (StringUtils.isNotBlank(url)) {
            try {
                URI.create(url).toURL();
            } catch (MalformedURLException _) {
                throw new ValidationException("Reward points redeem URL is not a valid URL");
            }
        }
    }

    @Override
    @AdminOnly
    @Transactional
    public void deleteEvent(long eventId) {
        var event = eventDao.getById(eventId);

        if (event.getShiftPlans() != null) event.getShiftPlans().forEach(plan -> audit.removedWithPlan(plan, "Assignment removed when event was deleted"));
        eventManagementAudit.record("event", "deleted", event.getId(), eventManagementAudit.snapshot(event), null);
        var eventEvent = EventEvent.eventDeleted(event);
        eventDao.delete(event);
        publisher.publishEvent(eventEvent);
    }

    private List<ShiftPlan> getVolunteerShiftPlans(Event event, Volunteer volunteer) {
        var volunteerShiftPlans = volunteer.getVolunteeringPlans();
        var planningShiftPlans = volunteer.getPlanningPlans();

        // filter shiftPlans that the volunteer is part of (volunteerShiftPlans)
        return event.getShiftPlans().stream()
            .filter(shiftPlan -> volunteerShiftPlans.contains(shiftPlan) || planningShiftPlans.contains(shiftPlan))
            .toList();
    }

    private void syncSocialMediaLinks(Event event, EventModificationDto modificationDto) {
        var incoming = SocialLinksParser.parseToDtos(modificationDto.getSocialLinks());

        // IMPORTANT: do not replace managed collection
        var links = event.getSocialMediaLinks();
        if (links == null) {
            links = new ArrayList<>();
            event.setSocialMediaLinks(links);
        }

        var incomingKeys = incoming.stream()
            .map(SocialMediaLinkDto::createKey)
            .collect(Collectors.toSet());

        links.removeIf(l -> !incomingKeys.contains(l.getKey()));

        var existingKeys = links.stream()
            .map(SocialMediaLink::getKey)
            .collect(Collectors.toSet());

        incoming.stream()
            .filter(dto -> existingKeys.add(dto.createKey()))
            .map(dto -> EventMapper.toSocialMediaLink(dto, event))
            .forEach(links::add);
    }
}
