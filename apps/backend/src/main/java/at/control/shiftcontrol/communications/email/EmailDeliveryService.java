package at.control.shiftcontrol.communications.email;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class EmailDeliveryService {
    private final EmailDeliveryRepository repository;
    EmailDeliveryService(EmailDeliveryRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(UUID requestId, UUID userId, String type, String title, String body, String link) {
        repository.insertIfAbsent(UUID.randomUUID(), requestId, userId, type, title, body, link);
    }

    record Delivery(UUID id, UUID leaseId, UUID recipientId, String type, String title, String body, String link) {}

    @Transactional
    public List<Delivery> claim() {
        var selected = repository.lockAvailable(1);
        var expires = repository.databaseTime().plus(Duration.ofMinutes(2));
        var leaseId = UUID.randomUUID();
        var result = selected.stream().map(d -> {
            d.claim(leaseId, expires);
            return new Delivery(d.id, leaseId, d.recipientId, d.type, d.title, d.body, d.linkTarget);
        }).toList();
        repository.flush();
        return result;
    }

    @Transactional
    public boolean finish(Delivery delivery, boolean canceled) {
        return repository.finish(delivery.id(), delivery.leaseId(), canceled ? "CANCELED" : "SENT") == 1;
    }

    @Transactional
    public void retry(Delivery delivery) { repository.retry(delivery.id(), delivery.leaseId()); }
}
