package at.conrunner.audit.api;

import java.util.List;
import org.springframework.stereotype.Service;

/** Adds request context to explicitly approved business facts. */
@Service
public class AuditRecorder {
    private final AuditWriter writer;
    private final AuditContext context;
    public AuditRecorder(AuditWriter writer, AuditContext context) {
        this.writer = writer; this.context = context;
    }
    public void record(String module, String action, String subjectType, String subjectId,
                       String subjectLabel, String summary, List<AuditEntry.Change> changes) {
        recordAs(context.actor(), module, action, new Subject(subjectType, subjectId, subjectLabel), summary, changes);
    }
    public record Subject(String type, String id, String label) {}

    /** Background callers must supply an explicit system actor. */
    public void recordAs(AuditEntry.Actor actor, String module, String action, Subject subject,
                         String summary, List<AuditEntry.Change> changes) {
        writer.record(new AuditEntry(actor, module, action, subject.type(), subject.id(), subject.label(),
            summary, context.correlationId(), changes));
    }
}
