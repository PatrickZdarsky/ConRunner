package at.conrunner.shift.event;

import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.config.RabbitMqConfig;
import at.conrunner.shift.lib.event.EventType;
import at.conrunner.shift.lib.event.events.UserEvent;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitEventConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withUserConfiguration(RabbitMqConfig.class, RabbitEventPublisher.class)
        .withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class))
        .withBean(CurrentShiftActor.class, () -> mock(CurrentShiftActor.class));

    @Test
    void publishingIsDisabledByDefault() {
        context.run(result -> {
            assertThat(result).doesNotHaveBean(RabbitEventPublisher.class);
            assertThat(result).doesNotHaveBean(RabbitMqConfig.class);
        });
    }

    @Test
    void preservesJsonEventContentAndNumericTimestamp() {
        context.withPropertyValues("shiftcontrol.events.rabbitmq.enabled=true").run(result -> {
            var event = new UserEvent(EventType.USERS_EVENT_UPDATE, "test.routing.key", null, List.of());
            event.setTimestamp(Instant.ofEpochSecond(123, 500_000_000));

            var converter = result.getBean(RabbitTemplate.class).getMessageConverter();
            var message = converter.toMessage(event, new MessageProperties());
            var payload = new JsonMapper().readTree(message.getBody());

            assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
            assertThat(payload.path("eventType").asText()).isEqualTo("USERS_EVENT_UPDATE");
            assertThat(payload.path("timestamp").decimalValue()).isEqualByComparingTo("123.5");
            assertThat(payload.has("routingKey")).isFalse();
        });
    }

    @Test
    void publishingCanBeEnabledWithoutATracer() {
        context.withPropertyValues("shiftcontrol.events.rabbitmq.enabled=true").run(result -> {
            assertThat(result).hasNotFailed();
            assertThat(result).hasSingleBean(RabbitEventPublisher.class);
            assertThat(result).hasSingleBean(RabbitMqConfig.class);
        });
    }
}
