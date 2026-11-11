package at.control.shiftcontrol.audit.history;

import at.control.shiftcontrol.audit.api.AuditEntry;
import at.control.shiftcontrol.audit.api.AuditWriter;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuditStore implements AuditWriter {
    private static final Logger log = LoggerFactory.getLogger(AuditStore.class);
    private final JdbcTemplate jdbc;
    AuditStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEntry entry) {
        try {
            insert(entry);
        } catch (RuntimeException failure) {
            log.error("Required audit write failed; the transaction will be rolled back");
            throw failure;
        }
    }
    private void insert(AuditEntry entry) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into audit_entry (id, occurred_at, actor_user_id, actor_kind, actor_identifier, actor_label,
              module, action, subject_type, subject_ref, subject_label, summary, correlation_id, result, schema_version)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SUCCESS', 1)
            """, id, Timestamp.from(Instant.now()), entry.actor().userId(), entry.actor().kind().name(),
            entry.actor().identifier(), entry.actor().label(), entry.module(), entry.action(), entry.subjectType(),
            entry.subjectId(), entry.subjectLabel(), entry.summary(), entry.correlationId());
        int index = 0;
        for (var change : entry.changes()) {
            jdbc.update("insert into audit_entry_change (entry_id, ordinal, field_name, before_value, after_value) values (?, ?, ?, ?, ?)",
                id, index++, change.field(), change.before(), change.after());
        }
    }
}
