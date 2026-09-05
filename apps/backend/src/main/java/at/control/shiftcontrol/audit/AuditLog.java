package at.control.shiftcontrol.audit;

import java.time.Clock;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Records business actions in the caller's transaction; a failed write rolls that action back. */
@Service
@RequiredArgsConstructor
public class AuditLog {
    private final AuditEntryRepository repository;
    private final Clock clock;

    public void record(UUID actorUserId, String action, String subjectType, UUID subjectId,
                       String previousState, String newState, String reason) {
        AuditEntryEntity entry = new AuditEntryEntity();
        entry.setOccurredAt(clock.instant());
        entry.setActorUserId(actorUserId);
        entry.setModule("shift");
        entry.setAction(action);
        entry.setSubjectType(subjectType);
        entry.setSubjectId(subjectId);
        entry.setPreviousState(previousState);
        entry.setNewState(newState);
        entry.setReason(normalizeReason(reason));
        repository.save(entry);
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String normalized = reason.trim();
        if (normalized.length() > 1024) {
            throw new IllegalArgumentException("Audit reason must be at most 1024 characters");
        }
        return normalized;
    }
}
