package at.conrunner.platform.customfields.internal;

import at.conrunner.platform.customfields.api.FieldEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

interface DefinitionRepository extends JpaRepository<DefinitionEntity, UUID> {
    /** Finds global definitions and definitions for the supplied event, in key order. */
    @Query("select d from DefinitionEntity d where d.entityType = :type and (d.eventScopeId is null or d.eventScopeId = :eventId) order by d.key")
    List<DefinitionEntity> applicable(FieldEntityType type, UUID eventId);

    /** Checks every scope when deciding whether a global key is available. */
    boolean existsByEntityTypeAndKey(FieldEntityType type, String key);
}
