package at.control.shiftcontrol.audit.history;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Period;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
class AuditRetention implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AuditRetention.class);
    private final JdbcTemplate jdbc;
    private final Period retention;
    AuditRetention(JdbcTemplate jdbc, @Value("${shiftcontrol.audit.retention:P2Y}") Period retention) {
        if (retention.isNegative() || retention.isZero()) throw new IllegalArgumentException("Audit retention must be positive");
        this.jdbc = jdbc; this.retention = retention;
    }
    @Override public void run(ApplicationArguments args) {
        var cutoff = Timestamp.from(Clock.systemUTC().instant().atZone(ZoneOffset.UTC).minus(retention).toInstant());
        int deleted;
        long total = 0;
        do {
            deleted = jdbc.update("delete from audit_entry where id in (select id from audit_entry where occurred_at < ? order by occurred_at, id limit 1000)", cutoff);
            total += deleted;
        } while (deleted == 1000);
        log.info("Audit retention removed {} expired entries", total);
    }
}
