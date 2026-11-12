package at.conrunner.eventcatalog.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import at.conrunner.eventcatalog.audit.CatalogAudit;
import at.conrunner.eventcatalog.error.EventCatalogConflictException;
import at.conrunner.eventcatalog.error.EventCatalogValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    private static final Instant START = Instant.parse("2026-05-12T07:00:00Z");
    private static final Instant END = Instant.parse("2026-05-12T22:00:00Z");

    @Mock
    CatalogAudit audit;
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void create_persistsTheLegacyEventFieldsAndDeduplicatesSocialLinks() {
        UUID eventId = UUID.randomUUID();
        EventData data = data("ShiftControl Fest", START, END, List.of(
            new EventLink(EventLinkType.WEBSITE, "https://shiftcontrol.at"),
            new EventLink(EventLinkType.WEBSITE, " https://shiftcontrol.at ")
        ));
        when(eventRepository.existsByNameIgnoreCase(data.name())).thenReturn(false);
        when(eventRepository.save(any(CatalogEventEntity.class))).thenAnswer(invocation -> {
            CatalogEventEntity event = invocation.getArgument(0);
            event.setId(eventId);
            return event;
        });

        EventDetails result = eventService.create(data);

        ArgumentCaptor<CatalogEventEntity> eventCaptor = ArgumentCaptor.forClass(CatalogEventEntity.class);
        verify(eventRepository).save(eventCaptor.capture());
        CatalogEventEntity saved = eventCaptor.getValue();
        assertEquals("ShiftControl Fest", saved.getName());
        assertEquals("Short description", saved.getShortDescription());
        assertEquals("Long description", saved.getLongDescription());
        assertEquals(START, saved.getStartTime());
        assertEquals(END, saved.getEndTime());
        assertEquals("https://shiftcontrol.at/rewards", saved.getRewardPointsRedeemUrl());
        assertEquals(1, saved.getLinks().size());
        assertEquals(eventId, result.id());
        assertEquals(List.of(new EventLink(EventLinkType.WEBSITE, "https://shiftcontrol.at")), result.links());
    }

    @Test
    void create_rejectsAnInvalidTimeRangeBeforeTouchingPersistence() {
        EventCatalogValidationException exception = assertThrows(EventCatalogValidationException.class,
            () -> new EventData("ShiftControl Fest", "Short description", "Long description", END, START,
                "https://shiftcontrol.at/rewards", List.of()));

        assertEquals("Event end time must be after start time", exception.getMessage());
    }

    @Test
    void create_rejectsAnInvalidRewardRedemptionUrlBeforeTouchingPersistence() {
        assertThrows(EventCatalogValidationException.class, () -> new EventData(
            "ShiftControl Fest", "Short description", "Long description", START, END, "not a URL", List.of()));
    }

    @Test
    void eventLink_requiresAnAbsoluteUrl() {
        assertThrows(EventCatalogValidationException.class, () -> new EventLink(EventLinkType.WEBSITE, "not a URL"));
    }

    @Test
    void create_rejectsMissingEventData() {
        assertThrows(EventCatalogValidationException.class, () -> eventService.create(null));
    }

    @Test
    void create_rejectsAnEventNameThatDiffersOnlyByCase() {
        EventData data = data("ShiftControl Fest", START, END, List.of());
        when(eventRepository.existsByNameIgnoreCase(data.name())).thenReturn(true);

        assertThrows(EventCatalogConflictException.class, () -> eventService.create(data));
    }

    @Test
    void update_allowsTheCurrentEventNameButRejectsAnotherEventsName() {
        UUID eventId = UUID.randomUUID();
        CatalogEventEntity current = entity(eventId, "ShiftControl Fest", START, END);
        EventData unchangedName = data(" shiftcontrol fest ", START, END, List.of());
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(current));
        when(eventRepository.save(current)).thenReturn(current);

        eventService.update(eventId, unchangedName);

        UUID otherId = UUID.randomUUID();
        CatalogEventEntity other = entity(otherId, "Another Event", START, END);
        when(eventRepository.existsByNameIgnoreCase(other.getName())).thenReturn(true);
        EventData duplicate = data("Another Event", START, END, List.of());

        assertThrows(EventCatalogConflictException.class, () -> eventService.update(eventId, duplicate));
    }

    @Test
    void findOpen_returnsOnlyEventsWhoseEndIsNotInThePast() {
        Instant now = Instant.parse("2026-05-12T12:00:00Z");
        CatalogEventEntity open = entity(UUID.randomUUID(), "Open", START, now);
        when(eventRepository.findAllByEndTimeGreaterThanEqualOrderByStartTimeAsc(now)).thenReturn(List.of(open));

        List<EventDetails> result = eventService.findOpen(now);

        assertEquals(List.of("Open"), result.stream().map(EventDetails::name).toList());
    }

    private static EventData data(String name, Instant start, Instant end, List<EventLink> links) {
        return new EventData(name, "Short description", "Long description", start, end,
            "https://shiftcontrol.at/rewards", links);
    }

    private static CatalogEventEntity entity(UUID id, String name, Instant start, Instant end) {
        CatalogEventEntity event = new CatalogEventEntity();
        event.setId(id);
        event.setName(name);
        event.setStartTime(start);
        event.setEndTime(end);
        return event;
    }
}
