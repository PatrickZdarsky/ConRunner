package at.control.shiftcontrol.audit.history;

import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value = "/api/v1/audit", produces = "application/json")
class AuditEndpoint {
    private final AuditHistory history;
    AuditEndpoint(AuditHistory history) { this.history = history; }
    @GetMapping
    AuditHistory.Result search(Authentication auth, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size, @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant until, @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String module, @RequestParam(required = false) String action,
            @RequestParam(required = false) String subjectType, @RequestParam(required = false) String subjectId) {
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("audit.read") || a.getAuthority().equals("admin")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (page < 0 || size < 1 || size > 100 || (from != null && until != null && !from.isBefore(until)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination or time range");
        return history.search(new AuditSearch(from, until, actorUserId, module, action, subjectType, subjectId), page, size);
    }
}
