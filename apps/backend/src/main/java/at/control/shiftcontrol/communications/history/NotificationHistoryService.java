package at.control.shiftcontrol.communications.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
class NotificationHistoryService {
    private final NotificationRepository repository;
    private final NotificationRevisions revisions;

    NotificationHistoryService(NotificationRepository repository, NotificationRevisions revisions) {
        this.repository = repository;
        this.revisions = revisions;
    }

    record Notification(UUID id, String type, String title, String body, String linkTarget,
                        Instant createdAt, Instant readAt) {}

    @Transactional(readOnly = true)
    public Page<Notification> list(UUID userId, int page, int size, String type, Boolean read, Instant fromTime, Instant toTime) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
        org.springframework.data.jpa.domain.Specification<NotificationEntity> filter = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("recipientId"), userId));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (read != null) predicates.add(read ? cb.isNotNull(root.get("readAt")) : cb.isNull(root.get("readAt")));
            if (fromTime != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromTime));
            if (toTime != null) predicates.add(cb.lessThan(root.get("createdAt"), toTime));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return repository.findAll(filter,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
            .map(n -> new Notification(n.id, n.type, n.title, n.body, n.linkTarget, n.createdAt, n.readAt));
    }

    @Transactional
    public boolean setRead(UUID userId, UUID notificationId, boolean read) {
        boolean changed = repository.setReadAt(notificationId, userId, read ? Instant.now() : null) == 1;
        if (changed) revisions.increment(userId);
        return changed;
    }
}
