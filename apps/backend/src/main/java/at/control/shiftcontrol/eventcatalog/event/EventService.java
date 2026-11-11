package at.control.shiftcontrol.eventcatalog.event;

import java.time.Instant;
import java.util.List;
import at.control.shiftcontrol.eventcatalog.audit.CatalogAudit;
import java.util.UUID;

import at.control.shiftcontrol.eventcatalog.error.EventCatalogConflictException;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogNotFoundException;
import at.control.shiftcontrol.eventcatalog.error.EventCatalogValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {
    private final CatalogAudit audit;
    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository, CatalogAudit audit) {
        this.audit = audit;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public EventDetails create(EventData data) {
        if (data == null) {
            throw new EventCatalogValidationException("Event data is required");
        }
        if (eventRepository.existsByNameIgnoreCase(data.name())) {
            throw new EventCatalogConflictException("An event with this name already exists");
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
            throw new EventCatalogValidationException("Event data is required");
        }
        if (!event.getName().equalsIgnoreCase(data.name()) && eventRepository.existsByNameIgnoreCase(data.name())) {
            throw new EventCatalogConflictException("An event with this name already exists");
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
        if (name == null || name.isBlank()) {
            return eventRepository.findAll().stream().map(EventService::toDetails).toList();
        }
        return eventRepository.findAllByNameContainingIgnoreCaseOrderByStartTimeAsc(name.trim()).stream()
            .map(EventService::toDetails)
            .toList();
    }

    public List<EventDetails> findOpen(Instant now) {
        if (now == null) {
            throw new EventCatalogValidationException("Open-event time is required");
        }
        return eventRepository.findAllByEndTimeGreaterThanEqualOrderByStartTimeAsc(now).stream()
            .map(EventService::toDetails)
            .toList();
    }

    @Transactional
    public void delete(UUID eventId) {
        var event = getEntity(eventId);
        audit.record("event", "deleted", event.getId(), snapshot(event), null);
        eventRepository.delete(event);
    }

    private CatalogEventEntity getEntity(UUID eventId) {
        if (eventId == null) {
            throw new EventCatalogValidationException("Event ID is required");
        }
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new EventCatalogNotFoundException("Event not found: " + eventId));
    }

    private static CatalogAudit.Snapshot snapshot(CatalogEventEntity e) {
        return new CatalogAudit.Snapshot(e.getName(), CatalogAudit.value(e.getStartTime()), CatalogAudit.value(e.getEndTime()), null, null);
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
            event.getEndTime(), event.getRewardPointsRedeemUrl(), event.getLinks().stream()
                .map(link -> new EventLink(link.getType(), link.getUrl()))
                .toList());
    }

}
