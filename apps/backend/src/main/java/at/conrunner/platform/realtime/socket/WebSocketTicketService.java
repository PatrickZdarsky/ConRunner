package at.conrunner.platform.realtime.socket;

import at.conrunner.platform.realtime.api.RealtimeUserAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
class WebSocketTicketService {
    private final WebSocketTicketRepository repository;
    private final RealtimeUserAccess users;
    private final SecureRandom random = new SecureRandom();
    WebSocketTicketService(WebSocketTicketRepository repository, RealtimeUserAccess users) {
        this.repository = repository;
        this.users = users;
    }

    @Transactional
    String issue(UUID userId, Instant tokenExpiry) {
        if (!users.isActive(userId)) throw new IllegalStateException("Account is not active");
        Instant now = Instant.now();
        if (tokenExpiry == null || !tokenExpiry.isAfter(now)) throw new IllegalArgumentException("Token has expired");
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant ticketExpiry = now.plusSeconds(60).isBefore(tokenExpiry) ? now.plusSeconds(60) : tokenExpiry;
        repository.save(new WebSocketTicketEntity(hash(ticket), userId, ticketExpiry, tokenExpiry));
        return ticket;
    }

    record TicketIdentity(UUID userId, Instant expiresAt) {}

    @Transactional
    Optional<TicketIdentity> consume(String ticket) {
        if (ticket == null || !ticket.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
        var stored = repository.findForUpdate(hash(ticket));
        if (stored.isEmpty()) return Optional.empty();
        var value = stored.get();
        repository.delete(value);
        if (!value.expiresAt.isAfter(Instant.now())) return Optional.empty();
        return Optional.of(new TicketIdentity(value.recipientId, value.sessionExpiresAt));
    }

    @Transactional
    void cleanup() { repository.deleteExpired(Instant.now()); }

    private String hash(String ticket) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ticket.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
