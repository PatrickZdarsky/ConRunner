package at.conrunner.platform.customfields.internal;

import at.conrunner.platform.customfields.api.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

/** Platform-owned definition row with JSONB configuration and a JPA version for label writes. */
@lombok.Getter
@lombok.Setter
@Entity
@Table(name = "platform_custom_field_definition")
class DefinitionEntity {
    @Id private UUID id;
    @Column(name = "field_key", nullable = false) private String key;
    @Column(nullable = false) private String label;
    @Enumerated(EnumType.STRING) @Column(name = "entity_type", nullable = false) private FieldEntityType entityType;
    @Enumerated(EnumType.STRING) @Column(name = "value_type", nullable = false) private FieldValueType valueType;
    @Column(nullable = false) private boolean multiple;
    @Column(nullable = false) private boolean required;
    @Column(name = "event_scope_id") private UUID eventScopeId;
    @Column(name = "built_in", nullable = false) private boolean builtIn;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false) private String configuration;
    @Version private long version;
}
