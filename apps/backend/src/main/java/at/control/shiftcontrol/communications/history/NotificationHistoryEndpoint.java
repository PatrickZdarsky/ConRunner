package at.control.shiftcontrol.communications.history;

import at.control.shiftcontrol.identity.AuthenticatedActor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import java.time.Instant;

@RestController
@RequestMapping(value = "/api/v1/me/notifications", produces = "application/json")
class NotificationHistoryEndpoint {
    private final NotificationHistoryService history;
    NotificationHistoryEndpoint(NotificationHistoryService history) { this.history = history; }

    record ReadState(Boolean read) {}

    @GetMapping
    PagedModel<NotificationHistoryService.Notification> list(Authentication authentication,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String type, @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant until) {
        if (page < 0 || size < 1 || size > 100 || (from != null && until != null && !from.isBefore(until)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination or date range");
        return new PagedModel<>(history.list(userId(authentication), page, size, type, read, from, until));
    }

    @PatchMapping("/{id}")
    void setRead(Authentication authentication, @PathVariable UUID id, @RequestBody ReadState state) {
        if (state == null || state.read() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Read state is required");
        if (!history.setRead(userId(authentication), id, state.read())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/read-all")
    void markAllRead(Authentication authentication) {
        history.markAllRead(userId(authentication));
    }

    @DeleteMapping("/{id}")
    void delete(Authentication authentication, @PathVariable UUID id) {
        if (!history.delete(userId(authentication), id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping
    void clear(Authentication authentication) {
        history.clear(userId(authentication));
    }

    private UUID userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedActor actor))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return actor.userId().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
