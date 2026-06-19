package com.securebank.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ topology this service owns and uses for delivery.
 *
 * <p>Topology (all names fixed by the platform spec):
 * <pre>
 *   exchange:  securebank.notifications.exchange  (topic)
 *   queue:     securebank.notifications.queue     (durable)
 *   binding:   queue  &lt;-- routing key "notification.#" --  exchange
 * </pre>
 *
 * <p>Because these beans are {@code Declarable}, Spring AMQP auto-declares them on the
 * broker at startup (creating them if absent) — so a fresh RabbitMQ needs no manual
 * setup. We also register a JSON message converter so {@code NotificationMessage}
 * records travel as readable JSON rather than opaque Java serialization.
 */
@Configuration
public class RabbitConfig {

    /** Exchange name — externalized so application.yml is the single source of truth. */
    @Value("${securebank.rabbit.exchange}")
    private String exchangeName;

    /** Queue name. */
    @Value("${securebank.rabbit.queue}")
    private String queueName;

    /** Routing key the producer stamps and the binding matches. */
    @Value("${securebank.rabbit.routing-key}")
    private String routingKey;

    /**
     * Topic exchange. Topic (rather than direct) keeps the door open for future
     * channel-specific routing keys (e.g. {@code notification.email},
     * {@code notification.sms}) without re-declaring the exchange.
     */
    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    /** Durable queue so undelivered messages survive a broker restart. */
    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    /**
     * Bind the queue to the exchange. We bind with a wildcard ("notification.#") so the
     * single queue receives every notification routing key — today just one, but ready
     * for per-channel keys later.
     */
    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(notificationsExchange)
                .with("notification.#");
    }

    /** Marshal message bodies as JSON (both producing and consuming). */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate wired with the JSON converter. The producer side
     * ({@code NotificationProducer}) uses this to publish.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    // --- Accessors used by the producer so it does not re-read @Value strings ---

    @Bean
    public RabbitNames rabbitNames() {
        return new RabbitNames(exchangeName, queueName, routingKey);
    }

    /** Small immutable holder so other beans can inject the configured names cleanly. */
    public record RabbitNames(String exchange, String queue, String routingKey) {
    }
}
