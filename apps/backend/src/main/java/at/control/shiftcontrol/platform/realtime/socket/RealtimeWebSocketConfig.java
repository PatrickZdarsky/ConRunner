package at.control.shiftcontrol.platform.realtime.socket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableWebSocket
@EnableScheduling
class RealtimeWebSocketConfig implements WebSocketConfigurer {
    private final RealtimeWebSocketHandler handler;
    private final String allowedOrigins;
    RealtimeWebSocketConfig(RealtimeWebSocketHandler handler,
        @Value("${shiftcontrol.platform.realtime.allowed-origins:}") String allowedOrigins) {
        this.handler = handler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        var registration = registry.addHandler(handler, "/api/v1/realtime/ws");
        if (!allowedOrigins.isBlank())
            registration.setAllowedOrigins(java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isBlank() && !origin.equals("*")).toArray(String[]::new));
    }
}
