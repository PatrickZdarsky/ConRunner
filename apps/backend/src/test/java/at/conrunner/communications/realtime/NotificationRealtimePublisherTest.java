package at.conrunner.communications.realtime;

import at.conrunner.communications.history.NotificationRevisions;
import at.conrunner.platform.realtime.api.RealtimeConnections;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.mockito.Mockito.*;

class NotificationRealtimePublisherTest {
    @Test
    void publishesOnInitialConnectionAndRevisionChange() {
        var revisions = mock(NotificationRevisions.class);
        var connections = mock(RealtimeConnections.class);
        var user = UUID.randomUUID();
        when(connections.localConnectedUsers()).thenReturn(Set.of(user));
        when(revisions.current(user)).thenReturn(0L, 0L, 1L);
        var publisher = new NotificationRealtimePublisher(revisions, connections);
        publisher.poll();
        publisher.poll();
        publisher.poll();
        verify(connections, times(2)).sendToLocalUser(user, "notifications.changed");
    }

    @Test
    void separateInstancesObserveTheSameDurableRevision() {
        var revisions = mock(NotificationRevisions.class);
        var firstInstance = mock(RealtimeConnections.class);
        var secondInstance = mock(RealtimeConnections.class);
        var user = UUID.randomUUID();
        when(firstInstance.localConnectedUsers()).thenReturn(Set.of(user));
        when(secondInstance.localConnectedUsers()).thenReturn(Set.of(user));
        when(revisions.current(user)).thenReturn(1L);
        new NotificationRealtimePublisher(revisions, firstInstance).poll();
        new NotificationRealtimePublisher(revisions, secondInstance).poll();
        verify(firstInstance).sendToLocalUser(user, "notifications.changed");
        verify(secondInstance).sendToLocalUser(user, "notifications.changed");
    }
}
