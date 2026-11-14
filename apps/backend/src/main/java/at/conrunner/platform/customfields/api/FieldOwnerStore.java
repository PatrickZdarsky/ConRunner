package at.conrunner.platform.customfields.api;

import at.conrunner.platform.exception.NotFoundException;
import java.util.Map;
import java.util.UUID;

/** Domain-owned storage for one custom-field owner type. */
public interface FieldOwnerStore {
    /**
     * Identifies the store for service registration and lookup.
     *
     * @return the entity type handled by this store
     */
    FieldEntityType entityType();

    /**
     * Resolves an existing owner's event scope.
     *
     * @param ownerId domain ID of the owner
     * @param lock whether to hold a write lock on the owner in the current transaction
     * @return the event ID, which is the owner ID for event owners
     * @throws NotFoundException if the owner does not exist
     */
    UUID eventId(UUID ownerId, boolean lock);

    /**
     * Reads all stored values for an owner. The caller resolves the owner before reading.
     *
     * @param ownerId domain ID of the owner
     * @return stored JSON strings keyed by definition ID
     */
    Map<UUID, String> read(UUID ownerId);

    /**
     * Inserts or replaces a value. The caller resolves and locks the owner in the same transaction.
     *
     * @param ownerId domain ID of the owner
     * @param definitionId ID of the applicable definition
     * @param json validated, serialized JSON value
     */
    void put(UUID ownerId, UUID definitionId, String json);

    /**
     * Deletes a stored value if present. The caller resolves and locks the owner in the same transaction.
     *
     * @param ownerId domain ID of the owner
     * @param definitionId ID of the applicable optional definition
     */
    void remove(UUID ownerId, UUID definitionId);
}
