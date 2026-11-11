package at.control.shiftcontrol.shift.audit;

import at.control.shiftcontrol.audit.api.AuditEntry;
import at.control.shiftcontrol.audit.api.AuditRecorder;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Shift;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlanningAudit {
    private final AuditRecorder recorder;
    public PlanningAudit(AuditRecorder recorder) { this.recorder = recorder; }
    public record Snapshot(String name, String parentId, String start, String end, String locationId,
                           String activityId, String roleId, String capacity, String rewardPoints, String skipAutoAssignment) {}
    private static String value(Object value) { return value == null ? null : value.toString(); }
    public Snapshot snapshot(Shift s) {
        return new Snapshot(s.getName(), value(s.getShiftPlan().getId()), value(s.getStartTime()), value(s.getEndTime()),
            s.getLocation() == null ? null : value(s.getLocation().getId()),
            s.getRelatedActivity() == null ? null : value(s.getRelatedActivity().getId()), null, null,
            value(s.getBonusRewardPoints()), null);
    }
    public Snapshot snapshot(PositionSlot s) {
        return new Snapshot(s.getName(), value(s.getShift().getId()), null, null, null, null,
            s.getRole() == null ? null : value(s.getRole().getId()), value(s.getDesiredVolunteerCount()),
            value(s.getOverrideRewardPoints()), value(s.isSkipAutoAssignment()));
    }
    public record PlanSnapshot(String name, String eventId, String lockStatus, String defaultPoints) {}
    public PlanSnapshot snapshot(at.control.shiftcontrol.shift.entity.ShiftPlan p) {
        return new PlanSnapshot(p.getName(), value(p.getEvent().getId()), value(p.getLockStatus()), value(p.getDefaultNoRolePointsPerMinute()));
    }
    public void planCreated(at.control.shiftcontrol.shift.entity.ShiftPlan p) { plan(p, "created", null, snapshot(p)); }
    public void planUpdated(at.control.shiftcontrol.shift.entity.ShiftPlan p, PlanSnapshot before) { plan(p, "updated", before, snapshot(p)); }
    public void planDeleted(at.control.shiftcontrol.shift.entity.ShiftPlan p) { plan(p, "deleted", snapshot(p), null); }
    private void plan(at.control.shiftcontrol.shift.entity.ShiftPlan p, String action, PlanSnapshot before, PlanSnapshot after) {
        recorder.record("shift", "plan." + action, "plan", value(p.getId()), after == null ? before.name() : after.name(),
            "Plan " + action, List.of(
                new AuditEntry.Change("name", before == null ? null : before.name(), after == null ? null : after.name()),
                new AuditEntry.Change("eventId", before == null ? null : before.eventId(), after == null ? null : after.eventId()),
                new AuditEntry.Change("lockStatus", before == null ? null : before.lockStatus(), after == null ? null : after.lockStatus()),
                new AuditEntry.Change("defaultPoints", before == null ? null : before.defaultPoints(), after == null ? null : after.defaultPoints())));
    }
    public void shiftCreated(Shift s) { record("shift", "created", s.getId(), null, snapshot(s)); }
    public void shiftUpdated(Shift s, Snapshot before) { record("shift", "updated", s.getId(), before, snapshot(s)); }
    public void shiftDeleted(Shift s) { record("shift", "deleted", s.getId(), snapshot(s), null); }
    public void slotCreated(PositionSlot s) { record("position_slot", "created", s.getId(), null, snapshot(s)); }
    public void slotUpdated(PositionSlot s, Snapshot before) { record("position_slot", "updated", s.getId(), before, snapshot(s)); }
    public void slotDeleted(PositionSlot s) { record("position_slot", "deleted", s.getId(), snapshot(s), null); }
    private void record(String type, String action, long id, Snapshot before, Snapshot after) {
        var label = after == null ? before.name() : after.name();
        recorder.record("shift", type + "." + action, type, Long.toString(id), label,
            (type.equals("shift") ? "Shift " : "Position ") + action, List.of(
                change("name", before, after, Snapshot::name), change("parentId", before, after, Snapshot::parentId),
                change("startTime", before, after, Snapshot::start), change("endTime", before, after, Snapshot::end),
                change("locationId", before, after, Snapshot::locationId), change("activityId", before, after, Snapshot::activityId),
                change("roleId", before, after, Snapshot::roleId), change("capacity", before, after, Snapshot::capacity),
                change("rewardPoints", before, after, Snapshot::rewardPoints),
                change("skipAutoAssignment", before, after, Snapshot::skipAutoAssignment)));
    }
    private static AuditEntry.Change change(String key, Snapshot before, Snapshot after,
                                            java.util.function.Function<Snapshot, String> getter) {
        return new AuditEntry.Change(key, before == null ? null : getter.apply(before), after == null ? null : getter.apply(after));
    }
}
