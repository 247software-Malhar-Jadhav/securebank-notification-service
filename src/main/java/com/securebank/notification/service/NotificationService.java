package com.securebank.notification.service;

import com.securebank.notification.domain.Notification;
import com.securebank.notification.domain.NotificationChannel;
import com.securebank.notification.domain.NotificationRepository;
import com.securebank.notification.domain.NotificationStatus;
import com.securebank.notification.dto.TransactionCompletedEvent;
import com.securebank.notification.messaging.NotificationMessage;
import com.securebank.notification.messaging.NotificationProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * The orchestration core of the service. Given an inbound transaction event it:
 * <ol>
 *   <li>guards against duplicates (idempotency by transaction reference),</li>
 *   <li>builds a localized message body,</li>
 *   <li>persists a {@link Notification} row (status PENDING), then</li>
 *   <li>publishes a {@link NotificationMessage} to RabbitMQ and flips the row to QUEUED.</li>
 * </ol>
 *
 * <p>The persist-then-publish is wrapped in a single transaction: if the publish throws,
 * the row write rolls back too, so we never have a "QUEUED" row that was never actually
 * sent. (A production system might use the transactional outbox pattern here; documented
 * as a future refinement in docs/notification-service.md.)
 */
@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final MessageLocalizer localizer;
    private final NotificationProducer producer;

    /**
     * Default locale for notifications. The transaction event has no per-user locale, so
     * we use a configured platform default (en) and could later resolve the recipient's
     * preferred locale from account-service. Configurable so deployments can switch.
     */
    @Value("${securebank.notification.default-locale:en}")
    private String defaultLocale;

    // Constructor injection only.
    public NotificationService(NotificationRepository repository,
                               MessageLocalizer localizer,
                               NotificationProducer producer) {
        this.repository = repository;
        this.localizer = localizer;
        this.producer = producer;
    }

    /**
     * Process one transaction event end to end (persist + publish).
     *
     * @param event the inbound, already-validated event
     */
    @Transactional
    public void process(TransactionCompletedEvent event) {
        // 1) Idempotency: the same event can be redelivered by Kafka (at-least-once).
        //    The transaction reference is our natural dedupe key.
        if (repository.existsByTransactionRef(event.reference())) {
            log.info("Skipping already-processed transaction reference={}", event.reference());
            return;
        }

        // 2) Build the localized body. Today everything goes via the LOG channel.
        String locale = defaultLocale;
        String body = localizer.localize(event, locale);

        // 3) Persist the audit row in PENDING state.
        Notification notification = Notification.builder()
                .accountRef(event.fromAccountId())
                .transactionRef(event.reference())
                .channel(NotificationChannel.LOG)
                .locale(locale)
                .body(body)
                .status(NotificationStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        notification = repository.save(notification);

        // 4) Publish to RabbitMQ and mark QUEUED. Inside the same TX (see class javadoc).
        NotificationMessage message = new NotificationMessage(
                notification.getId(),
                notification.getAccountRef(),
                notification.getChannel().name(),
                notification.getLocale(),
                notification.getBody());
        producer.publish(message);

        notification.setStatus(NotificationStatus.QUEUED);
        repository.save(notification);

        log.info("Queued notification id={} for transaction reference={}",
                notification.getId(), event.reference());
    }
}
