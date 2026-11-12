package at.conrunner.platform.realtime.socket;

import at.conrunner.platform.realtime.api.RealtimeConnections;
import at.conrunner.platform.realtime.api.RealtimeUserAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
class RealtimeWebSocketHandler extends TextWebSocketHandler implements RealtimeConnections {
    private static final Logger log = LoggerFactory.getLogger(RealtimeWebSocketHandler.class);
    private final WebSocketTicketService tickets;
    private final RealtimeUserAccess users;
    private final ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<>();

    private static class Connection {
        final WebSocketSession session;
        final Instant connectedAt = Instant.now();
        volatile UUID userId;
        volatile Instant expiresAt;
        Connection(WebSocketSession session) { this.session = session; }
    }

    RealtimeWebSocketHandler(WebSocketTicketService tickets, RealtimeUserAccess users) {
        this.tickets = tickets;
        this.users = users;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(128);
        connections.put(session.getId(), new Connection(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        var connection = connections.get(session.getId());
        if (connection == null) { session.close(CloseStatus.POLICY_VIOLATION); return; }
        synchronized (connection) {
            if (connection.userId != null || Instant.now().isAfter(connection.connectedAt.plusSeconds(10))) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            var identity = tickets.consume(message.getPayload());
            if (identity.isEmpty() || !users.isActive(identity.get().userId())) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            connection.expiresAt = identity.get().expiresAt();
            connection.userId = identity.get().userId();
            session.sendMessage(new TextMessage("{\"type\":\"ready\"}"));
        }
    }

    @Override
    public Set<UUID> localConnectedUsers() {
        return connections.values().stream().filter(c -> c.userId != null && c.session.isOpen()
            && c.expiresAt.isAfter(Instant.now())).map(c -> c.userId).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void sendToLocalUser(UUID userId, String type) {
        if (userId == null || type == null || !type.matches("[a-z][a-z0-9._-]{0,95}"))
            throw new IllegalArgumentException("Invalid realtime message");
        for (var connection : connections.values()) {
            synchronized (connection) {
                if (!userId.equals(connection.userId)) {
                    continue;
                }
                send(connection, type);
            }
        }
    }

    private void send(Connection connection, String type) {
        try {
            if (valid(connection)) {
                connection.session.sendMessage(new TextMessage("{\"type\":\"" + type + "\"}"));
            } else {
                close(connection, CloseStatus.POLICY_VIOLATION);
            }
        } catch (Exception _) {
            log.warn("Realtime delivery failed for a connection");
            close(connection, CloseStatus.SERVER_ERROR);
        }
    }

    private boolean valid(Connection connection) {
        return connection.session.isOpen() && connection.expiresAt.isAfter(Instant.now())
            && Instant.now().isBefore(connection.connectedAt.plusSeconds(900))
            && users.isActive(connection.userId);
    }

    private void close(Connection connection, CloseStatus status) {
        if (connection == null) return;
        connections.remove(connection.session.getId());
        try {
            if (connection.session.isOpen()) {
                connection.session.close(status);
            }
        } catch (IOException _) {
            // The connection is already unregistered. Closing the socket is best effort.
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { connections.remove(session.getId()); }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) { close(connections.get(session.getId()), CloseStatus.SERVER_ERROR); }

    @Scheduled(fixedDelayString = "${shiftcontrol.platform.realtime.maintenance-delay:PT3S}")
    void maintainConnections() {
        for (var connection : connections.values()) {
            synchronized (connection) {
                try {
                    if (!connection.session.isOpen()) { connections.remove(connection.session.getId()); continue; }
                    if (connection.userId == null) {
                        if (!Instant.now().isBefore(connection.connectedAt.plusSeconds(10))) close(connection, CloseStatus.POLICY_VIOLATION);
                    } else if (!valid(connection)) close(connection, CloseStatus.POLICY_VIOLATION);
                } catch (RuntimeException _) { close(connection, CloseStatus.SERVER_ERROR); }
            }
        }
    }

    @Scheduled(fixedDelayString = "PT5M")
    void cleanupTickets() { tickets.cleanup(); }
}
