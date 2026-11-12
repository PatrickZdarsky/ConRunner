package at.conrunner.communications.request;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import at.conrunner.communications.history.NotificationHistoryWriter;
import at.conrunner.communications.email.EmailDeliveryService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class NotificationRequestProcessingService {
    private final NotificationRequestRepository repository;
    private final NotificationHistoryWriter history;
    private final EmailDeliveryService email;

    NotificationRequestProcessingService(NotificationRequestRepository repository, NotificationHistoryWriter history, EmailDeliveryService email) {
        this.repository = repository;
        this.history = history;
        this.email = email;
    }

    record ClaimedRequest(UUID id, UUID leaseId, UUID triggerId, UUID recipientId,
                          String type, String templateData, int attempts, boolean emailRequested, boolean inAppRequested) {}

    @Transactional
    public List<ClaimedRequest> claim(int limit, Duration lease) {
        if (limit < 1 || limit > 100 || lease == null || lease.isNegative() || lease.isZero() || lease.toMillis() < 1)
            throw new IllegalArgumentException("Invalid request lease");
        var selected = repository.lockAvailable(limit);
        var leaseId = UUID.randomUUID();
        var expires = repository.databaseTime().plus(lease);
        var claimed = selected.stream().map(request -> {
            request.claim(leaseId, expires);
            return new ClaimedRequest(request.id, leaseId, request.triggerId, request.recipientId,
                request.type, request.templateData, request.attempts, request.emailRequested, request.inAppRequested);
        }).toList();
        repository.flush();
        return claimed;
    }

    @Transactional
    public boolean complete(UUID id, UUID leaseId) {
        return repository.complete(id, leaseId) == 1;
    }

    @Transactional
    public void completeWithHistory(ClaimedRequest request, String title, String body, String link) {
        if (repository.complete(request.id(), request.leaseId()) != 1)
            throw new IllegalStateException("Notification request lease expired");
        if (request.inAppRequested()) history.write(request.id(), request.recipientId(), request.type(), title, body, link);
        if (request.emailRequested()) email.enqueue(request.id(), request.recipientId(), request.type(), title, body, link);
    }

    @Transactional
    public boolean retry(UUID id, UUID leaseId, Instant availableAt, int maxAttempts) {
        if (maxAttempts < 1) throw new IllegalArgumentException("Invalid attempt limit");
        return repository.retry(id, leaseId, availableAt, maxAttempts) == 1;
    }
}
