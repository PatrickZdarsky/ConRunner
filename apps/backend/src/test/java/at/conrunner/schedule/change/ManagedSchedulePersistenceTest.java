package at.conrunner.schedule.change;

import java.time.Instant;
import java.util.UUID;

import at.conrunner.TestcontainersConfiguration;
import at.conrunner.schedule.event.CatalogEventEntity;
import at.conrunner.schedule.event.EventRepository;
import at.conrunner.schedule.session.BaselinePresence;
import at.conrunner.schedule.session.SessionBaselineEntity;
import at.conrunner.schedule.session.SessionBaselineRepository;
import at.conrunner.schedule.session.SessionEntity;
import at.conrunner.schedule.session.SessionRepository;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
    "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml",
    "spring.jpa.hibernate.ddl-auto=none"
})
@Import(TestcontainersConfiguration.class)
class ManagedSchedulePersistenceTest {
    private static final Instant START = Instant.parse("2026-11-14T13:00:00Z");
    private static final Instant END = START.plusSeconds(3600);

    @Autowired EventRepository events;
    @Autowired SessionRepository sessions;
    @Autowired SessionBaselineRepository baselines;
    @Autowired ChangeBatchRepository batches;
    @Autowired ChangeEntryRepository entries;
    @Autowired EntityManager entityManager;

    @Test
    void queriesSharedDraftAndOrderedCommittedChanges() {
        UUID eventId = event();
        var session = new SessionEntity();
        session.setEventId(eventId);
        session.setName("Opening");
        session.setStartTime(START);
        session.setEndTime(END);
        session.setEffectiveContent(JsonNodeFactory.instance.objectNode().put("description", "Opening session"));
        UUID occurrenceId = sessions.saveAndFlush(session).getId();

        var baseline = new SessionBaselineEntity();
        baseline.setOccurrenceId(occurrenceId);
        baseline.setEventId(eventId);
        baseline.setStartTime(START);
        baseline.setEndTime(END);
        baseline.setContent(session.getEffectiveContent().deepCopy());
        baselines.saveAndFlush(baseline);

        var batch = draft(eventId);
        var entry = new ChangeEntryEntity();
        entry.setEventId(eventId);
        entry.setBatchId(batch.getId());
        entry.setOccurrenceId(occurrenceId);
        entry.setOperationType(ChangeOperation.CONTENT_CORRECTION);
        entry.setReason("Clarify the session description");
        entry.setPayload(JsonNodeFactory.instance.objectNode()
            .put("field", "content.description").put("action", "SET").put("value", "Updated description"));
        entries.saveAndFlush(entry);

        assertThat(batches.findCurrentDraft(eventId)).hasValueSatisfying(draft -> assertThat(draft.getId()).isEqualTo(batch.getId()));
        assertThat(batches.latestCommittedSequence(eventId)).isZero();
        assertThat(entries.findCommittedOccurrenceHistory(eventId, occurrenceId)).isEmpty();

        batch.setState(ChangeState.COMMITTED);
        batch.setSequence(1L);
        batch.setCommittedAt(START);
        entry.setState(ChangeState.COMMITTED);
        entityManager.flush();
        entityManager.clear();

        assertThat(batches.findCurrentDraft(eventId)).isEmpty();
        assertThat(batches.latestCommittedSequence(eventId)).isEqualTo(1);
        assertThat(batches.findCommittedChangesAfter(eventId, 0, PageRequest.ofSize(10)))
            .extracting(ChangeBatchEntity::getId).containsExactly(batch.getId());
        assertThat(entries.findCommittedOccurrenceHistory(eventId, occurrenceId))
            .singleElement().satisfies(change -> {
                assertThat(change.getPayload()).isEqualTo(entry.getPayload());
                assertThat(change.getReason()).isEqualTo("Clarify the session description");
            });
        assertThat(baselines.findById(occurrenceId))
            .hasValueSatisfying(saved -> {
                assertThat(saved.getContent()).isEqualTo(baseline.getContent());
                assertThat(saved.getPresence()).isEqualTo(BaselinePresence.PRESENT);
                saved.setPresence(BaselinePresence.REMOVED);
            });
        entityManager.flush();
        entityManager.clear();

        assertThat(baselines.findById(occurrenceId))
            .hasValueSatisfying(saved -> assertThat(saved.getPresence()).isEqualTo(BaselinePresence.REMOVED));
    }

    @Test
    void rejectsASecondDraftForTheSameEvent() {
        UUID eventId = event();
        draft(eventId);
        assertThatThrownBy(() -> draft(eventId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID event() {
        var event = new CatalogEventEntity();
        event.setName("Convention");
        event.setStartTime(START);
        event.setEndTime(END);
        return events.saveAndFlush(event).getId();
    }

    private ChangeBatchEntity draft(UUID eventId) {
        var batch = new ChangeBatchEntity();
        batch.setEventId(eventId);
        batch.setCreatedAt(START);
        batch.setUpdatedAt(START);
        return batches.saveAndFlush(batch);
    }
}
