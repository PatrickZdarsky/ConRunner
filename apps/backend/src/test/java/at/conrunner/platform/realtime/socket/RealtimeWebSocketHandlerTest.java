package at.conrunner.platform.realtime.socket;

import at.conrunner.platform.realtime.api.RealtimeUserAccess;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RealtimeWebSocketHandlerTest {
    @Test
    void routesDifferentMessageTypesOnlyToTheTargetUser() throws Exception {
        var tickets = mock(WebSocketTicketService.class);
        var users = mock(RealtimeUserAccess.class);
        var handler = new RealtimeWebSocketHandler(tickets, users);
        var first = mock(WebSocketSession.class);
        var second = mock(WebSocketSession.class);
        var user1 = UUID.randomUUID();
        var user2 = UUID.randomUUID();
        when(first.getId()).thenReturn("first");
        when(second.getId()).thenReturn("second");
        when(first.isOpen()).thenReturn(true);
        when(second.isOpen()).thenReturn(true);
        when(tickets.consume("one")).thenReturn(Optional.of(new WebSocketTicketService.TicketIdentity(user1, Instant.now().plusSeconds(120))));
        when(tickets.consume("two")).thenReturn(Optional.of(new WebSocketTicketService.TicketIdentity(user2, Instant.now().plusSeconds(120))));
        when(users.isActive(any())).thenReturn(true);
        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);
        handler.handleTextMessage(first, new TextMessage("one"));
        handler.handleTextMessage(second, new TextMessage("two"));
        assertThat(handler.localConnectedUsers()).containsExactlyInAnyOrder(user1, user2);
        handler.sendToLocalUser(user1, "notifications.changed");
        handler.sendToLocalUser(user1, "schedule.changed");
        var messages = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(first, times(3)).sendMessage(messages.capture());
        assertThat(messages.getAllValues()).extracting(TextMessage::getPayload)
            .containsExactly("{\"type\":\"ready\"}", "{\"type\":\"notifications.changed\"}", "{\"type\":\"schedule.changed\"}");
        verify(second, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void invalidTicketClosesConnection() throws Exception {
        var tickets = mock(WebSocketTicketService.class);
        var handler = new RealtimeWebSocketHandler(tickets, mock(RealtimeUserAccess.class));
        var session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session");
        when(tickets.consume("bad")).thenReturn(Optional.empty());
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("bad"));
        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(session, never()).sendMessage(any());
    }
}
