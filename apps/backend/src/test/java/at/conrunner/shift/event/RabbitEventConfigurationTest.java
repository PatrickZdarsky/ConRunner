package at.conrunner.shift.event;

import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.config.RabbitMqConfig;
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
    void publishingCanBeEnabledWithoutATracer() {
        context.withPropertyValues("shiftcontrol.events.rabbitmq.enabled=true").run(result -> {
            assertThat(result).hasNotFailed();
            assertThat(result).hasSingleBean(RabbitEventPublisher.class);
            assertThat(result).hasSingleBean(RabbitMqConfig.class);
        });
    }
}
