package at.conrunner.eventcatalog.audit;

import at.conrunner.audit.api.AuditEntry;
import at.conrunner.audit.api.AuditRecorder;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CatalogAudit {
    private final AuditRecorder recorder;
    public CatalogAudit(AuditRecorder recorder) { this.recorder = recorder; }
    public record Snapshot(String name, String start, String end, String parent, String location) {}
    public void record(String type, String operation, UUID id, Snapshot before, Snapshot after) {
        var label = after == null ? before.name() : after.name();
        recorder.record("eventcatalog", type + "." + operation, type, id.toString(), label,
            type.substring(0, 1).toUpperCase() + type.substring(1) + " " + operation, List.of(
                change("name", before, after, Snapshot::name),
                change("startTime", before, after, Snapshot::start),
                change("endTime", before, after, Snapshot::end),
                change("eventId", before, after, Snapshot::parent),
                change("locationId", before, after, Snapshot::location)));
    }
    private static AuditEntry.Change change(String name, Snapshot before, Snapshot after,
                                            java.util.function.Function<Snapshot, String> field) {
        return new AuditEntry.Change(name, before == null ? null : field.apply(before),
            after == null ? null : field.apply(after));
    }
    public static String value(Object value) { return value == null ? null : value.toString(); }
}
