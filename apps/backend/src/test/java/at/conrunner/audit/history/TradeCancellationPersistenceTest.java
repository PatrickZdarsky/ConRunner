package at.conrunner.audit.history;

import at.conrunner.TestcontainersConfiguration;
import at.conrunner.audit.api.AuditContext;
import at.conrunner.audit.api.AuditEntry;
import at.conrunner.audit.api.AuditRecorder;
import at.conrunner.shift.audit.OperationsAudit;
import at.conrunner.shift.dao.impl.AssignmentSwitchRequestDaoImpl;
import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.AssignmentSwitchRequest;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.type.AssignmentStatus;
import at.conrunner.shift.lib.type.LockStatus;
import at.conrunner.shift.lib.type.TradeStatus;
import at.conrunner.shift.entity.*;
import at.conrunner.shift.lib.type.*;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({TestcontainersConfiguration.class, AuditStore.class, AuditRecorder.class, OperationsAudit.class, AssignmentSwitchRequestDaoImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TradeCancellationPersistenceTest {
    @Autowired AssignmentSwitchRequestDaoImpl dao;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean AuditContext context;
    record Fixture(long slotId, UUID userId, long tradeId) {}

    @BeforeEach void setup() {
        jdbc.execute("create table if not exists location (id bigint primary key, event_id bigint, name varchar(255), description varchar(1024), url varchar(1024), additional_properties text, read_only boolean)");
        jdbc.execute("create table if not exists audit_entry (id uuid primary key, occurred_at timestamptz not null, actor_user_id uuid, actor_kind varchar(16), actor_identifier varchar(128), actor_label varchar(255), module varchar(64), action varchar(96), subject_type varchar(64), subject_ref varchar(128), subject_label varchar(255), summary varchar(512), correlation_id varchar(128), result varchar(32), schema_version integer)");
        jdbc.execute("create table if not exists audit_entry_change (entry_id uuid references audit_entry(id), ordinal integer, field_name varchar(64), before_value varchar(1024), after_value varchar(1024), primary key(entry_id, ordinal))");
        jdbc.execute("delete from audit_entry_change");
        jdbc.execute("delete from audit_entry");
        when(context.actor()).thenReturn(new AuditEntry.Actor(AuditEntry.ActorKind.SYSTEM, null, "test", "Test"));
    }
    private Fixture fixture() {
        return new TransactionTemplate(manager).execute(status -> {
            Instant now = Instant.parse("2030-01-01T00:00:00Z");
            Event event = Event.builder().name("Event " + UUID.randomUUID()).startTime(now).endTime(now.plusSeconds(3600)).build();
            em.persist(event);
            ShiftPlan plan = ShiftPlan.builder().event(event).name("Plan").lockStatus(LockStatus.SELF_SIGNUP).build(); em.persist(plan);
            Shift shift = Shift.builder().shiftPlan(plan).name("Shift").startTime(now).endTime(now.plusSeconds(3600)).build(); em.persist(shift);
            PositionSlot slot = PositionSlot.builder().shift(shift).name("Slot").desiredVolunteerCount(2).build(); em.persist(slot);
            Volunteer one = Volunteer.builder().id(UUID.randomUUID()).build(); Volunteer two = Volunteer.builder().id(UUID.randomUUID()).build(); em.persist(one); em.persist(two);
            Assignment a = Assignment.of(slot, one, AssignmentStatus.ACCEPTED); Assignment b = Assignment.of(slot, two, AssignmentStatus.ACCEPTED); em.persist(a); em.persist(b);
            AssignmentSwitchRequest trade = AssignmentSwitchRequest.builder().offeringAssignment(a).requestedAssignment(b).status(TradeStatus.OPEN).createdAt(now).build(); em.persist(trade); em.flush();
            return new Fixture(slot.getId(), one.getId(), trade.getId());
        });
    }
    @Test void failedAuditWriteRollsBackCancellation() {
        var f = fixture();
        jdbc.execute("alter table audit_entry add constraint test_reject_cancel check (action <> 'trade.cancelled')");
        try {
            assertThatThrownBy(() -> new TransactionTemplate(manager).executeWithoutResult(s -> dao.cancelTradesForPositionSlot(f.slotId(), f.userId())))
                .hasStackTraceContaining("test_reject_cancel");
            assertThat(jdbc.queryForObject("select status from assignment_switch_request where id = ?", String.class, f.tradeId())).isEqualTo("OPEN");
            assertThat(jdbc.queryForObject("select count(*) from audit_entry where subject_ref = ?", Integer.class, Long.toString(f.tradeId()))).isZero();
        } finally { jdbc.execute("alter table audit_entry drop constraint test_reject_cancel"); }
    }
    @Test void cancellationLocksTradeUntilTransactionCommits() throws Exception {
        var f = fixture();
        var locked = new CountDownLatch(1); var release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> new TransactionTemplate(manager).executeWithoutResult(s -> {
                dao.cancelTradesForPositionSlot(f.slotId(), f.userId());
                locked.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Lock release timed out"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> new TransactionTemplate(manager).executeWithoutResult(s -> {
                jdbc.execute("set local lock_timeout = '300ms'");
                dao.cancelTradesForPositionSlot(f.slotId(), f.userId());
            })).hasStackTraceContaining("lock timeout");
            release.countDown(); first.get(5, TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("select status from assignment_switch_request where id = ?", String.class, f.tradeId())).isEqualTo("CANCELED");
            assertThat(jdbc.queryForObject("select count(*) from audit_entry where subject_ref = ? and action = 'trade.cancelled'", Integer.class, Long.toString(f.tradeId()))).isEqualTo(1);
            assertThat(jdbc.queryForObject("select before_value || ' -> ' || after_value from audit_entry_change c join audit_entry e on e.id = c.entry_id where e.subject_ref = ? and c.field_name = 'status'", String.class, Long.toString(f.tradeId())))
                .isEqualTo("OPEN -> CANCELED");
        } finally { release.countDown(); executor.shutdownNow(); }
    }
}
