package com.securebank.notification.messaging;

import java.io.Serializable;

/**
 * The payload we publish onto RabbitMQ for delivery. This is a separate, minimal wire
 * contract from both the inbound Kafka event and the JPA entity — it carries only what a
 * delivery channel needs.
 *
 * <p>Implements {@link Serializable} as a courtesy; in practice it is marshalled as JSON
 * by a {@code Jackson2JsonMessageConverter} (see {@code RabbitConfig}).
 *
 * @param notificationId the DB id of the persisted notification (so the delivery
 *                       consumer can mark the exact row DELIVERED)
 * @param accountRef     the account the message is for
 * @param channel        the target channel name (e.g. "LOG")
 * @param locale         the locale the body was rendered in
 * @param body           the localized, ready-to-send notification text
 */
public record NotificationMessage(
        Long notificationId,
        String accountRef,
        String channel,
        String locale,
        String body
) implements Serializable {
}
