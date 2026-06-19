package com.securebank.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Notification} rows.
 *
 * <p>Most access is by the transaction reference (our idempotency / correlation key):
 * the inbound Kafka consumer checks "have I seen this reference?" and the RabbitMQ
 * delivery consumer looks the row back up to mark it DELIVERED.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Look up a notification by its originating transaction reference. */
    Optional<Notification> findByTransactionRef(String transactionRef);

    /** Idempotency guard: have we already recorded this transaction? */
    boolean existsByTransactionRef(String transactionRef);
}
