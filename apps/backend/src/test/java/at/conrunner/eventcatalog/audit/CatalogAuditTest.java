package at.conrunner.eventcatalog.audit;

import at.conrunner.audit.api.AuditEntry;
import at.conrunner.audit.api.AuditRecorder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class CatalogAuditTest {
    @Test void recordsSanitizedBeforeAndAfter() {
        var recorder = mock(AuditRecorder.class);
        var audit = new CatalogAudit(recorder);
        var id = UUID.randomUUID();
        var before = new CatalogAudit.Snapshot("Old", "2026-01-01T00:00:00Z", null, null, null);
        var after = new CatalogAudit.Snapshot("New", "2026-01-02T00:00:00Z", null, null, null);
        audit.record("event", "updated", id, before, after);
        verify(recorder).record("eventcatalog", "event.updated", "event", id.toString(), "New", "Event updated",
            List.of(new AuditEntry.Change("name", "Old", "New"),
                new AuditEntry.Change("startTime", before.start(), after.start()),
                new AuditEntry.Change("endTime", null, null),
                new AuditEntry.Change("eventId", null, null),
                new AuditEntry.Change("locationId", null, null),
                new AuditEntry.Change("archivedAt", null, null)));
    }
}
