package com.securebank.notification.consumer;

import com.securebank.notification.dto.TransactionCompletedEvent;
import com.securebank.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Inbound edge: the Kafka consumer for {@code TransactionCompleted} events.
 *
 * <p>This is one side of the platform's <b>pub/sub (Observer) pattern</b>:
 * transaction-service is the subject/publisher; this service is an observer that reacts
 * to each completed transaction. It is fully decoupled — transaction-service knows
 * nothing about notifications.
 *
 * <p>Deserialization to {@link TransactionCompletedEvent} is handled by the JSON
 * deserializer configured in {@code KafkaConfig}. We keep this class thin: it just hands
 * the event to {@link NotificationService} which does the real work.
 */
@Slf4j
@Component
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    // Constructor injection only.
    public TransactionEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Consume one transaction event. Topic and group are taken from application.yml so
     * the Docker profile can override the broker address.
     */
    @KafkaListener(
            topics = "${securebank.kafka.transactions-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        if (event == null) {
            // Can happen if the ErrorHandlingDeserializer swallowed a poison record.
            log.warn("Received null/undeserializable transaction event; skipping");
            return;
        }
        log.info("Received TransactionCompleted reference={} status={} amount={} {}",
                event.reference(), event.status(), event.amount(), event.currency());
        notificationService.process(event);
    }
}
