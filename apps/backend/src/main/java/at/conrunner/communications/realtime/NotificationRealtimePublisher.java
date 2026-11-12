package at.conrunner.communications.realtime;

import at.conrunner.communications.history.NotificationRevisions;
import at.conrunner.platform.realtime.api.RealtimeConnections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Each instance checks durable revisions for its own sessions; history is the recovery source. */
@Component
class NotificationRealtimePublisher {
    private static final Logger log = LoggerFactory.getLogger(NotificationRealtimePublisher.class);
    private final NotificationRevisions revisions;
    private final RealtimeConnections connections;
    private final Map<UUID, Long> observed = new HashMap<>();

    NotificationRealtimePublisher(NotificationRevisions revisions, RealtimeConnections connections) {
        this.revisions = revisions;
        this.connections = connections;
    }

    @Scheduled(fixedDelayString = "${shiftcontrol.communications.realtime.poll-delay:PT3S}")
    synchronized void poll() {
        var users = connections.localConnectedUsers();
        observed.keySet().retainAll(users);
        for (var user : users) {
            try {
                long current = revisions.current(user);
                if (!Long.valueOf(current).equals(observed.get(user))) {
                    connections.sendToLocalUser(user, "notifications.changed");
                    observed.put(user, current);
                }
            } catch (RuntimeException _) {
                log.warn("Could not check notification updates for a connected user");
            }
        }
    }
}
