package at.control.shiftcontrol.shift.audit;

import at.control.shiftcontrol.audit.api.AuditEntry;
import at.control.shiftcontrol.audit.api.AuditRecorder;
import at.control.shiftcontrol.shift.entity.Role;
import at.control.shiftcontrol.shift.entity.TimeConstraint;
import at.control.shiftcontrol.shift.entity.ShiftPlanInvite;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OperationsAudit {
    private final AuditRecorder recorder;
    public OperationsAudit(AuditRecorder recorder) { this.recorder = recorder; }
    public record RoleSnapshot(String name, String plan, String selfAssignable, String points) {}
    public RoleSnapshot snapshot(Role role) {
        return new RoleSnapshot(role.getName(), value(role.getShiftPlan().getId()), value(role.isSelfAssignable()), value(role.getRewardPointsPerMinute()));
    }
    public void role(Role role, String action, RoleSnapshot before, RoleSnapshot after) {
        recorder.record("shift", "role." + action, "role", value(role.getId()), role.getName(), "Role " + action,
            List.of(new AuditEntry.Change("name", before == null ? null : before.name(), after == null ? null : after.name()),
                new AuditEntry.Change("planId", before == null ? null : before.plan(), after == null ? null : after.plan()),
                new AuditEntry.Change("selfAssignable", before == null ? null : before.selfAssignable(), after == null ? null : after.selfAssignable()),
                new AuditEntry.Change("pointsPerMinute", before == null ? null : before.points(), after == null ? null : after.points())));
    }
    public void membership(Role role, UUID userId, boolean assigned) {
        recorder.record("shift", assigned ? "role.assigned" : "role.unassigned", "role", value(role.getId()), role.getName(),
            assigned ? "Role assigned to volunteer" : "Role removed from volunteer",
            List.of(new AuditEntry.Change("volunteerId", assigned ? null : value(userId), assigned ? value(userId) : null),
                new AuditEntry.Change("planId", value(role.getShiftPlan().getId()), value(role.getShiftPlan().getId()))));
    }
    public void constraint(TimeConstraint c, boolean created) {
        recorder.record("shift", created ? "time_constraint.created" : "time_constraint.deleted", "time_constraint", value(c.getId()), null,
            created ? "Time constraint created" : "Time constraint deleted", List.of(
                field("volunteerId", value(c.getVolunteer().getId()), created), field("eventId", value(c.getEvent().getId()), created),
                field("type", value(c.getType()), created), field("startTime", value(c.getStartTime()), created), field("endTime", value(c.getEndTime()), created)));
    }
    public void invite(ShiftPlanInvite invite, String action, Boolean before, Boolean after) {
        recorder.record("shift", "invite." + action, "invite", value(invite.getId()), null, "Plan invitation " + action,
            List.of(new AuditEntry.Change("planId", value(invite.getShiftPlan().getId()), value(invite.getShiftPlan().getId())),
                new AuditEntry.Change("type", value(invite.getType()), value(invite.getType())),
                new AuditEntry.Change("active", value(before), value(after))));
    }
    public void planAccess(UUID userId, long planId, String action, boolean before, boolean after) {
        recorder.record("shift", "volunteer." + action, "volunteer", value(userId), null,
            "Volunteer " + action.replace('_', ' '), List.of(
                new AuditEntry.Change("planId", value(planId), value(planId)),
                new AuditEntry.Change("value", value(before), value(after))));
    }
    public record TradeSnapshot(String status, String offering, String requested) {}
    public TradeSnapshot snapshot(at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest trade) {
        return new TradeSnapshot(value(trade.getStatus()), value(trade.getOfferingAssignment().getId()), value(trade.getRequestedAssignment().getId()));
    }
    public void trade(at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest trade, String action, TradeSnapshot before) {
        var after = snapshot(trade);
        recorder.record("shift", "trade." + action, "trade", value(trade.getId()), null, "Trade " + action,
            List.of(new AuditEntry.Change("status", before == null ? null : before.status(), after.status()),
                new AuditEntry.Change("offeringAssignmentId", before == null ? null : before.offering(), after.offering()),
                new AuditEntry.Change("requestedAssignmentId", before == null ? null : before.requested(), after.requested())));
    }
    public void tradeDeleted(at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest trade) {
        var before = snapshot(trade);
        recorder.record("shift", "trade.deleted", "trade", value(trade.getId()), null, "Trade removed",
            List.of(new AuditEntry.Change("status", before.status(), null),
                new AuditEntry.Change("offeringAssignmentId", before.offering(), null),
                new AuditEntry.Change("requestedAssignmentId", before.requested(), null)));
    }
    private static AuditEntry.Change field(String key, String value, boolean created) {
        return new AuditEntry.Change(key, created ? null : value, created ? value : null);
    }
    private static String value(Object v) { return v == null ? null : v.toString(); }
}
