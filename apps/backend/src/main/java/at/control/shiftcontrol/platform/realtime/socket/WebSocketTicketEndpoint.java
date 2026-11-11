package at.control.shiftcontrol.platform.realtime.socket;

import at.control.shiftcontrol.platform.realtime.api.RealtimeUserAccess;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value = "/api/v1/realtime", produces = "application/json")
class WebSocketTicketEndpoint {
    private final WebSocketTicketService tickets;
    private final RealtimeUserAccess users;
    WebSocketTicketEndpoint(WebSocketTicketService tickets, RealtimeUserAccess users) {
        this.tickets = tickets;
        this.users = users;
    }
    record Ticket(String ticket, int expiresInSeconds) {}

    @PostMapping("/ticket")
    ResponseEntity<Ticket> issue(Authentication authentication) {
        var user = users.authenticate(authentication).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Ticket(tickets.issue(user.userId(), user.expiresAt()), 60));
    }
}
