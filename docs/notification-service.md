# Notification Service — Architecture & How-To

The notification service is the platform's **purely event-driven** component. It bridges
**Kafka** (domain events) and **RabbitMQ** (delivery), persisting a localized audit log
of everything it processes. It speaks no gRPC and exposes no business REST API.

## 1. The Kafka → RabbitMQ flow

```mermaid
flowchart LR
    subgraph TX[transaction-service]
        T1[Transfer saga completes]
    end

    subgraph K[Kafka]
        TOPIC[(topic: securebank.transactions)]
    end

    subgraph NS[notification-service]
        C1[TransactionEventConsumer\n@KafkaListener]
        S1[NotificationService\nlocalize + persist]
        DB[(Postgres\nnotifications.notifications)]
        P1[NotificationProducer\nRabbitTemplate]
        C2[NotificationDeliveryConsumer\n@RabbitListener]
        ST[ChannelStrategyResolver\n→ LogChannel]
    end

    subgraph R[RabbitMQ]
        EX{{exchange:\nsecurebank.notifications.exchange}}
        Q[[queue:\nsecurebank.notifications.queue]]
    end

    T1 -- "TransactionCompleted (JSON)" --> TOPIC
    TOPIC --> C1 --> S1
    S1 -- "INSERT status=PENDING/QUEUED" --> DB
    S1 --> P1
    P1 -- "routing key notification.transaction.completed" --> EX
    EX -- "binding notification.#" --> Q
    Q --> C2 --> ST
    ST -- "deliver (log today)" --> OUT[(SMS / Email / Push\n— future channels)]
    C2 -- "UPDATE status=DELIVERED" --> DB
```

### Step by step

1. **transaction-service** finishes a transfer and publishes `TransactionCompleted` to
   the Kafka topic `securebank.transactions`.
2. **`TransactionEventConsumer`** (`@KafkaListener`) deserializes the JSON into
   `TransactionCompletedEvent` and delegates to `NotificationService`.
3. **`NotificationService`**:
   - skips the event if its `reference` was already processed (idempotency),
   - builds a localized body via `MessageLocalizer` + `MessageSource`,
   - saves a `notifications` row (`PENDING`),
   - publishes a `NotificationMessage` through `NotificationProducer` (`QUEUED`).
4. **RabbitMQ** routes the message from the `securebank.notifications.exchange` (topic)
   to the bound `securebank.notifications.queue`.
5. **`NotificationDeliveryConsumer`** (`@RabbitListener`) resolves a `ChannelStrategy`
   and delivers, then marks the row `DELIVERED` (or `FAILED`).

## 2. Event schema (`securebank.transactions`)

JSON matching what transaction-service publishes (aligned with the `Transaction`
message in `transaction.proto`):

| Field           | Type             | Notes                                  |
|-----------------|------------------|----------------------------------------|
| `reference`     | string           | Unique transfer ref; idempotency key   |
| `fromAccountId` | string           | Source (debited) account               |
| `toAccountId`   | string           | Destination (credited) account         |
| `amount`        | number (decimal) | Mapped to `BigDecimal` — never a float  |
| `currency`      | string           | ISO-4217 (e.g. `INR`, `USD`)           |
| `status`        | string           | `COMPLETED` \| `REJECTED` \| `FAILED`  |
| `createdAt`     | string (ISO-8601)| Producer timestamp (`OffsetDateTime`)  |

```json
{
  "reference": "TXN-9f3c",
  "fromAccountId": "acc-001",
  "toAccountId": "acc-002",
  "amount": 150.00,
  "currency": "INR",
  "status": "COMPLETED",
  "createdAt": "2026-06-19T12:34:56Z"
}
```

The DTO (`TransactionCompletedEvent`) tolerates unknown fields, so additive producer
changes never break this consumer.

## 3. Localization (en / hi / mr)

Message text lives in resource bundles, never in code:

| Bundle                     | Locale   | Script               |
|----------------------------|----------|----------------------|
| `i18n/messages.properties`    | English  | Latin (default)      |
| `i18n/messages_hi.properties` | Hindi    | Devanagari (हिन्दी)  |
| `i18n/messages_mr.properties` | Marathi  | Devanagari (मराठी)   |

`I18nConfig` configures a `ResourceBundleMessageSource` with **UTF-8** decoding so the
Devanagari survives. `MessageLocalizer` supplies the placeholder arguments
(`{0}`=amount, `{1}`=currency, `{2}`=destination, `{3}`=reference) and selects the
`Locale`. The locale is currently the configured platform default (`en`); a future
enhancement is to look up the recipient's preferred locale (e.g. from account-service).

## 4. Persistence — the notifications log

`notifications.notifications` (Flyway `V1__create_notifications.sql`):

| Column            | Purpose                                            |
|-------------------|----------------------------------------------------|
| `id`              | PK                                                 |
| `account_ref`     | Account the notification concerns                  |
| `transaction_ref` | Source transaction ref — **UNIQUE** (idempotency)  |
| `channel`         | `LOG` (today) / `EMAIL` / `SMS` / `PUSH`           |
| `locale`          | `en` / `hi` / `mr`                                 |
| `body`            | Rendered, localized text                            |
| `status`          | `PENDING → QUEUED → DELIVERED` (or `FAILED`)        |
| `created_at`      | Event-processing time                              |
| `delivered_at`    | Delivery time (null until delivered)               |

Flyway owns the schema; Hibernate is set to `validate` only.

## 5. Design patterns

### Observer / publish–subscribe
Two layers of decoupled eventing:
- **Kafka**: transaction-service (subject) emits events; this service (observer) reacts.
  Neither knows about the other beyond the topic + event shape.
- **RabbitMQ**: `NotificationService` publishes and forgets; the delivery consumer
  reacts independently. This isolates the (potentially slow/flaky) delivery integrations
  from the fast event-ingest path.

### Strategy (pluggable delivery channels)
`ChannelStrategy` defines the delivery contract; `ChannelStrategyResolver` indexes all
implementations by `NotificationChannel` and the delivery consumer dispatches by channel
at runtime.

**Today:** only `LogChannel` (logs the message).

**Adding Email / SMS / Push** — the integration seam:
```java
@Component
public class EmailChannel implements ChannelStrategy {
    public NotificationChannel channel() { return NotificationChannel.EMAIL; }
    public void deliver(NotificationMessage m) { /* call SMTP / SES / SendGrid */ }
}
```
Annotate it `@Component` — `ChannelStrategyResolver` picks it up automatically. **No
change** to `NotificationDeliveryConsumer`, `NotificationService`, or the producer is
required. SMS (Twilio/MSG91) and Push (FCM/APNs) follow the same shape.

## 6. Reliability notes & future work

- **Idempotency** is handled by the unique `transaction_ref` constraint + an
  `existsByTransactionRef` pre-check, so Kafka's at-least-once redelivery is safe.
- **Persist-then-publish** runs in one DB transaction, so we never record `QUEUED`
  without actually publishing. A stricter guarantee against the dual-write problem would
  be the **transactional outbox** pattern (write to an outbox table, relay to RabbitMQ
  asynchronously) — a natural next step.
- **Delivery failures** mark the row `FAILED` and rethrow so the broker can apply
  retry/DLQ policy; a dead-letter queue is the recommended production addition.

## 7. How to run

See the [README](../README.md) for build/Docker/k8s commands. Quick local loop: start
Postgres + Kafka + RabbitMQ, `mvn spring-boot:run`, then publish a `TransactionCompleted`
JSON to `securebank.transactions` and watch for the `[DELIVERY:LOG]` log line and the
`DELIVERED` row.
