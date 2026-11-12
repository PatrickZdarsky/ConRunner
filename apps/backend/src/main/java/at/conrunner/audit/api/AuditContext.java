package at.conrunner.audit.api;

import at.conrunner.identity.AuthenticatedActor;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuditContext {
    private final ObjectProvider<Tracer> tracers;
    public AuditContext(ObjectProvider<Tracer> tracers) { this.tracers = tracers; }
    public AuditEntry.Actor actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthenticatedActor actor))
            throw new IllegalStateException("Audit requires an authenticated actor or an explicit system actor");
        return actor.isMachine()
            ? new AuditEntry.Actor(AuditEntry.ActorKind.MACHINE, null, actor.name(), actor.name())
            : new AuditEntry.Actor(AuditEntry.ActorKind.HUMAN, actor.userId().orElseThrow(), null, actor.name());
    }
    public String correlationId() {
        var tracer = tracers.getIfAvailable();
        var trace = tracer == null ? null : tracer.currentTraceContext().context();
        return trace == null ? null : trace.traceId();
    }
}
