package at.conrunner.communications.history;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class NotificationRevisions {
    private final NotificationRevisionRepository repository;
    NotificationRevisions(NotificationRevisionRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.MANDATORY)
    void increment(UUID userId) { repository.increment(userId); }

    @Transactional(readOnly = true)
    public long current(UUID userId) { return repository.revision(userId).orElse(0L); }
}
