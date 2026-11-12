package at.conrunner.audit.history;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuditHistory {
    private static final String SEARCH_FILTERS = """
        where (cast(:fromTime as timestamptz) is null or occurred_at >= :fromTime)
          and (cast(:untilTime as timestamptz) is null or occurred_at < :untilTime)
          and (cast(:actorId as uuid) is null or actor_user_id = :actorId)
          and (cast(:module as varchar) is null or module = :module)
          and (cast(:action as varchar) is null or action = :action)
          and (cast(:subjectType as varchar) is null or subject_type = :subjectType)
          and (cast(:subjectId as varchar) is null or subject_ref = :subjectId)
        """;
    private static final String COUNT_ENTRIES = "select count(*) from audit_entry " + SEARCH_FILTERS;
    private static final String FIND_ENTRIES = "select * from audit_entry " + SEARCH_FILTERS
        + "order by occurred_at desc, id desc limit :size offset :offset";

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate searchJdbc;

    AuditHistory(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.searchJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    record Change(String field, String before, String after) {}
    record Entry(UUID id, Instant occurredAt, String actorKind, UUID actorUserId, String actorIdentifier,
                 String actorLabel, String module, String action, String subjectType, String subjectId,
                 String subjectLabel, String summary, String result, String correlationId, int schemaVersion,
                 List<Change> changes) {}
    record Result(List<Entry> items, int page, int size, long total) {}

    @Transactional(readOnly = true)
    Result search(AuditSearch filter, int page, int size) {
        var parameters = new MapSqlParameterSource()
            .addValue("fromTime", utc(filter.from()))
            .addValue("untilTime", utc(filter.until()))
            .addValue("actorId", filter.actorUserId())
            .addValue("module", filter.module())
            .addValue("action", filter.action())
            .addValue("subjectType", filter.subjectType())
            .addValue("subjectId", filter.subjectId())
            .addValue("size", size)
            .addValue("offset", (long) page * size);
        Long total = searchJdbc.queryForObject(COUNT_ENTRIES, parameters, Long.class);
        var entries = searchJdbc.query(FIND_ENTRIES, parameters, (rs, _) -> map(rs));
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
    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
