package at.conrunner.platform.customfields.api;

import java.util.UUID;

/** Domain check for user references made by a particular owner. */
@FunctionalInterface
public interface FieldReferenceAccess {
    /**
     * Checks the referenced user's existence and whether the owner may reference that user.
     *
     * @param eventId event ID of the owner
     * @param userId referenced user ID
     * @return whether the user exists and the reference is permitted
     */
    boolean canReferenceUser(UUID eventId, UUID userId);
}
