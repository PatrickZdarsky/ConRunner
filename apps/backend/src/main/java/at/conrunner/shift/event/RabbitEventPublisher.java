package at.conrunner.shift.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import at.conrunner.shift.lib.event.BaseEvent;
import at.conrunner.platform.exception.IllegalArgumentException;
import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.config.RabbitMqConfig;

@Service
@ConditionalOnProperty(prefix = "shiftcontrol.events.rabbitmq", name = "enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class RabbitEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final CurrentShiftActor currentActor;
    private final ObjectProvider<Tracer> tracerProvider;

    private static final String ROUTING_KEY_PREFIX = "shiftcontrol.";

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true
    )
    private void publishEvent(BaseEvent event) {
        event.setTraceId(getTraceId());
        if (event.getActingUserId() == null) {
            event.setActingUserId(getCurrentUserId());
        }
        var routingKey = event.getRoutingKey();
        if (routingKey == null || routingKey.isBlank()) {
            throw new IllegalArgumentException("Event class " + event.getClass().getName() + " returned null or blank routing key", null);
        }

        log.trace("Publishing event to RabbitMQ with routing key {}: {}", ROUTING_KEY_PREFIX + routingKey, event);
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NAME, ROUTING_KEY_PREFIX + routingKey, event);
    }

    private String getTraceId() {
        var tracer = tracerProvider.getIfAvailable();
        if (tracer == null) return null;
        var context = tracer.currentTraceContext().context();
        return context != null ? context.traceId() : null;
    }

    @Nullable
    private String getCurrentUserId() {
        return currentActor.find().flatMap(actor -> actor.getUserId()).map(java.util.UUID::toString).orElse(null);
    }
}
