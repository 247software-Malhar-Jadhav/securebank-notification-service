package com.securebank.notification.messaging;

import com.securebank.notification.config.RabbitConfig.RabbitNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link NotificationMessage}s onto the RabbitMQ notifications exchange.
 *
 * <p>This is the "publish" half of the pub/sub (Observer) flow: after the service has
 * persisted a notification, it fires it onto the exchange and forgets about it. The
 * {@code NotificationDeliveryConsumer} on the bound queue picks it up asynchronously.
 */
@Slf4j
@Component
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitNames names;

    // Constructor injection only.
    public NotificationProducer(RabbitTemplate rabbitTemplate, RabbitNames names) {
        this.rabbitTemplate = rabbitTemplate;
        this.names = names;
    }

    /**
     * Convert-and-send the message to the configured exchange + routing key. The JSON
     * converter registered in {@code RabbitConfig} handles serialization.
     */
    public void publish(NotificationMessage message) {
        rabbitTemplate.convertAndSend(names.exchange(), names.routingKey(), message);
        log.debug("Published notification id={} to exchange={} routingKey={}",
                message.notificationId(), names.exchange(), names.routingKey());
    }
}
