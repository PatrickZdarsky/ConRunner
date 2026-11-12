package at.conrunner.audit.history;

import java.time.Instant;
import java.util.UUID;
record AuditSearch(Instant from, Instant until, UUID actorUserId, String module, String action,
                   String subjectType, String subjectId) {}
