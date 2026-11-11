package at.control.shiftcontrol.audit.history;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuditHistory {
    private final JdbcTemplate jdbc;
    AuditHistory(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    record Change(String field, String before, String after) {}
    record Entry(UUID id, Instant occurredAt, String actorKind, UUID actorUserId, String actorIdentifier,
                 String actorLabel, String module, String action, String subjectType, String subjectId,
                 String subjectLabel, String summary, String result, String correlationId, int schemaVersion,
                 List<Change> changes) {}
    record Result(List<Entry> items, int page, int size, long total) {}

    @Transactional(readOnly = true)
    Result search(AuditSearch filter, int page, int size) {
        var where = new StringBuilder(" where 1=1");
        var args = new ArrayList<Object>();
        add(where, args, "occurred_at >= ?", filter.from() == null ? null : Timestamp.from(filter.from()));
        add(where, args, "occurred_at < ?", filter.until() == null ? null : Timestamp.from(filter.until()));
        add(where, args, "actor_user_id = ?", filter.actorUserId());
        add(where, args, "module = ?", filter.module());
        add(where, args, "action = ?", filter.action());
        add(where, args, "subject_type = ?", filter.subjectType());
        add(where, args, "subject_ref = ?", filter.subjectId());
        Long total = jdbc.queryForObject("select count(*) from audit_entry" + where, Long.class, args.toArray());
        var pageArgs = new ArrayList<>(args); pageArgs.add(size); pageArgs.add((long) page * size);
        var entries = jdbc.query("select * from audit_entry" + where + " order by occurred_at desc, id desc limit ? offset ?",
            (rs, row) -> map(rs), pageArgs.toArray());
        return new Result(entries, page, size, total == null ? 0 : total);
    }
    private Entry map(ResultSet rs) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        var changes = jdbc.query("select field_name, before_value, after_value from audit_entry_change where entry_id = ? order by ordinal",
            (r, n) -> new Change(r.getString(1), r.getString(2), r.getString(3)), id);
        return new Entry(id, rs.getTimestamp("occurred_at").toInstant(), rs.getString("actor_kind"),
            rs.getObject("actor_user_id", UUID.class), rs.getString("actor_identifier"), rs.getString("actor_label"),
            rs.getString("module"), rs.getString("action"), rs.getString("subject_type"), rs.getString("subject_ref"),
            rs.getString("subject_label"), rs.getString("summary"), rs.getString("result"), rs.getString("correlation_id"),
            rs.getInt("schema_version"), changes);
    }
    private static void add(StringBuilder sql, List<Object> args, String expression, Object value) {
        if (value != null) { sql.append(" and ").append(expression); args.add(value); }
    }
}
