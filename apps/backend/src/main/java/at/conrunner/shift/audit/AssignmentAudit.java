package at.conrunner.shift.audit;

import at.conrunner.audit.api.AuditRecorder;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.audit.api.AuditEntry;
import at.conrunner.identity.User;
import at.conrunner.identity.UserService;
import at.conrunner.shift.entity.Assignment;
import java.util.List;
import org.springframework.stereotype.Service;

/** Only approved assignment fields are recorded; no request, entity or credential is serialized. */
@Service
public class AssignmentAudit {
    private final AuditRecorder recorder;
    private final UserService users;
    public AssignmentAudit(AuditRecorder recorder, UserService users) {
        this.recorder = recorder; this.users = users;
    }
    public record Snapshot(String volunteerId, String volunteerName, String status, String rewardPoints) {}
    public Snapshot snapshot(Assignment a) {
        var id = a.getAssignedVolunteer().getId();
        var name = users.getUsers(List.of(id)).stream().findFirst().map(User::getUsername).orElse(null);
        return new Snapshot(id.toString(), name, a.getStatus().name(), Integer.toString(a.getAcceptedRewardPoints()));
    }
    private Snapshot after(Assignment a, Snapshot before) {
        if (!a.getAssignedVolunteer().getId().toString().equals(before.volunteerId())) return snapshot(a);
        return new Snapshot(before.volunteerId(), before.volunteerName(), a.getStatus().name(), Integer.toString(a.getAcceptedRewardPoints()));
    }
    public void removedWithSlot(PositionSlot slot, String summary) {
        if (slot.getAssignments() != null) slot.getAssignments().forEach(a ->
            changed("assignment.removed", summary, a, snapshot(a), null));
    }
    public void removedWithShift(Shift shift, String summary) {
        if (shift.getSlots() != null) shift.getSlots().forEach(slot -> removedWithSlot(slot, summary));
    }
    public void removedWithPlan(ShiftPlan plan, String summary) {
        if (plan.getShifts() != null) plan.getShifts().forEach(shift -> removedWithShift(shift, summary));
    }
    public void auctionClaimed(Assignment a, Snapshot before) { changed("assignment.reassigned", "Auction assignment claimed", a, before, after(a, before)); }
    public void traded(Assignment a, Snapshot before) { changed("assignment.reassigned", "Assignment changed in trade", a, before, after(a, before)); }
    public void accepted(Assignment a, Snapshot before) { changed("assignment.accepted", "Assignment accepted", a, before, after(a, before)); }
    public void joined(Assignment a) { changed("assignment.joined", "Volunteer joined position", a, null, snapshot(a)); }
    public void left(Assignment a) { left(a, snapshot(a)); }
    public void left(Assignment a, Snapshot before) { changed("assignment.left", "Volunteer left position", a, before, null); }
    public void auctionClosed(Assignment a) { auctionClosed(a, snapshot(a)); }
    public void auctionClosed(Assignment a, Snapshot before) { changed("assignment.left", "Volunteer left position when auctions were closed", a, before, null); }
    public void requestDeclined(Assignment a) { requestDeclined(a, snapshot(a)); }
    public void requestDeclined(Assignment a, Snapshot before) { changed("assignment.request.declined", "Join request declined", a, before, null); }
    public void emergencyLeaveRequested(Assignment a, Snapshot before) { changed("assignment.leave.requested", "Leave requested due to an emergency time constraint", a, before, after(a, before)); }
    public void planLeft(Assignment a) { changed("assignment.left", "Volunteer left position when leaving plan", a, snapshot(a), null); }
    public void requestCreated(Assignment a) { changed("assignment.request.created", "Volunteer requested to join position", a, null, snapshot(a)); }
    public void leaveRequested(Assignment a, Snapshot before) { changed("assignment.leave.requested", "Volunteer requested to leave position", a, before, after(a, before)); }
    public void requestWithdrawn(Assignment a) { requestWithdrawn(a, snapshot(a)); }
    public void requestWithdrawn(Assignment a, Snapshot before) { changed("assignment.request.withdrawn", "Join request withdrawn", a, before, null); }
    public void leaveWithdrawn(Assignment a, Snapshot before) { changed("assignment.leave.withdrawn", "Leave request withdrawn", a, before, after(a, before)); }
    public void auctionCreated(Assignment a, Snapshot before) { changed("assignment.auction.created", "Assignment offered for auction", a, before, after(a, before)); }
    public void auctionCancelled(Assignment a, Snapshot before) { changed("assignment.auction.cancelled", "Assignment auction cancelled", a, before, after(a, before)); }
    public void leaveDeclined(Assignment a, Snapshot before) { changed("assignment.leave.declined", "Leave request declined", a, before, after(a, before)); }
    private void changed(String action, String summary, Assignment assignment, Snapshot before, Snapshot after) {
        recorder.record("shift", action, "assignment", Long.toString(assignment.getId()),
            assignment.getPositionSlot().getName(), summary, List.of(
            new AuditEntry.Change("positionSlotId", before == null ? null : Long.toString(assignment.getPositionSlot().getId()), after == null ? null : Long.toString(assignment.getPositionSlot().getId())),
            new AuditEntry.Change("volunteerId", before == null ? null : before.volunteerId(), after == null ? null : after.volunteerId()),
            new AuditEntry.Change("volunteerName", before == null ? null : before.volunteerName(), after == null ? null : after.volunteerName()),
            new AuditEntry.Change("status", before == null ? null : before.status(), after == null ? null : after.status()),
            new AuditEntry.Change("acceptedRewardPoints", before == null ? null : before.rewardPoints(), after == null ? null : after.rewardPoints())
        ));
    }
}
