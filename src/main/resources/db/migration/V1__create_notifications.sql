-- V1: notifications log table.
--
-- One row per processed TransactionCompleted event. This is the durable audit trail of
-- everything this service has notified about, independent of the delivery channel.
--
-- Flyway runs this against the `notifications` schema (configured in application.yml).

CREATE TABLE notifications (
    -- Surrogate PK.
    id              BIGSERIAL PRIMARY KEY,

    -- The account the notification concerns (source account of the transfer).
    account_ref     VARCHAR(64)  NOT NULL,

    -- Originating transaction reference. UNIQUE so the consumer is idempotent: a
    -- redelivered Kafka event cannot create a second row.
    transaction_ref VARCHAR(64)  NOT NULL UNIQUE,

    -- Target channel: LOG (today) | EMAIL | SMS | PUSH (future).
    channel         VARCHAR(16)  NOT NULL,

    -- Locale the body was rendered in: en | hi | mr.
    locale          VARCHAR(8)   NOT NULL,

    -- The fully rendered, localized message text.
    body            VARCHAR(1024) NOT NULL,

    -- Lifecycle: PENDING -> QUEUED -> DELIVERED (or FAILED).
    status          VARCHAR(16)  NOT NULL,

    -- When we processed the event.
    created_at      TIMESTAMPTZ  NOT NULL,

    -- When delivery completed (NULL until DELIVERED).
    delivered_at    TIMESTAMPTZ
);

-- Common query path: find by status (e.g. dashboards / retries of FAILED rows).
CREATE INDEX idx_notifications_status ON notifications (status);

-- Common query path: look up everything for an account.
CREATE INDEX idx_notifications_account_ref ON notifications (account_ref);
