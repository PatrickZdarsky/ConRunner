package at.control.shiftcontrol.platform.realtime.api;

import java.util.Set;
import java.util.UUID;

/** Local connections only. Each application instance must publish to its own connections. */
public interface RealtimeConnections {
    Set<UUID> localConnectedUsers();

    /** Sends a type-only invalidation to every currently authorized local session for the user. */
    void sendToLocalUser(UUID userId, String type);
}
