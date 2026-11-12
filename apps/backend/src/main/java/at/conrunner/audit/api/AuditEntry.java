package at.conrunner.audit.api;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Explicit, sanitized audit facts. Callers must approve every value; never pass secrets or raw payloads. */
public record AuditEntry(Actor actor, String module, String action, String subjectType, String subjectId,
                         String subjectLabel, String summary, String correlationId, List<Change> changes) {
    public AuditEntry {
        Objects.requireNonNull(actor, "actor");
        require(module, 64); require(action, 96); require(subjectType, 64); require(subjectId, 128);
        require(summary, 512); optional(subjectLabel, 255); optional(correlationId, 128);
        changes = List.copyOf(Objects.requireNonNull(changes, "changes"));
        if (changes.size() > 50) throw new IllegalArgumentException("Too many audit changes");
    }
    public enum ActorKind { HUMAN, MACHINE, SYSTEM }
    public record Actor(ActorKind kind, UUID userId, String identifier, String label) {
        public Actor {
            Objects.requireNonNull(kind, "kind");
            if ((kind == ActorKind.HUMAN) != (userId != null)) throw new IllegalArgumentException("Human actors require a user ID; other actors must not have one");
            if (kind != ActorKind.HUMAN) require(identifier, 128); else optional(identifier, 128);
            optional(label, 255);
        }
    }
    /** Values are approved, human-readable scalar snapshots, not serialized objects. Null means absent. */
    public record Change(String field, String before, String after) {
        public Change { require(field, 64); optional(before, 1024); optional(after, 1024); }
    }
    private static void require(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException("Invalid audit value");
    }
    private static void optional(String value, int max) {
        if (value != null && value.length() > max) throw new IllegalArgumentException("Audit value too long");
    }
}
