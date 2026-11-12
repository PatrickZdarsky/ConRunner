package at.conrunner.shift.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class TraceIdFilter extends OncePerRequestFilter {
    private final ObjectProvider<Tracer> tracerProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String traceId = getTraceId();
        if (traceId != null) {
            response.setHeader("X-Trace-Id", traceId);
        }
        filterChain.doFilter(request, response);
    }

    private String getTraceId() {
        var tracer = tracerProvider.getIfAvailable();
        if (tracer == null) return null;
        var context = tracer.currentTraceContext().context();
        return context != null ? context.traceId() : null;
    }
}
