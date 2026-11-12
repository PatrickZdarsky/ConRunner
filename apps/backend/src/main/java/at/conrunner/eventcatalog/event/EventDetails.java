package at.conrunner.eventcatalog.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Immutable event data returned by the event-catalogue application service. */
public record EventDetails(
    UUID id,
    String name,
    String shortDescription,
    String longDescription,
    Instant startTime,
    Instant endTime,
    Instant archivedAt,
    String rewardPointsRedeemUrl,
    List<EventLink> links
) {
}
