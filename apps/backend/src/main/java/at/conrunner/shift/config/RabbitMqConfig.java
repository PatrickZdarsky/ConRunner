package at.conrunner.shift.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty(prefix = "shiftcontrol.events.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMqConfig {
    public static final String EXCHANGE_NAME = "shiftcontrol";

    @Bean
    public Declarables topicExchangeConfig() {
        var shiftExchange = new TopicExchange(EXCHANGE_NAME);

        return new Declarables(shiftExchange);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final var rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public JacksonJsonMessageConverter producerJackson2MessageConverter() {
        var mapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
        return new JacksonJsonMessageConverter(mapper);
    }
}
