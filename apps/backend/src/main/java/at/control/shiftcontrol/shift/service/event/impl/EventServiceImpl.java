package at.control.shiftcontrol.shift.service.event.impl;

import at.control.shiftcontrol.shift.audit.EventManagementAudit;
import org.springframework.transaction.annotation.Transactional;
import at.control.shiftcontrol.shift.audit.AssignmentAudit;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.SocialMediaLink;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.event.events.EventEvent;
import at.control.shiftcontrol.platform.exception.BadRequestException;
import at.control.shiftcontrol.platform.exception.ValidationException;
import at.control.shiftcontrol.identity.AdminOnly;
import at.control.shiftcontrol.shift.auth.CurrentShiftActor;
import at.control.shiftcontrol.shift.dao.EventDao;
import at.control.shiftcontrol.shift.dao.RewardPointsTransactionDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.event.EventDto;
import at.control.shiftcontrol.shift.dto.event.EventModificationDto;
import at.control.shiftcontrol.shift.dto.event.EventSearchDto;
import at.control.shiftcontrol.shift.dto.event.EventShiftPlansOverviewDto;
import at.control.shiftcontrol.shift.dto.event.SocialMediaLinkDto;
import at.control.shiftcontrol.shift.dto.role.RoleDto;
import at.control.shiftcontrol.shift.dto.rows.PlanVolunteerIdRow;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanContactInfoDto;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanDto;
import at.control.shiftcontrol.shift.dto.user.ContactInfoDto;
import at.control.shiftcontrol.shift.mapper.EventMapper;
import at.control.shiftcontrol.shift.mapper.RoleMapper;
import at.control.shiftcontrol.shift.mapper.ShiftPlanMapper;
import at.control.shiftcontrol.shift.mapper.UserAssemblingMapper;
import at.control.shiftcontrol.shift.service.StatisticService;
import at.control.shiftcontrol.shift.service.event.EventService;
import at.control.shiftcontrol.shift.util.SecurityHelper;
import at.control.shiftcontrol.shift.util.SocialLinksParser;

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
        return ShiftPlanMapper.toShiftPlanDto(getUserRelatedShiftPlanEntitiesOfEvent(eventId, volunteer));
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
        var volunteer = volunteerDao.getById(userId);

        var eventOverviewDto = EventMapper.toEventDto(event);
        var userRelevantShiftPlans = getUserRelatedShiftPlanEntitiesOfEvent(eventId, volunteer);

        // get roles of the user inside this event's shiftplans; Remove equally named roles (from different ShiftPlans)
        var roles = volunteer.getRoles().stream()
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

    private List<ShiftPlan> getUserRelatedShiftPlanEntitiesOfEvent(long eventId, Volunteer volunteer) {
        var event = eventDao.getById(eventId);
        var shiftPlans = event.getShiftPlans();

        // skip filtering for admin users
        if (securityHelper.isUserAdmin()) {
            return shiftPlans.stream().toList();
        }

        var volunteerShiftPlans = volunteer.getVolunteeringPlans();
        var planningShiftPlans = volunteer.getPlanningPlans();

        // filter shiftPlans that the volunteer is part of (volunteerShiftPlans)
        return shiftPlans.stream()
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
