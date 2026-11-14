package at.conrunner.platform.customfields.api;

import at.conrunner.platform.exception.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

/** PostgreSQL implementation of {@link FieldOwnerStore} for JSONB value tables. */
public final class JdbcFieldOwnerStore implements FieldOwnerStore {
    private final FieldEntityType type;
    private final JdbcTemplate jdbc;
    private final String table;
    private final String ownerQuery;
    private final String lockClause;

    /**
     * Configures a store with trusted SQL from the owning domain. The value table must have a
     * unique constraint on {@code (owner_id, definition_id)} for upserts.
     *
     * @param type entity type handled by the store
     * @param jdbc JDBC template using the domain transaction's data source
     * @param table value table name consisting of lowercase ASCII letters and underscores
     * @param ownerQuery SQL that binds one owner UUID and selects its event UUID in the first column
     * @param lockClause SQL suffix, including leading whitespace, that locks the owner and any
     *        rows needed to keep its event scope stable during writes
     * @throws IllegalArgumentException if the table name contains disallowed characters or is empty
     */
    public JdbcFieldOwnerStore(FieldEntityType type, JdbcTemplate jdbc, String table, String ownerQuery, String lockClause) {
        if (!table.matches("[a-z_]+")) throw new IllegalArgumentException("Invalid table name");
        this.type = type;
        this.jdbc = jdbc;
        this.table = table;
        this.ownerQuery = ownerQuery;
        this.lockClause = lockClause;
    }
    @Override public FieldEntityType entityType() { return type; }
    @Override public UUID eventId(UUID ownerId, boolean lock) {
        var ids = jdbc.query(ownerQuery + (lock ? lockClause : ""), (rs, row) -> rs.getObject(1, UUID.class), ownerId);
        if (ids.isEmpty()) throw new NotFoundException("Custom field owner not found");
        return ids.getFirst();
    }
    @Override public Map<UUID, String> read(UUID ownerId) {
        Map<UUID, String> result = new HashMap<>();
        jdbc.query("select definition_id, value::text from " + table + " where owner_id = ?",
                rs -> { result.put(rs.getObject(1, UUID.class), rs.getString(2)); }, ownerId);
        return result;
    }
    @Override public void put(UUID ownerId, UUID definitionId, String json) {
        jdbc.update("insert into " + table + " (id, owner_id, definition_id, value) values (?, ?, ?, cast(? as jsonb)) "
                + "on conflict (owner_id, definition_id) do update set value = excluded.value",
                UUID.randomUUID(), ownerId, definitionId, json);
    }
    @Override public void remove(UUID ownerId, UUID definitionId) {
        jdbc.update("delete from " + table + " where owner_id = ? and definition_id = ?", ownerId, definitionId);
    }
}
