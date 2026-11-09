package at.control.shiftcontrol.shift.sync.pretalx;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import at.control.shiftcontrol.shift.entity.Activity;
import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.Location;
import at.control.shiftcontrol.shift.lib.event.events.ActivityEvent;
import at.control.shiftcontrol.shift.lib.event.events.EventEvent;
import at.control.shiftcontrol.shift.lib.event.events.LocationEvent;
import at.shiftcontrol.pretalxclient.model.Room;
import at.shiftcontrol.pretalxclient.model.Submission;
import at.shiftcontrol.pretalxclient.model.TalkSlot;
import at.control.shiftcontrol.shift.dao.ActivityDao;
import at.control.shiftcontrol.shift.dao.EventDao;
import at.control.shiftcontrol.shift.dao.LocationDao;
import at.control.shiftcontrol.shift.dto.event.EventSearchDto;
import at.control.shiftcontrol.shift.dto.location.LocationSearchDto;

@Slf4j
@Service
public class PretalxSyncService {
    private static final String ACTING_USERID = "pretalx-sync";

    private final ApplicationEventPublisher eventPublisher;
    private final PretalxApiKeyLoader apiKeyLoader;
    private final PretalxDataGatherer dataGatherer;

    private final EventDao eventDao;
    private final LocationDao locationDao;
    private final ActivityDao activityDao;

    private final String locale;

    public PretalxSyncService(ApplicationEventPublisher eventPublisher, PretalxApiKeyLoader apiKeyLoader, PretalxDataGatherer dataGatherer, EventDao eventDao,
                              LocationDao locationDao, ActivityDao activityDao, @Value("${pretalx.locale}") String locale) {
        this.eventPublisher = eventPublisher;
        this.apiKeyLoader = apiKeyLoader;
        this.dataGatherer = dataGatherer;
        this.eventDao = eventDao;
        this.locationDao = locationDao;
        this.activityDao = activityDao;
        this.locale = locale;
    }

    @Scheduled(cron = "${pretalx.sync.cron}")
    @Transactional
    public void syncAll() {
        apiKeyLoader.refreshApiKeys();
        var accessibleEvents = apiKeyLoader.accessibleEventSlugs();
        log.debug("Starting Pretalx synchronization for {} events", accessibleEvents.size());
        var now = Instant.now();

        for (var eventSlug : accessibleEvents) {
            var ptEvent = dataGatherer.gatherEventDetails(eventSlug);
            var event = syncEvent(ptEvent);
            var locationLookupTable = syncRooms(event.getId(), ptEvent);
            var activities = syncActivities(event, ptEvent.getSlug(), locationLookupTable);

            log.debug("Synchronized event: {} (ID: {}) with {} locations and {} activities",
                event.getName(),
                event.getId(),
                locationLookupTable.size(),
                activities.size());
        }
        var timeDiff = Instant.now().toEpochMilli() - now.toEpochMilli();
        log.debug("Pretalx synchronization completed in {}ms. Total events synchronized: {}", timeDiff, accessibleEvents.size());
    }

    private Event syncEvent(at.shiftcontrol.pretalxclient.model.Event ptEvent) {
        if (ptEvent.getName().get(locale) == null) {
            throw new PretalxSyncException("Event name is missing for locale: " + locale + " in event: " + ptEvent.getSlug());
        }
        String eventName = ptEvent.getName().get(locale);
        var startTime = ptEvent.getDateFrom().atStartOfDay(ZoneId.systemDefault()).toInstant();
        var endTime = ptEvent.getDateTo().atStartOfDay(ZoneId.systemDefault()).toInstant();

        var existingEvents = eventDao.search(EventSearchDto.builder().name(eventName).build());
        if (existingEvents.isEmpty()) {
            log.info("Creating new event: {} (Slug: {})", eventName, ptEvent.getSlug());
            //Create new event
            var event = Event.builder()
                .name(eventName)
                .startTime(startTime)
                .endTime(endTime)
                .build();
            event = eventDao.save(event);
            eventPublisher.publishEvent(EventEvent.eventCreated(event).withActingUserId(ACTING_USERID));
            return event;
        } else if (existingEvents.size() == 1) {
            //Update existing event
            var existingEvent = existingEvents.get(0);
            if (!existingEvent.getStartTime().equals(startTime)
                || !existingEvent.getEndTime().equals(endTime)) {
                log.info("Updating existing event: {} (ID: {})", eventName, existingEvent.getId());

                existingEvent.setName(eventName);
                existingEvent.setStartTime(startTime);
                existingEvent.setEndTime(endTime);
                existingEvent = eventDao.save(existingEvent);
                eventPublisher.publishEvent(EventEvent.eventUpdated(existingEvent).withActingUserId(ACTING_USERID));
                return existingEvent;
            }
            return existingEvent;
        } else {
            //Todo: Better way to identify events?
            throw new PretalxSyncException("Multiple events found with name: " + eventName);
        }
    }

    private Map<Integer, Location> syncRooms(long eventId, at.shiftcontrol.pretalxclient.model.Event ptEvent) {
        var rooms = dataGatherer.gatherRooms(ptEvent.getSlug());
        var event = eventDao.getById(eventId);

        var locations = new HashMap<Integer, Location>();
        for (var room : rooms) {
            var location = syncRoom(event, room);
            locations.put(room.getId(), location);
        }

        return locations;
    }

    private Location syncRoom(Event event, Room room) {
        var roomName = getStringForLocale(room.getName());
        var locations = locationDao.search(event.getId(), LocationSearchDto.builder().name(roomName).build());

        if (locations.isEmpty()) {
            log.info("Creating new location: {} for event: {} (ID: {})", roomName, event.getName(), event.getId());
            //Create new location
            var location = Location.builder()
                .name(roomName)
                .event(event)
                .readOnly(true)
                .description(room.getDescription() != null ? getStringForLocale(room.getDescription()) : null)
                .build();
            location = locationDao.save(location);
            eventPublisher.publishEvent(LocationEvent.locationCreated(location).withActingUserId(ACTING_USERID));
            return location;
        } else if (locations.size() == 1) {
            //Update existing location
            var location = locations.iterator().next();
            var description = room.getDescription() != null ? getStringForLocale(room.getDescription()) : null;

            if (!location.getName().equals(roomName)
                || (description != null && !description.equals(location.getDescription()))) {
                log.info("Updating existing location: {} (ID: {}) for event: {} (ID: {})", roomName, location.getId(), event.getName(), event.getId());
                location.setName(roomName);
                location.setDescription(description);
                location = locationDao.save(location);
                eventPublisher.publishEvent(LocationEvent.locationUpdated(location).withActingUserId(ACTING_USERID));
                return location;
            }
            return location;
        } else {
            throw new PretalxSyncException("Multiple locations found with name: " + roomName);
        }
    }

    private List<Activity> syncActivities(Event event, String ptEventSlug, Map<Integer, Location> locationLookupTable) {
        var submissions = dataGatherer.gatherSubmissions(ptEventSlug);
        var slots = dataGatherer.gatherSlots(ptEventSlug);

        var activities = new ArrayList<Activity>(slots.size());
        for (var slot : slots) {
            //Find submission for slot
            var submission = submissions.stream()
                .filter(sub -> sub.getCode().equals(slot.getSubmission()))
                .findFirst().orElseThrow(() -> new PretalxSyncException("No submission found for slot with submission code: " + slot.getSubmission()));

            var activity = syncActivity(event, ptEventSlug, submission, slot, locationLookupTable);
            activities.add(activity);
        }
        return activities;
    }

    private Activity syncActivity(Event event, String ptEventSlug, Submission submission, TalkSlot slot, Map<Integer, Location> locationLookupTable) {
        var activityOpt = activityDao.findByEventAndName(event.getId(), submission.getTitle());
        var location = locationLookupTable.get(slot.getRoom());
        if (location == null) {
            throw new PretalxSyncException("No location found for room ID: " + slot.getRoom());
        }
        if (slot.getStart() == null || slot.getEnd() == null) {
            throw new PretalxSyncException("Slot start or end time is null for slot with ID: " + slot.getId());
        }

        var startTime = slot.getStart().toInstant();
        var endTime = slot.getEnd().toInstant();

        var internalNote = dataGatherer.getInternalNoteForSubmission(ptEventSlug, submission.getCode());
        var descriptionText = submission.getDescription();
        if (internalNote != null && !internalNote.isEmpty()) {
            descriptionText = (descriptionText != null ? descriptionText + "\n\n " : "") + "Internal Note: " + internalNote;
        }

        Activity activity;
        if (activityOpt.isEmpty()) {
            //Create new activity
            activity = Activity.builder()
                .event(event)
                .name(submission.getTitle())
                .description(descriptionText)
                .startTime(startTime)
                .endTime(endTime)
                .location(location)
                .readOnly(true)
                .build();
            log.info("Creating new activity: {} for event: {} (ID: {})", activity.getName(), event.getName(), event.getId());

            activity = activityDao.save(activity);
            eventPublisher.publishEvent(ActivityEvent.activityCreated(activity).withActingUserId(ACTING_USERID));
        } else {
            activity = activityOpt.get();

            if (!activity.getStartTime().equals(startTime)
                || !activity.getEndTime().equals(endTime)
                || activity.getLocation().getId() != location.getId()
                || (submission.getDescription() != null && !submission.getDescription().equals(descriptionText))) {
                //Update existing activity
                activity.setStartTime(startTime);
                activity.setEndTime(endTime);
                activity.setLocation(location);
                activity.setDescription(descriptionText);
                activity = activityDao.save(activity);
                eventPublisher.publishEvent(ActivityEvent.activityUpdated(activity).withActingUserId(ACTING_USERID));
                log.info("Updating existing activity: {} (ID: {}) for event: {} (ID: {})", activity.getName(), activity.getId(), event.getName(),
                    event.getId());
            }
        }
        return activity;
    }

    private String getStringForLocale(@NonNull Map<String, String> map) {
        if (map.isEmpty() || !map.containsKey(locale)) {
            return "";
        }

        return map.get(locale);
    }
}
