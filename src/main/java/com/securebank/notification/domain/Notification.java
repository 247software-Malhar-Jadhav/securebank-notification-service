package com.securebank.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * JPA entity = one row in the {@code notifications.notifications} table. This is the
 * persistent audit log: every transaction event we process leaves exactly one row here,
 * regardless of which channel ends up delivering it.
 *
 * <p>Kept separate from {@link com.securebank.notification.dto.TransactionCompletedEvent}
 * on purpose — the inbound event is a transient wire contract, this is our own durable
 * record. The table is created/managed by Flyway (see V1__create_notifications.sql); we
 * do NOT let Hibernate auto-DDL it.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    /** Surrogate primary key (BIGSERIAL in Postgres). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The account (or user) the notification is about. We use the source account from
     * the transaction event as the "owner" reference for the message.
     */
    @Column(name = "account_ref", nullable = false)
    private String accountRef;

    /**
     * The originating transaction reference. Doubles as our idempotency key so we never
     * persist/deliver the same event twice (unique constraint in the migration).
     */
    @Column(name = "transaction_ref", nullable = false, unique = true)
    private String transactionRef;

    /** Which channel this notification targets (LOG today). */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    /** The locale the body was rendered in (en / hi / mr). */
    @Column(name = "locale", nullable = false, length = 8)
    private String locale;

    /** The fully rendered, localized notification text. */
    @Column(name = "body", nullable = false, length = 1024)
    private String body;

    /** Current lifecycle status (see {@link NotificationStatus}). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status;

    /** When this row was created (event-processing time). */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** When delivery completed (null until DELIVERED). */
    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;
}
