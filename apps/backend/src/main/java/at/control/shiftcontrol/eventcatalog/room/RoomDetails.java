package at.control.shiftcontrol.eventcatalog.room;

import java.util.UUID;

/** Immutable room data returned by the room application service. */
public record RoomDetails(UUID id, UUID eventId, String name, String description, String url, boolean readOnly) {
}
