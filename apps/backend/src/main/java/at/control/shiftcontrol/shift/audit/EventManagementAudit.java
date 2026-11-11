package at.control.shiftcontrol.shift.audit;

import at.control.shiftcontrol.audit.api.AuditEntry;
import at.control.shiftcontrol.audit.api.AuditRecorder;
import at.control.shiftcontrol.shift.entity.Activity;
import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.Location;
import java.util.List;
import org.springframework.stereotype.Service;

/** Only deliberately selected fields are retained; URLs and free-form descriptions are excluded. */
@Service
public class EventManagementAudit {
    private final AuditRecorder recorder;
    public EventManagementAudit(AuditRecorder recorder) { this.recorder = recorder; }
    public record Snapshot(String name, String eventId, String start, String end, String locationId) {}
    private static String v(Object o) { return o == null ? null : o.toString(); }
    public Snapshot snapshot(Event e) { return new Snapshot(e.getName(), null, v(e.getStartTime()), v(e.getEndTime()), null); }
    public Snapshot snapshot(Location l) { return new Snapshot(l.getName(), v(l.getEvent().getId()), null, null, null); }
    public Snapshot snapshot(Activity a) { return new Snapshot(a.getName(), v(a.getEvent().getId()), v(a.getStartTime()), v(a.getEndTime()), a.getLocation() == null ? null : v(a.getLocation().getId())); }
    public void record(String type, String action, long id, Snapshot before, Snapshot after) {
        var label = after == null ? before.name() : after.name();
        recorder.record("shift", type + "." + action, type, v(id), label, type + " " + action,
            List.of(change("name", before, after, Snapshot::name), change("eventId", before, after, Snapshot::eventId),
                change("startTime", before, after, Snapshot::start), change("endTime", before, after, Snapshot::end),
                change("locationId", before, after, Snapshot::locationId)));
    }
    private static AuditEntry.Change change(String field, Snapshot b, Snapshot a, java.util.function.Function<Snapshot, String> f) {
        return new AuditEntry.Change(field, b == null ? null : f.apply(b), a == null ? null : f.apply(a));
    }
}
