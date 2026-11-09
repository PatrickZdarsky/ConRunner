package at.control.shiftcontrol.shift.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import at.control.shiftcontrol.shift.lib.event.BaseEvent;
import at.control.shiftcontrol.shift.lib.exception.IllegalArgumentException;
import at.control.shiftcontrol.shift.auth.ApplicationUserProvider;
import at.shiftcontrol.shiftservice.auth.user.ShiftControlUser;
import at.control.shiftcontrol.shift.config.RabbitMqConfig;

@Service
@Slf4j
@RequiredArgsConstructor
public class RabbitEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationUserProvider userProvider;
    private final Tracer tracer;

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
        var context = this.tracer.currentTraceContext().context();
        return context != null ? context.traceId() : null;
    }

    @Nullable
    private String getCurrentUserId() {
        var userOpt = userProvider.getNullableApplicationUser();
        if (userOpt.isPresent()) {
            var user = (ShiftControlUser) userOpt.get();
            return user.getUserId();
        } else {
            return null;
        }
    }
}
