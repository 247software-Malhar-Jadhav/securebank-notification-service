package com.securebank.notification.consumer;

import com.securebank.notification.delivery.ChannelStrategy;
import com.securebank.notification.delivery.ChannelStrategyResolver;
import com.securebank.notification.domain.Notification;
import com.securebank.notification.domain.NotificationChannel;
import com.securebank.notification.domain.NotificationRepository;
import com.securebank.notification.domain.NotificationStatus;
import com.securebank.notification.messaging.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Outbound edge: the RabbitMQ consumer that performs (simulated) delivery.
 *
 * <p>It reads from {@code securebank.notifications.queue}, resolves the appropriate
 * {@link ChannelStrategy} (Strategy pattern) for the message's channel, "delivers" it,
 * and marks the persisted {@link Notification} row {@code DELIVERED}.
 *
 * <p><b>This is the integration seam for real delivery.</b> Today the only registered
 * strategy is {@code LogChannel}, which just logs. SMS / email / push would plug in here
 * as additional {@code ChannelStrategy} beans — no change to this consumer is required;
 * it dispatches purely by {@link NotificationChannel}.
 */
@Slf4j
@Component
public class NotificationDeliveryConsumer {

    private final NotificationRepository repository;
    private final ChannelStrategyResolver strategyResolver;

    // Constructor injection only.
    public NotificationDeliveryConsumer(NotificationRepository repository,
                                        ChannelStrategyResolver strategyResolver) {
        this.repository = repository;
        this.strategyResolver = strategyResolver;
    }

    /**
     * Consume one notification from the queue and deliver it.
     *
     * <p>The queue name is resolved from application.yml. {@code @Transactional} keeps the
     * status update atomic; if delivery throws we record FAILED and rethrow so the broker
     * can apply its retry/requeue policy.
     */
    @RabbitListener(queues = "${securebank.rabbit.queue}")
    @Transactional
    public void onNotification(NotificationMessage message) {
        log.info("Delivering notification id={} via channel={}",
                message.notificationId(), message.channel());

        // Look the persisted row back up so we can flip its status.
        Notification notification = repository.findById(message.notificationId())
                .orElse(null);
        if (notification == null) {
            // The row should always exist (we persist before publishing); guard anyway.
            log.warn("No notification row for id={}; delivering without status update",
                    message.notificationId());
        }

        try {
            // Strategy pattern: pick the delivery algorithm by channel at runtime.
            NotificationChannel channel = NotificationChannel.valueOf(message.channel());
            ChannelStrategy strategy = strategyResolver.resolve(channel);
            strategy.deliver(message);

            if (notification != null) {
                notification.setStatus(NotificationStatus.DELIVERED);
                notification.setDeliveredAt(OffsetDateTime.now());
                repository.save(notification);
            }
            log.info("Delivered notification id={}", message.notificationId());

        } catch (Exception ex) {
            log.error("Delivery failed for notification id={}: {}",
                    message.notificationId(), ex.getMessage(), ex);
            if (notification != null) {
                notification.setStatus(NotificationStatus.FAILED);
                repository.save(notification);
            }
            // Rethrow so the listener container / broker can handle retry semantics.
            throw new RuntimeException("Notification delivery failed", ex);
        }
    }
}
