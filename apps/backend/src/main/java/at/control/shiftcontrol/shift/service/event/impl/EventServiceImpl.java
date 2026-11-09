package at.control.shiftcontrol.shift.service.event.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import at.shiftcontrol.lib.entity.Event;
import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.lib.entity.SocialMediaLink;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.lib.event.events.EventEvent;
import at.shiftcontrol.lib.exception.BadRequestException;
import at.shiftcontrol.lib.exception.ValidationException;
import at.shiftcontrol.shiftservice.annotation.AdminOnly;
import at.shiftcontrol.shiftservice.auth.ApplicationUserProvider;
import at.shiftcontrol.shiftservice.dao.EventDao;
import at.shiftcontrol.shiftservice.dao.RewardPointsTransactionDao;
import at.shiftcontrol.shiftservice.dao.userprofile.VolunteerDao;
import at.shiftcontrol.shiftservice.dto.event.EventDto;
import at.shiftcontrol.shiftservice.dto.event.EventModificationDto;
import at.shiftcontrol.shiftservice.dto.event.EventSearchDto;
import at.shiftcontrol.shiftservice.dto.event.EventShiftPlansOverviewDto;
import at.shiftcontrol.shiftservice.dto.event.SocialMediaLinkDto;
import at.shiftcontrol.shiftservice.dto.role.RoleDto;
import at.shiftcontrol.shiftservice.dto.rows.PlanVolunteerIdRow;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanContactInfoDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanDto;
import at.shiftcontrol.shiftservice.dto.user.ContactInfoDto;
import at.shiftcontrol.shiftservice.mapper.EventMapper;
import at.shiftcontrol.shiftservice.mapper.RoleMapper;
import at.shiftcontrol.shiftservice.mapper.ShiftPlanMapper;
import at.shiftcontrol.shiftservice.mapper.UserAssemblingMapper;
import at.shiftcontrol.shiftservice.service.StatisticService;
import at.shiftcontrol.shiftservice.service.event.EventService;
import at.shiftcontrol.shiftservice.userdirectory.UserInfo;
import at.shiftcontrol.shiftservice.userdirectory.UserLookupService;
import at.shiftcontrol.shiftservice.util.SecurityHelper;
import at.shiftcontrol.shiftservice.util.SocialLinksParser;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventDao eventDao;
    private final VolunteerDao volunteerDao;
    private final RewardPointsTransactionDao rewardPointsTransactionDao;
    private final StatisticService statisticService;
    private final ApplicationUserProvider userProvider;
    private final SecurityHelper securityHelper;
    private final ApplicationEventPublisher publisher;
    private final UserLookupService userDirectoryService;

    @Override
    public EventDto getEvent(long eventId) {
        var event = eventDao.getById(eventId);
        securityHelper.assertUserIsAllowedToAccessEvent(event);

        return EventMapper.toEventDto(event);
    }

    @Override
    public List<EventDto> search(EventSearchDto searchDto) {
        var filteredEvents = eventDao.search(searchDto);
        var currentUser = userProvider.getCurrentUser();

        // skip filtering for admin users
        if (securityHelper.isUserAdmin(currentUser)) {
            return EventMapper.toEventDto(filteredEvents);
        }
        String userId = currentUser.getUserId();

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
    public Collection<EventDto> getAllOpenEvents(String currentUser) {
        if (securityHelper.isUserAdmin()) {
            return EventMapper.toEventDto(eventDao.getAllOpenEvents());
        }
        return EventMapper.toEventDto(eventDao.getAllOpenEventsForUser(currentUser));
    }

    @Override
    public List<ShiftPlanDto> getUserRelatedShiftPlansOfEvent(long eventId, String userId) {
        var volunteer = volunteerDao.getById(userId);
        return ShiftPlanMapper.toShiftPlanDto(getUserRelatedShiftPlanEntitiesOfEvent(eventId, volunteer));
    }

    @Override
    public Collection<ShiftPlanContactInfoDto> getPlannerContactInfo(long eventId, String userId) {
        // check access
        var event = eventDao.getById(eventId);
        securityHelper.assertUserIsAllowedToAccessEvent(event);

        // get planner IDs of users plans
        Collection<PlanVolunteerIdRow> rows = eventDao.getPlannersForEventAndUser(eventId, userId);

        // Resolve all planner contacts from Shiftservice's local user directory.
        Set<String> allPlannerIds =
            rows.stream()
                .map(PlanVolunteerIdRow::getVolunteerId)
                .collect(Collectors.toSet());
        var planners = userDirectoryService.getUserByIds(allPlannerIds);

        // create maps to lookup userIds per plan, planNames and planner-userRepresentations
        Map<Long, List<String>> volunteerIdsByPlan =
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
        Map<String, ContactInfoDto> contactInfoById =
            planners.stream()
                .collect(Collectors.toMap(
                    UserInfo::id,
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
    public EventShiftPlansOverviewDto getEventShiftPlansOverview(long eventId, String userId) {
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
            .rewardPoints((int) rewardPointsTransactionDao.sumPointsByVolunteerAndEvent(userId, eventId))
            .ownEventStatistics(statisticService.getOwnStatisticsOfShiftPlans(userRelevantShiftPlans, userId))
            .overallEventStatistics(statisticService.getOverallEventStatistics(event))
            .roles(roles)
            .build();
    }

    @Override
    @AdminOnly
    public EventDto createEvent(@NonNull EventModificationDto modificationDto) {
        validateEventModificationDto(modificationDto);
        validateNameUniqueness(modificationDto.getName(), null);

        Event event = EventMapper.toEvent(modificationDto);
        event = eventDao.save(event);

        syncSocialMediaLinks(event, modificationDto);

        publisher.publishEvent(EventEvent.eventCreated(event));
        return EventMapper.toEventDto(event);
    }

    @Override
    @AdminOnly
    public EventDto updateEvent(long eventId, @NonNull EventModificationDto modificationDto) {
        validateEventModificationDto(modificationDto);
        validateNameUniqueness(modificationDto.getName(), eventId);

        Event event = eventDao.getById(eventId);
        EventMapper.updateEvent(event, modificationDto);
        syncSocialMediaLinks(event, modificationDto);
        eventDao.save(event);

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
            } catch (MalformedURLException e) {
                throw new ValidationException("Reward points redeem URL is not a valid URL");
            }
        }
    }

    @Override
    @AdminOnly
    public void deleteEvent(long eventId) {
        var event = eventDao.getById(eventId);

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
