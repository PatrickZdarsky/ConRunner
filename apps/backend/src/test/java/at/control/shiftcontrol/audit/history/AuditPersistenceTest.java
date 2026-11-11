package at.control.shiftcontrol.audit.history;

import at.control.shiftcontrol.TestcontainersConfiguration;
import at.control.shiftcontrol.audit.api.AuditEntry;
import at.control.shiftcontrol.audit.api.AuditWriter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import({TestcontainersConfiguration.class, AuditStore.class, AuditHistory.class})
class AuditPersistenceTest {
    @Autowired AuditWriter writer;
    @Autowired AuditHistory history;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;

    private AuditEntry entry(UUID actor, String subject) {
        return new AuditEntry(new AuditEntry.Actor(AuditEntry.ActorKind.HUMAN, actor, null, "Alex"), "shift",
            "assignment.joined", "assignment", subject, "Reception", "Volunteer joined position", "trace-1",
            List.of(new AuditEntry.Change("status", null, "ACCEPTED")));
    }
    private TransactionTemplate independent() {
        var tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx;
    }
    @Test void storesSearchableHumanReadableAndStructuredFacts() {
        UUID actor = UUID.randomUUID();
        writer.record(entry(actor, "42"));
        var found = history.search(new AuditSearch(Instant.now().minusSeconds(60), Instant.now().plusSeconds(60),
            actor, "shift", "assignment.joined", "assignment", "42"), 0, 10);
        assertThat(found.total()).isEqualTo(1);
        assertThat(found.items()).singleElement().satisfies(e -> {
            assertThat(e.actorLabel()).isEqualTo("Alex");
            assertThat(e.subjectLabel()).isEqualTo("Reception");
            assertThat(e.summary()).isEqualTo("Volunteer joined position");
            assertThat(e.changes()).containsExactly(new AuditHistory.Change("status", null, "ACCEPTED"));
        });
        assertThat(history.search(new AuditSearch(null, null, UUID.randomUUID(), null, null, null, null), 0, 10).total()).isZero();
    }
    @Test void requiresTransactionAndRollsBackAlongsideBusinessData() {
        var noTx = new TransactionTemplate(transactions);
        noTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        noTx.executeWithoutResult(s -> assertThatThrownBy(() -> writer.record(entry(UUID.randomUUID(), "x")))
            .isInstanceOf(IllegalTransactionStateException.class));
        UUID id = UUID.randomUUID();
        independent().executeWithoutResult(status -> {
            jdbc.execute("create table if not exists audit_test_business (id uuid primary key)");
            jdbc.update("insert into audit_test_business values (?)", id);
            writer.record(entry(id, "rollback"));
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("select count(*) from audit_entry where subject_ref='rollback'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='audit_test_business'", Integer.class)).isZero();
    }
    @Test void failedAuditInsertRollsBackBusinessWrite() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> independent().executeWithoutResult(status -> {
            jdbc.update("insert into audit_entry (id,occurred_at,actor_user_id,module,action,subject_type,subject_ref,summary) values (?,current_timestamp,?,'x','x','x','business','x')", id, id);
            var invalid = new AuditEntry(new AuditEntry.Actor(AuditEntry.ActorKind.HUMAN, id, null, "Alex"),
                "shift", "assignment.updated", "assignment", "invalid", null, "Updated", null,
                List.of(new AuditEntry.Change("status", null, "invalid\u0000text")));
            writer.record(invalid);
        })).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("select count(*) from audit_entry where id=?", Integer.class, id)).isZero();
    }
    @Test void retentionDeletesExpiredEntriesAndTheirChanges() {
        UUID actor = UUID.randomUUID();
        writer.record(entry(actor, "expired"));
        writer.record(entry(actor, "recent"));
        jdbc.update("update audit_entry set occurred_at = current_timestamp - interval '3 years' where subject_ref='expired'");
        new AuditRetention(jdbc, java.time.Period.ofYears(2)).run(null);
        assertThat(history.search(new AuditSearch(null, null, actor, null, null, null, null), 0, 10).items())
            .extracting(AuditHistory.Entry::subjectId).containsExactly("recent");
        assertThat(jdbc.queryForObject("select count(*) from audit_entry_change", Integer.class)).isEqualTo(1);
    }

    @Test void realAssignmentServiceProducesReadableJoinAndLeaveHistory() {
        var dao = org.mockito.Mockito.mock(at.control.shiftcontrol.shift.dao.AssignmentDao.class);
        var switchDao = org.mockito.Mockito.mock(at.control.shiftcontrol.shift.dao.AssignmentSwitchRequestDao.class);
        var rewards = org.mockito.Mockito.mock(at.control.shiftcontrol.shift.service.rewardpoints.RewardPointsService.class);
        var events = org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class);
        var notifications = org.mockito.Mockito.mock(at.control.shiftcontrol.communications.api.NotificationRequests.class);
        var users = org.mockito.Mockito.mock(at.control.shiftcontrol.identity.UserService.class);
        var current = org.mockito.Mockito.mock(at.control.shiftcontrol.shift.auth.CurrentShiftActor.class);
        var actor = org.mockito.Mockito.mock(at.control.shiftcontrol.shift.auth.ShiftActor.class);
        @SuppressWarnings("unchecked") var traces = (org.springframework.beans.factory.ObjectProvider<io.micrometer.tracing.Tracer>)
            org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        UUID userId = UUID.randomUUID();
        org.mockito.Mockito.when(current.find()).thenReturn(java.util.Optional.of(actor));
        org.mockito.Mockito.when(actor.requireUserId()).thenReturn(userId);
        org.mockito.Mockito.when(actor.getName()).thenReturn("Alex");
        org.mockito.Mockito.when(users.getUsers(List.of(userId))).thenReturn(List.of(
            at.control.shiftcontrol.identity.User.builder().username("Alex").build()));
        org.mockito.Mockito.when(dao.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            at.control.shiftcontrol.shift.entity.Assignment a = invocation.getArgument(0);
            a.setId(101); return a;
        });
        var context = org.mockito.Mockito.mock(at.control.shiftcontrol.audit.api.AuditContext.class);
        org.mockito.Mockito.when(context.actor()).thenReturn(new AuditEntry.Actor(AuditEntry.ActorKind.HUMAN, userId, null, "Alex"));
        var audit = new at.control.shiftcontrol.shift.audit.AssignmentAudit(new at.control.shiftcontrol.audit.api.AuditRecorder(writer, context), users);
        var service = new at.control.shiftcontrol.shift.service.impl.AssignmentServiceImpl(audit, rewards, dao, switchDao, events, notifications);
        var slot = org.mockito.Mockito.mock(at.control.shiftcontrol.shift.entity.PositionSlot.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        org.mockito.Mockito.when(slot.getId()).thenReturn(42L);
        org.mockito.Mockito.when(slot.getName()).thenReturn("Reception");
        var volunteer = at.control.shiftcontrol.shift.entity.Volunteer.builder().id(userId).build();
        var assignment = service.assign(slot, volunteer,
            at.control.shiftcontrol.shift.dto.positionslot.PositionSlotRequestDto.builder().acceptedRewardPointsConfigHash("not recorded").build());
        service.unassignInternal(assignment);
        var result = history.search(new AuditSearch(null, null, userId, "shift", null, "assignment", "101"), 0, 10);
        assertThat(result.items()).extracting(AuditHistory.Entry::action)
            .containsExactly("assignment.left", "assignment.joined");
        assertThat(result.items().get(0).changes()).contains(new AuditHistory.Change("volunteerName", "Alex", null));
        assertThat(result.items().get(1).changes()).contains(new AuditHistory.Change("positionSlotId", null, "42"));
        assertThat(result.toString()).doesNotContain("not recorded");
    }

}
