# securebank-notification-service

The **event-driven notification microservice** of the SecureBank platform.

It has **no public REST API and no gRPC** — it is purely asynchronous:

```
Kafka  securebank.transactions  ──►  notification-service  ──►  RabbitMQ  ──►  delivery
       (TransactionCompleted)        (localize + persist)       exchange/queue   (LogChannel)
```

| Property        | Value                                            |
|-----------------|--------------------------------------------------|
| Root package    | `com.securebank.notification`                    |
| HTTP port       | `8085` (actuator / swagger only)                 |
| Database        | PostgreSQL, schema `notifications`               |
| Inbound         | Kafka topic `securebank.transactions`            |
| Outbound        | RabbitMQ `securebank.notifications.exchange` → `securebank.notifications.queue` |
| Stack           | Java 21 (virtual threads), Spring Boot 3.3.x, Spring Kafka, Spring AMQP, Spring Data JPA, Flyway, Lombok, springdoc, Micrometer/Prometheus |

## What it does

For each `TransactionCompleted` event:

1. **Deduplicate** by transaction reference (Kafka is at-least-once).
2. **Localize** a message via `MessageSource` — English / Hindi / Marathi (real Devanagari).
3. **Persist** a row into `notifications.notifications` (the audit log) with status `PENDING → QUEUED`.
4. **Publish** a `NotificationMessage` to RabbitMQ.
5. A separate `NotificationDeliveryConsumer` reads the queue, **delivers** via a
   `ChannelStrategy` (currently `LogChannel`), and marks the row `DELIVERED`.

## Design patterns

- **Observer / pub-sub** — transaction-service publishes events; this service is a
  decoupled observer. RabbitMQ fan-out repeats the pattern for delivery.
- **Strategy** — `ChannelStrategy` abstracts "how to deliver". `LogChannel` is the only
  implementation today; **Email / SMS / Push plug in by adding a new bean** with no
  changes to the consumer (see `NotificationDeliveryConsumer` and
  `docs/notification-service.md`).

## Build & run

```bash
# Build (skip tests):
mvn -q -DskipTests package

# Build + test:
mvn -B package

# Run locally (needs Postgres, Kafka, RabbitMQ on localhost — see application.yml):
mvn spring-boot:run
```

### Docker

```bash
docker build -t securebank/notification-service:latest .
# Runs with SPRING_PROFILES_ACTIVE=docker (hostnames: postgres / kafka / rabbitmq).
```

### Kubernetes

```bash
kubectl apply -f k8s/   # configmap, secret, deployment, service
```

## Configuration

- `application.yml` — default (localhost) profile.
- `application-docker.yml` — in-network hostnames (`postgres`, `kafka:29092`, `rabbitmq`).
- Topic / exchange / queue / locale are externalized under the `securebank.*` keys.

## Observability

- `GET /actuator/health` (+ `/health/liveness`, `/health/readiness`)
- `GET /actuator/prometheus`
- `GET /swagger-ui.html`

## Producing a test event

Publish JSON to `securebank.transactions`:

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

You should see a `[DELIVERY:LOG]` line and a `DELIVERED` row in
`notifications.notifications`.

See [`docs/notification-service.md`](docs/notification-service.md) for the full flow,
event schema, localization details and pattern rationale.
