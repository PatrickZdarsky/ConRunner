package at.conrunner.schedule.event;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import at.conrunner.platform.exception.ConflictException;
import at.conrunner.platform.exception.NotFoundException;
import at.conrunner.platform.exception.ValidationException;
import at.conrunner.schedule.audit.CatalogAudit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {
    private final CatalogAudit audit;
    private final EventRepository eventRepository;
    private final Clock clock;

    public EventService(EventRepository eventRepository, CatalogAudit audit, Clock clock) {
        this.audit = audit;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Transactional
    public EventDetails create(EventData data) {
        if (data == null) {
            throw new ValidationException("Event data is required");
        }
        if (eventRepository.existsByNameIgnoreCase(data.name())) {
            throw new ConflictException("An event with this name already exists");
        }

        CatalogEventEntity event = new CatalogEventEntity();
        apply(event, data);
        event = eventRepository.save(event);
        audit.record("event", "created", event.getId(), null, snapshot(event));
        return toDetails(event);
    }

    @Transactional
    public EventDetails update(UUID eventId, EventData data) {
        CatalogEventEntity event = getEntity(eventId);
        if (data == null) {
            throw new ValidationException("Event data is required");
        }
        if (!event.getName().equalsIgnoreCase(data.name()) && eventRepository.existsByNameIgnoreCase(data.name())) {
            throw new ConflictException("An event with this name already exists");
        }
        if (event.getArchivedAt() != null && data.endTime().isAfter(clock.instant())) {
            throw new ConflictException("Unarchive the event before moving its end into the future");
        }
        var before = snapshot(event);
        apply(event, data);
        event = eventRepository.save(event);
        audit.record("event", "updated", event.getId(), before, snapshot(event));
        return toDetails(event);
    }

    public EventDetails get(UUID eventId) {
        return toDetails(getEntity(eventId));
    }

    public List<EventDetails> search(String name) {
        return search(name, false);
    }

    public List<EventDetails> search(String name, boolean archived) {
        String term = name == null ? "" : name.trim();
        var events = archived
            ? eventRepository.findAllByArchivedAtIsNotNullAndNameContainingIgnoreCaseOrderByStartTimeAsc(term)
            : eventRepository.findAllByArchivedAtIsNullAndNameContainingIgnoreCaseOrderByStartTimeAsc(term);
        return events.stream()
            .map(EventService::toDetails)
            .toList();
    }

    public List<EventDetails> findOpen(Instant now) {
        if (now == null) {
            throw new ValidationException("Open-event time is required");
        }
        return eventRepository.findAllByArchivedAtIsNullAndEndTimeGreaterThanEqualOrderByStartTimeAsc(now).stream()
            .map(EventService::toDetails)
            .toList();
    }

    @Transactional
    public EventDetails archive(UUID eventId) {
        var event = getEntity(eventId);
        if (event.getArchivedAt() == null) {
            Instant now = clock.instant();
            if (event.getEndTime().isAfter(now)) {
                throw new ConflictException("Only events that have ended can be archived");
            }
            var before = snapshot(event);
            event.setArchivedAt(now);
            audit.record("event", "archived", eventId, before, snapshot(event));
        }
        return toDetails(event);
    }

    @Transactional
    public EventDetails unarchive(UUID eventId) {
        var event = getEntity(eventId);
        if (event.getArchivedAt() != null) {
            var before = snapshot(event);
            event.setArchivedAt(null);
            audit.record("event", "unarchived", eventId, before, snapshot(event));
        }
        return toDetails(event);
    }

    @Transactional
    public void delete(UUID eventId) {
        var event = getEntity(eventId);
        audit.record("event", "deleted", event.getId(), snapshot(event), null);
        eventRepository.delete(event);
    }

    private CatalogEventEntity getEntity(UUID eventId) {
        if (eventId == null) {
            throw new ValidationException("Event ID is required");
        }
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
    }

    private static CatalogAudit.Snapshot snapshot(CatalogEventEntity e) {
        return new CatalogAudit.Snapshot(e.getName(), CatalogAudit.value(e.getStartTime()), CatalogAudit.value(e.getEndTime()),
            null, null, CatalogAudit.value(e.getArchivedAt()));
    }

    private static void apply(CatalogEventEntity event, EventData data) {
        event.setName(data.name());
        event.setShortDescription(data.shortDescription());
        event.setLongDescription(data.longDescription());
        event.setStartTime(data.startTime());
        event.setEndTime(data.endTime());
        event.setRewardPointsRedeemUrl(data.rewardPointsRedeemUrl());
        event.replaceLinks(data.links());
    }

    private static EventDetails toDetails(CatalogEventEntity event) {
        return new EventDetails(event.getId(), event.getName(), event.getShortDescription(), event.getLongDescription(), event.getStartTime(),
            event.getEndTime(), event.getArchivedAt(), event.getRewardPointsRedeemUrl(), event.getLinks().stream()
                .map(link -> new EventLink(link.getType(), link.getUrl()))
                .toList());
    }

}
