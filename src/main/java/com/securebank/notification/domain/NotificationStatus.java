package com.securebank.notification.domain;

/**
 * Lifecycle of a single notification row.
 *
 * <p>The happy path is PENDING -&gt; QUEUED -&gt; DELIVERED:
 * <ul>
 *   <li>{@link #PENDING} — row persisted from the Kafka event, not yet handed to RabbitMQ.</li>
 *   <li>{@link #QUEUED}  — successfully published to the RabbitMQ exchange.</li>
 *   <li>{@link #DELIVERED} — the delivery consumer "sent" it through a channel.</li>
 *   <li>{@link #FAILED}  — delivery raised an error (kept for retry/inspection).</li>
 * </ul>
 */
public enum NotificationStatus {
    PENDING,
    QUEUED,
    DELIVERED,
    FAILED
}
